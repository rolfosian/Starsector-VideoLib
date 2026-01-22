package videolib.decoder;

import org.apache.log4j.Logger;
import org.lwjgl.openal.AL10;

import videolib.ffmpeg.AudioFrame;
import videolib.ffmpeg.FFmpeg;
import videolib.ffmpeg.Frame;
import videolib.ffmpeg.VideoFrame;

import videolib.VideoLibModPlugin;
import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;

import videolib.buffers.TextureBuffer;
import videolib.buffers.AudioFrameBuffer;
import videolib.buffers.RGBATextureBuffer;
import videolib.buffers.TexBuffer;

import videolib.playerui.PlayerControlPanel;
import videolib.projector.Projector;
import videolib.speakers.Speakers;

public class DecoderWithSound implements Decoder {
    private static final Logger logger = Logger.getLogger(DecoderWithSound.class);
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
    private Thread videoDecodeThread;
    private long ctxPtr;

    private TexBuffer textureBuffer;
    private AudioFrameBuffer audioBuffer;

    // private float gameFps = 0f;
    private float videoFps = 0f;
    private float spf = 0f;
    private double videoDurationSeconds = 0;
    private long videoDurationUs = 0;

    private int currentVideoTextureId = 0;
    private long currentVideoPts = 0;
    private long currentAudioPts = 0;
    private long lastRenderedAudioPts = 0;

    public final Projector videoProjector;
    public Speakers speakers;
    private int speakersSourceId;

    private int width;
    private int height;

    private int audioChannels;
    private int audioSampleRate;

    private float timeAccumulator = 0f;

    public DecoderWithSound(Projector videoProjector, String videoFilePath, int width, int height, float volume, PlayMode startingPlayMode, EOFMode startingEOFMode) {
        print("Initializing DecoderWithSound for file " + videoFilePath + " using class " + videoProjector.getClass().getName());
        this.videoFilePath = videoFilePath;

        this.videoProjector = videoProjector;

        this.audioBuffer = new AudioFrameBuffer(30);

        this.width = width;
        this.height = height;

        this.PLAY_MODE = startingPlayMode;
        this.EOF_MODE = startingEOFMode;
    }

    private void decodeLoop() {
        // print("DecoderWithSound decodeLoop started");
        while (running) {
            if (!textureBuffer.isFull() && !audioBuffer.isFull()) {
                Frame f = FFmpeg.read(ctxPtr);

                if (f == null) { // EOF / Error
                    if (FFmpeg.getErrorStatus(ctxPtr) != FFmpeg.AVERROR_EOF) {
                        logger.error("FFmpeg error for file " + videoFilePath + ": " + FFmpeg.getErrorMessage(ctxPtr)
                        + ", interrupting main thread...",
                        new RuntimeException(FFmpeg.getErrorMessage(ctxPtr)));

                        FFmpeg.closeCtx(ctxPtr);
                        ctxPtr = 0;
                        textureBuffer.clear();
                        audioBuffer.clear();

                        VideoLibModPlugin.getMainThread().interrupt();
                        return;
                    }

                    if (this.PLAY_MODE != PlayMode.SEEKING) {
                        synchronized(seekLock) {
                            if (this.EOF_MODE == EOFMode.PAUSE || this.EOF_MODE == EOFMode.PLAY_UNTIL_END) {
                                PlayerControlPanel controlPanel = videoProjector.getControlPanel();

                                if (controlPanel != null) {
                                    if (!videoProjector.paused()) {
                                        while (!textureBuffer.isEmpty() && !audioBuffer.isEmpty()) {
                                            sleep(1);
                                            if (videoProjector.paused()) break;
                                            if (!videoProjector.isRendering()) break;
                                        }
                                        videoProjector.pause();
                                        speakers.restart(); // HOLY FUCKIBG BANDAID WHY DOES THIS WORK TO CURB A/V DESYNC BUT NOT SIMPLY STOP()
                                        speakers.pause();
                                        seekWithoutClearingBuffer(0);
        
                                        if (videoProjector.getPlayMode() != PlayMode.SEEKING) {
                                            
                                            controlPanel.setProgressDisplay(currentVideoPts);
                                            controlPanel.getPlayButton().setEnabled(true);
                                            controlPanel.getPauseButton().setEnabled(false);
                                            controlPanel.getStopButton().setEnabled(true);
                                        }
                                        continue;
    
                                    } else {
                                        while (!textureBuffer.isEmpty() && !audioBuffer.isEmpty()) {
                                            sleep(1);
                                            if (videoProjector.paused()) break;
                                            if (!videoProjector.isRendering()) break;
                                        }
                                        videoProjector.pause();
                                        speakers.restart(); // HOLY FUCKIBG BANDAID WHY DOES THIS WORK TO CURB A/V DESYNC BUT NOT SIMPLY STOP()
                                        speakers.pause();
                                        seekWithoutClearingBuffer(0);

                                        controlPanel.setProgressDisplay(currentVideoPts);
                                        controlPanel.getPlayButton().setEnabled(true);
                                        controlPanel.getPauseButton().setEnabled(false);
                                        controlPanel.getStopButton().setEnabled(true);
                                        
                                        continue;
                                    }
                                } else {
                                    switch(this.EOF_MODE) {
                                        case PLAY_UNTIL_END:
                                            while (!textureBuffer.isEmpty() && !audioBuffer.isEmpty() && AL10.alGetSourcei(speakersSourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                                                sleep(1);
                                                if (videoProjector.paused()) break;
                                                if (!videoProjector.isRendering()) break;
                                            }
                                            seekWithoutClearingBuffer(0);
                                            videoProjector.pause();
                                            speakers.restart(); // HOLY FUCKIBG BANDAID WHY DOES THIS WORK TO CURB A/V DESYNC BUT NOT SIMPLY STOP()
                                            speakers.pause();

                                            videoProjector.setPlayMode(PlayMode.SEEKING);
                                            videoProjector.setIsRendering(false);
                                            break;

                                        case PAUSE:
                                            while (!textureBuffer.isEmpty() && !audioBuffer.isEmpty() && AL10.alGetSourcei(speakersSourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                                                sleep(1);
                                                if (videoProjector.paused()) break;
                                                if (!videoProjector.isRendering()) break;
                                            }
                                            seekWithoutClearingBuffer(0);
                                            videoProjector.pause();
                                            speakers.restart(); // HOLY FUCKIBG BANDAID WHY DOES THIS WORK TO CURB A/V DESYNC BUT NOT SIMPLY STOP()
                                            speakers.pause();
                                            break;

                                        default: // TODO: impl FINISH case to cleanup; we cant do it from this thread because we need the gl context to delete the textures i believe
                                            sleep(1);
                                            break;

                                    }
                                }
                            } else {
                                while (AL10.alGetSourcei(speakersSourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                                    sleep(1);
                                    if (videoProjector.paused()) break;
                                    if (!videoProjector.isRendering()) break;
                                } 
                                speakers.stop();
                                speakers.play();
                                seekWithoutClearingBuffer(0);
                                continue;
                            }
                        }
                        sleep(1);
                        continue;

                    } else {
                        switch(this.EOF_MODE) {
                            case PAUSE:
                                seekWithoutClearingBuffer(videoDurationUs);
                                while (!textureBuffer.isEmpty() && !audioBuffer.isEmpty() && AL10.alGetSourcei(speakersSourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                                    sleep(1);
                                    if (!videoProjector.isRendering()) break;
                                }
                                videoProjector.pause();
                                speakers.restart();
                                speakers.pause();
                                break;

                            case PLAY_UNTIL_END:
                                seekWithoutClearingBuffer(0);
                                while (!textureBuffer.isEmpty() && !audioBuffer.isEmpty() && AL10.alGetSourcei(speakersSourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                                    sleep(1);
                                    if (!videoProjector.isRendering()) break;
                                }
                                videoProjector.pause();
                                speakers.restart();
                                speakers.pause();
                                break;
                                
                            case LOOP:
                                seekWithoutClearingBuffer(0);
                                continue;
                            default:
                                break;
                        }
                    }

                } else {
                    if (f instanceof VideoFrame) {
                        synchronized (textureBuffer) {
                            textureBuffer.add((VideoFrame)f);
                        }
                    } else {
                        synchronized(audioBuffer) {
                            audioBuffer.add((AudioFrame)f);
                        }
                    }
                }
            } else {
                sleep(1);
            }
        }
        // print("DecoderWithSound decodeLoop ended");
    }

    public int getCurrentVideoTextureId(float deltaTime) {
        // gameFps = 1 / deltaTime;
        timeAccumulator += deltaTime;

        currentAudioPts = speakers.getCurrentAudioPts();
        if (lastRenderedAudioPts == currentAudioPts) return currentVideoTextureId;

        synchronized(textureBuffer) {
            while (timeAccumulator >= spf) {
                timeAccumulator -= spf;
                
                currentVideoPts = textureBuffer.update();

                while (!textureBuffer.isEmpty() && (currentAudioPts > currentVideoPts) ) {
                    currentVideoPts = textureBuffer.update();
                }
            }
        }

        lastRenderedAudioPts = currentAudioPts;
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
        // print("Starting DecoderWithSound for file", videoFilePath);
        running = true;

        ctxPtr = FFmpeg.openCtx(videoFilePath, width, height, startUs);
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
        // this.textureBuffer = isRGBA ? new RGBATextureBuffer(30) : new TextureBuffer(30);
        this.textureBuffer = new RGBATextureBuffer(30);
        this.textureBuffer.initTexStorage(width, height);
        this.currentVideoTextureId = this.textureBuffer.getTextureId();
        // this.textureBuffer = isRGBA ? new RGBATextureBufferList() : new TextureBufferList();

        audioChannels = FFmpeg.getAudioChannels(ctxPtr);
        audioSampleRate = FFmpeg.getAudioSampleRate(ctxPtr);
        // print("Audio Channels=", audioChannels);
        // print("Audio Sample Rate=", audioSampleRate);

        videoDecodeThread = new Thread(this::decodeLoop, "DecoderWithSound-decodeLoop");
        videoDecodeThread.start();

        // print("DecoderWithSound decoderLoop thread started");

        while(!textureBuffer.isFull() && !audioBuffer.isFull()) sleep(1);
        return;
    }

    public void finish() {
        print("Stopping DecoderWithSound thread");
        running = false;
        timeAccumulator = 0f;
        videoFps = 0f;

        // print("Joining DecoderWithSound thread");
        try {
            videoDecodeThread.join();
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (ctxPtr != 0) {
            // print("Closing FFmpeg ctx");
            FFmpeg.closeCtx(ctxPtr);
            ctxPtr = 0;
        }

        // print("Clearing Texture/Video Buffer");
        synchronized(textureBuffer) {
            textureBuffer.clear();
            textureBuffer.cleanupTexStorage();
        }
        synchronized(audioBuffer) {
            audioBuffer.clear();
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
            synchronized(audioBuffer) {
                audioBuffer.clear();
            }
            speakers.stop();
            speakers.notifySeek(targetUs);

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

    public int getAudioChannels() {
        return this.audioChannels;
    }
    public int getSampleRate() {
        return this.audioSampleRate;
    }

    public void setSpeakers(Speakers speakers) {
        this.speakers = speakers;
        this.speakersSourceId = speakers.getSourceId();
    }

    public Speakers getSpeakers() {
        return this.speakers;
    }

    public AudioFrameBuffer getAudioFrameBuffer() {
        return this.audioBuffer;
    }

    public TexBuffer getTextureBuffer() {
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
        return FFmpeg.getErrorStatus(ctxPtr);
    }

    public void setVideoFilePath(String path) {
        this.videoFilePath = path;
    }

    public long getFFmpegCtxPtr() {
        return this.ctxPtr;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch(InterruptedException ignored) {}
    }
}