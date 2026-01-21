package videolib.decoder;

import videolib.ffmpeg.FFmpeg;
import videolib.ffmpeg.VideoFrame;

import videolib.playerui.PlayerControlPanel;
import videolib.projector.Projector;

import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;

import videolib.buffers.RGBATextureBuffer;
import videolib.buffers.TextureBuffer;

import videolib.VideoLibModPlugin;

import org.apache.log4j.Logger;

public class MuteDecoder implements Decoder {
    private static final Logger logger = Logger.getLogger(MuteDecoder.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    private PlayMode PLAY_MODE;
    private PlayMode OLD_PLAY_MODE;
    private EOFMode EOF_MODE;

    private Object seekLock = new Object();

    private String videoFilePath;
    private volatile boolean running = false;
    private Thread decodeThread;
    private long pipePtr;
    private TextureBuffer textureBuffer;

    // private float gameFps = 0f;
    private float videoFps = 0f;
    private float spf = 0f;
    private double videoDurationSeconds = 0;
    private long videoDurationUs = 0;

    private int currentVideoTextureId = 0;
    private long currentVideoPts = 0;

    public final Projector videoProjector;

    private int width;
    private int height;

    private float timeAccumulator = 0f;

    public MuteDecoder(Projector videoProjector, String videoFilePath, int width, int height, PlayMode startingPlayMode, EOFMode startingEOFMode) {
        print("Initializing MuteDecoder for file " + videoFilePath);
        this.videoProjector = videoProjector;
        this.videoFilePath = videoFilePath;

        this.width = width;
        this.height = height;

        this.PLAY_MODE = startingPlayMode;
        this.EOF_MODE = startingEOFMode;
    }

    private void decodeLoop() {
        // print("MuteDecoder decodeLoop started");
        // try {
        while (running) {
            if (!textureBuffer.isFull()) {
                VideoFrame f = FFmpeg.readFrameNoSound(pipePtr);

                if (f == null) { // EOF / Error
                    if (FFmpeg.getErrorStatus(pipePtr) != FFmpeg.AVERROR_EOF) {
                        logger.error("FFmpeg error for file " + videoFilePath + ": " + FFmpeg.getErrorMessage(pipePtr)
                        + ", interrupting main thread...",
                        new RuntimeException(FFmpeg.getErrorMessage(pipePtr)));

                        FFmpeg.closePipe(pipePtr);
                        pipePtr = 0;
                        textureBuffer.clear();
                        VideoLibModPlugin.getMainThread().interrupt();
                        return;
                    }

                    if (this.PLAY_MODE != PlayMode.SEEKING) {
                        synchronized(seekLock) {
                            if (this.EOF_MODE == EOFMode.PAUSE || this.EOF_MODE == EOFMode.PLAY_UNTIL_END) {
                                PlayerControlPanel controlPanel = videoProjector.getControlPanel();

                                if (controlPanel != null) {
                                    if (!videoProjector.paused()) {
                                        while (!textureBuffer.isEmpty()) {
                                            sleep(1);
                                            if (videoProjector.paused()) break;
                                            if (!videoProjector.isRendering()) break;
                                        }
                                        seekWithoutClearingBuffer(0);
        
                                        if (videoProjector.getPlayMode() != PlayMode.SEEKING) {
                                            videoProjector.pause();
                                            
                                            controlPanel.setProgressDisplay(currentVideoPts);
                                            controlPanel.getPlayButton().setEnabled(true);
                                            controlPanel.getPauseButton().setEnabled(false);
                                            controlPanel.getStopButton().setEnabled(true);
                                        }
    
                                    } else {
                                        while (!textureBuffer.isEmpty()) {
                                            sleep(1);
                                            if (videoProjector.paused()) break;
                                            if (!videoProjector.isRendering()) break;
                                        }
                                        controlPanel.setProgressDisplay(currentVideoPts);
                                        controlPanel.getPlayButton().setEnabled(true);
                                        controlPanel.getPauseButton().setEnabled(false);
                                        controlPanel.getStopButton().setEnabled(true);
                                        
                                        seekWithoutClearingBuffer(0);
                                        continue;
                                    }
                                } else {
                                    switch(this.EOF_MODE) {
                                        case PLAY_UNTIL_END:
                                            while (!textureBuffer.isEmpty()) {
                                                sleep(1);
                                                if (videoProjector.paused()) break;
                                                if (!videoProjector.isRendering()) break;
                                            }
                                            seekWithoutClearingBuffer(0);
                                            videoProjector.pause();
                                            videoProjector.setPlayMode(PlayMode.SEEKING);
                                            videoProjector.setIsRendering(false);
                                            break;

                                        case PAUSE:
                                            while (!textureBuffer.isEmpty()) {
                                                sleep(1);
                                                if (videoProjector.paused()) break;
                                                if (!videoProjector.isRendering()) break;
                                            }
                                            seekWithoutClearingBuffer(0);
                                            videoProjector.pause();
                                            break;

                                        default: // TODO: impl FINISH case to cleanup; we cant do it from this thread because we need the gl context to delete the textures i believe
                                            sleep(1);
                                            break;

                                    }
                                }
                            } else {
                                seekWithoutClearingBuffer(0);
                            }
                            
                        }
                        sleep(1);
                        continue;
                    }

                } else {
                    synchronized (textureBuffer) {
                        textureBuffer.add(f);
                    }
                }
            
            } else {
                sleep(1);
            }
        }
        // } catch (Throwable e) {
        //     logger.error(e.getMessage(), e);
        // }
        // print("MuteDecoder decodeLoop ended");
    }

    public int getCurrentVideoTextureId(float deltaTime) {
        // gameFps = 1 / deltaTime;
        timeAccumulator += deltaTime;

        synchronized(textureBuffer) {
            while (timeAccumulator >= spf) {
                timeAccumulator -= spf;
                
                currentVideoPts = textureBuffer.update();
            }
        }
        return currentVideoTextureId;
    }

    public int getCurrentVideoTextureId() {
        while (textureBuffer.isEmpty()) sleep(1); 

        synchronized(textureBuffer) {
            currentVideoPts = textureBuffer.update();
        }
        return currentVideoTextureId;
    }

    public float getVideoFps() { return videoFps; }

    public void start(long startUs) {
        if (running) return;
        // print("Starting MuteDecoder for file", videoFilePath);
        running = true;

        pipePtr = FFmpeg.openPipeNoSound(videoFilePath, width, height, startUs);
        // print("Opened FFmpeg pipe, ptr =", pipePtr);

        if (pipePtr == 0) throw new RuntimeException("Failed to initiate FFmpeg pipe context for " + videoFilePath);

        videoDurationSeconds = FFmpeg.getDurationSeconds(pipePtr);
        videoDurationUs = FFmpeg.getDurationUs(pipePtr);

        videoFps = FFmpeg.getVideoFps(pipePtr);
        spf = 1 / videoFps;
        // print("Video Framerate =", videoFps);
        // print("Video Duration=", videoDurationSeconds);
        // print("Video DurationUs=", videoDurationUs);

        boolean isRGBA = FFmpeg.isRGBA(pipePtr);
        // print("isRGBA=", isRGBA);
        this.textureBuffer = isRGBA ? new RGBATextureBuffer(10) : new TextureBuffer(10);
        this.textureBuffer.initTexStorage(width, height);
        this.currentVideoTextureId = this.textureBuffer.getTextureId();

        decodeThread = new Thread(this::decodeLoop, "MuteDecoder");
        decodeThread.start();
        // print("MuteDecoder decoderLoop thread started");

        while(textureBuffer.isEmpty()) sleep(1);
        return;
    }

    public void finish() {
        // print("Stopping MuteDecoder decoderLoop thread");
        running = false;
        timeAccumulator = 0f;
        videoFps = 0f;

        // print("Joining MuteDecoder decoderLoop thread");
        try {
            decodeThread.join();
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (pipePtr != 0) {
            print("Closing FFmpeg pipe");
            FFmpeg.closePipe(pipePtr);
            pipePtr = 0;
        }

        // print("Clearing Texture/Video Buffer");
        synchronized(textureBuffer) {
            textureBuffer.clear();
            textureBuffer.cleanupTexStorage();
        }
        // textureBuffer.glDeleteBuffers();
    }

    public void stop() {
        PLAY_MODE = PlayMode.STOPPED;
        timeAccumulator = 0f;
        seek(0);
    }

    public void restart(long startUs) {
        finish();
        start(startUs);
    }

    public void seek(long targetUs) {
        synchronized(seekLock) {
            // print("Seeking to", targetUs, "µs");

            FFmpeg.seek(pipePtr, targetUs);
    
            synchronized(textureBuffer) {
                textureBuffer.clear();
            }
            this.currentVideoPts = targetUs;
        }
    }

    public void seekWithoutClearingBuffer(long targetUs) {
        synchronized(seekLock) {
            // print("Seeking to", targetUs, "µs");
            FFmpeg.seek(pipePtr, targetUs);
        }
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public TextureBuffer getTextureBuffer() {
        return this.textureBuffer;
    }

    public float getSpf() {
        return this.spf;
    }

    public double getDurationSeconds() {
        return this.videoDurationSeconds;
    }

    public long getDurationUs() {
        return this.videoDurationUs;
    }

    public long getCurrentVideoPts() {
        return this.currentVideoPts;
    }

    public EOFMode getEOFMode() {
        return this.EOF_MODE;
    }

    public void setEOFMode(EOFMode mode) {
        this.EOF_MODE = mode;
    }

    public PlayMode getPlayMode() {
        return this.PLAY_MODE;
    }
    
    public void setPlayMode(PlayMode newMode) {
        // print("Setting Mode", newMode);
        this.OLD_PLAY_MODE = this.PLAY_MODE;
        this.PLAY_MODE = newMode;
    }

    public int getErrorStatus() {
        return FFmpeg.getErrorStatus(pipePtr);
    }

    public void setVideoFilePath(String path) {
        this.videoFilePath = path;
    }

    public long getFFmpegPipePtr() {
        return this.pipePtr;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch(InterruptedException ignored) {}
    }

    public int getAudioChannels() {return 0;}
    public int getSampleRate() {return 0;}
}
