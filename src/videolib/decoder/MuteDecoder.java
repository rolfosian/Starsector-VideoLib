package videolib.decoder;

import videolib.ffmpeg.FFmpeg;
import videolib.ffmpeg.VideoFrame;

import videolib.playerui.PlayerControlPanel;
import videolib.projector.Projector;

import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;
import videolib.buffers.AudioFrameBuffer;
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

    protected PlayMode PLAY_MODE;
    protected PlayMode OLD_PLAY_MODE;
    protected EOFMode EOF_MODE;

    protected Object seekLock = new Object();

    protected String videoFilePath;
    protected volatile boolean running = false;
    protected Thread decodeThread;
    protected long ctxPtr;
    protected TextureBuffer textureBuffer;

    // protected float gameFps = 0f;
    protected float videoFps = 0f;
    protected float spf = 0f;
    protected double videoDurationSeconds = 0;
    protected long videoDurationUs = 0;

    protected int currentVideoTextureId = 0;
    protected long currentVideoPts = 0;

    public final Projector videoProjector;

    protected int width;
    protected int height;

    protected float timeAccumulator = 0f;

    public MuteDecoder(Projector videoProjector, String videoFilePath, int width, int height, PlayMode startingPlayMode, EOFMode startingEOFMode) {
        print("Initializing MuteDecoder for file " + videoFilePath + " using class " + videoProjector.getClass().getName());
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
                VideoFrame f = FFmpeg.readFrameNoSound(ctxPtr);

                if (f != null) {
                    synchronized (textureBuffer) {
                        textureBuffer.add(f);
                    }
                    continue;
                }

                // EOF or Error past this point

                if (FFmpeg.getErrorStatus(ctxPtr) != FFmpeg.AVERROR_EOF) {
                    logger.error("FFmpeg error for file " + videoFilePath + ": " + FFmpeg.getErrorMessage(ctxPtr)
                    + ", interrupting main thread...",
                    new RuntimeException(FFmpeg.getErrorMessage(ctxPtr)));

                    FFmpeg.closeCtx(ctxPtr);
                    ctxPtr = 0;
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
                sleep(1);
            }
        }
        // } catch (Throwable e) {
        //     logger.error(e.getMessage(), e);
        // }
        // print("MuteDecoder decodeLoop ended");
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    public int getCurrentVideoTextureId(float deltaTime) {
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

    public int getCurrentVideoTextureIdDoNotUpdatePts() {
        while (textureBuffer.isEmpty()) sleep(1); 

        synchronized(textureBuffer) {
            textureBuffer.update();
        }
        return currentVideoTextureId;
    }

    public float getVideoFps() { return videoFps; }

    public void start(long startUs) {
        if (running) return;
        // print("Starting MuteDecoder for file", videoFilePath);
        running = true;

        ctxPtr = FFmpeg.openCtxNoSound(videoFilePath, width, height, startUs);
        // print("Opened FFmpeg ctx, ptr =", ctxPtr);

        if (ctxPtr == 0) throw new RuntimeException("Failed to initiate FFmpeg ctx context for " + videoFilePath);

        videoDurationSeconds = FFmpeg.getDurationSeconds(ctxPtr);
        videoDurationUs = FFmpeg.getDurationUs(ctxPtr);

        videoFps = FFmpeg.getVideoFps(ctxPtr);
        spf = 1 / videoFps;
        // print("Video Framerate =", videoFps);
        // print("Video Duration=", videoDurationSeconds);
        // print("Video DurationUs=", videoDurationUs);

        // boolean isRGBA = FFmpeg.isRGBA(ctxPtr);
        // print("isRGBA=", isRGBA);
        // this.textureBuffer = isRGBA ? new RGBATextureBuffer(10) : new TextureBuffer(10);
        this.textureBuffer = new RGBATextureBuffer(3);
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

        if (ctxPtr != 0) {
            print("Closing FFmpeg ctx");
            FFmpeg.closeCtx(ctxPtr);
            ctxPtr = 0;
        }

        // print("Clearing Texture/Video Buffer");
        synchronized(textureBuffer) {
            textureBuffer.clear();
            textureBuffer.cleanupTexStorage();
        }
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

            FFmpeg.seek(ctxPtr, targetUs);
    
            synchronized(textureBuffer) {
                textureBuffer.clear();
            }
            this.currentVideoPts = targetUs;
        }
    }

    public void seekWithoutClearingBuffer(long targetUs) {
        synchronized(seekLock) {
            // print("Seeking to", targetUs, "µs");
            FFmpeg.seek(ctxPtr, targetUs);
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

    public AudioFrameBuffer getAudioBuffer() {
        return null;
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
        return FFmpeg.getErrorStatus(ctxPtr);
    }

    public void setVideoFilePath(String path) {
        this.videoFilePath = path;
    }

    public long getFFmpegCtxPtr() {
        return this.ctxPtr;
    }

    protected static final void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch(InterruptedException ignored) {}
    }

    public String getVideoFilePath() {
        return this.videoFilePath;
    }

    public int getAudioChannels() {return 0;}
    public int getSampleRate() {return 0;}
}
