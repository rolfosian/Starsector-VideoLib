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

    protected PlayMode PLAY_MODE;
    protected PlayMode OLD_PLAY_MODE;
    protected EOFMode EOF_MODE;

    protected Object seekLock = new Object();

    protected String videoFilePath;
    protected volatile boolean running = false;
    protected Thread videoDecodeThread;
    protected long ctxPtr;

    protected TexBuffer textureBuffer;
    protected AudioFrameBuffer audioBuffer;

    // protected float gameFps = 0f;
    protected float videoFps = 0f;
    protected float spf = 0f;
    protected double videoDurationSeconds = 0;
    protected long videoDurationUs = 0;

    protected int currentVideoTextureId = 0;
    protected long currentVideoPts = 0;
    protected long currentAudioPts = 0;
    protected long lastRenderedAudioPts = 0;

    public final Projector videoProjector;
    public Speakers speakers;
    protected int speakersSourceId;

    protected int width;
    protected int height;

    protected int audioChannels;
    protected int audioSampleRate;

    protected float timeAccumulator = 0f;

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
        while (this.running) {
            if (!this.textureBuffer.isFull() && !this.audioBuffer.isFull()) {
                Frame f = FFmpeg.read(this.ctxPtr);

                if (f != null) {
                    if (f instanceof VideoFrame vf) {
                        synchronized (this.textureBuffer) {
                            this.textureBuffer.add(vf);
                        }
                    } else {
                        synchronized(this.audioBuffer) {
                            this.audioBuffer.add((AudioFrame)f);
                        }
                    }
                    continue;
                }

                // EOF or Error past this point

                if (FFmpeg.getErrorStatus(this.ctxPtr) != FFmpeg.AVERROR_EOF) {
                    logger.error("FFmpeg error for file " + videoFilePath + ": " + FFmpeg.getErrorMessage(this.ctxPtr)
                    + ", interrupting main thread...",
                    new RuntimeException(FFmpeg.getErrorMessage(this.ctxPtr)));

                    FFmpeg.closeCtx(this.ctxPtr);
                    this.ctxPtr = 0;
                    textureBuffer.clear();
                    this.audioBuffer.clear();

                    VideoLibModPlugin.getMainThread().interrupt();
                    return;
                }

                if (this.PLAY_MODE != PlayMode.SEEKING) {
                    synchronized(seekLock) {
                        if (this.EOF_MODE == EOFMode.PAUSE || this.EOF_MODE == EOFMode.PLAY_UNTIL_END) {
                            PlayerControlPanel controlPanel = this.videoProjector.getControlPanel();

                            if (controlPanel != null) {
                                if (!this.videoProjector.paused()) {
                                    while (!this.textureBuffer.isEmpty() && !this.audioBuffer.isEmpty()) {
                                        sleep(1);
                                        if (this.videoProjector.paused()) break;
                                        if (!this.videoProjector.isRendering()) break;
                                    }
                                    this.videoProjector.pause();
                                    this.speakers.restart(); // HOLY FUCKIBG BANDAID WHY DOES THIS WORK TO CURB A/V DESYNC BUT NOT SIMPLY STOP()
                                    this.speakers.pause();
                                    seekWithoutClearingBuffer(0);
    
                                    if (videoProjector.getPlayMode() != PlayMode.SEEKING) {
                                        
                                        controlPanel.setProgressDisplay(this.currentVideoPts);
                                        controlPanel.getPlayButton().setEnabled(true);
                                        controlPanel.getPauseButton().setEnabled(false);
                                        controlPanel.getStopButton().setEnabled(true);
                                    }
                                    continue;

                                } else {
                                    while (!this.textureBuffer.isEmpty() && !this.audioBuffer.isEmpty()) {
                                        sleep(1);
                                        if (this.videoProjector.paused()) break;
                                        if (!this.videoProjector.isRendering()) break;
                                    }
                                    this.videoProjector.pause();
                                    this.speakers.restart(); // HOLY FUCKIBG BANDAID WHY DOES THIS WORK TO CURB A/V DESYNC BUT NOT SIMPLY STOP()
                                    this.speakers.pause();
                                    seekWithoutClearingBuffer(0);

                                    controlPanel.setProgressDisplay(this.currentVideoPts);
                                    controlPanel.getPlayButton().setEnabled(true);
                                    controlPanel.getPauseButton().setEnabled(false);
                                    controlPanel.getStopButton().setEnabled(true);
                                    
                                    continue;
                                }
                            } else {
                                switch(this.EOF_MODE) {
                                    case PLAY_UNTIL_END:
                                        while (!this.textureBuffer.isEmpty() && !this.audioBuffer.isEmpty() && AL10.alGetSourcei(this.speakersSourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                                            sleep(1);
                                            if (this.videoProjector.paused()) break;
                                            if (!this.videoProjector.isRendering()) break;
                                        }
                                        seekWithoutClearingBuffer(0);
                                        this.videoProjector.pause();
                                        this.speakers.restart(); // HOLY FUCKIBG BANDAID WHY DOES THIS WORK TO CURB A/V DESYNC BUT NOT SIMPLY STOP()
                                        this.speakers.pause();

                                        this.videoProjector.setPlayMode(PlayMode.SEEKING);
                                        this.videoProjector.setIsRendering(false);
                                        break;

                                    case PAUSE:
                                        while (!this.textureBuffer.isEmpty() && !this.audioBuffer.isEmpty() && AL10.alGetSourcei(this.speakersSourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                                            sleep(1);
                                            if (this.videoProjector.paused()) break;
                                            if (!this.videoProjector.isRendering()) break;
                                        }
                                        this.seekWithoutClearingBuffer(0);
                                        this.videoProjector.pause();
                                        this.speakers.restart(); // HOLY FUCKIBG BANDAID WHY DOES THIS WORK TO CURB A/V DESYNC BUT NOT SIMPLY STOP()
                                        this.speakers.pause();
                                        break;

                                    default: // TODO: impl FINISH case to cleanup; we cant do it from this thread because we need the gl context to delete the textures i believe
                                        sleep(1);
                                        break;

                                }
                            }
                        } else {
                            while (AL10.alGetSourcei(this.speakersSourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                                sleep(1);
                                if (this.videoProjector.paused()) break;
                                if (!this.videoProjector.isRendering()) break;
                            } 
                            this.speakers.stop();
                            this.speakers.play();
                            this.seekWithoutClearingBuffer(0);
                            continue;
                        }
                    }
                    sleep(1);
                    continue;

                } else {
                    switch(this.EOF_MODE) {
                        case PAUSE:
                            seekWithoutClearingBuffer(this.videoDurationUs);
                            while (!this.textureBuffer.isEmpty() && !this.audioBuffer.isEmpty() && AL10.alGetSourcei(this.speakersSourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                                sleep(1);
                                if (!this.videoProjector.isRendering()) break;
                            }
                            this.videoProjector.pause();
                            this.speakers.restart();
                            this.speakers.pause();
                            break;

                        case PLAY_UNTIL_END:
                            this.seekWithoutClearingBuffer(0);
                            while (!this.textureBuffer.isEmpty() && !this.audioBuffer.isEmpty() && AL10.alGetSourcei(this.speakersSourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                                sleep(1);
                                if (!this.videoProjector.isRendering()) break;
                            }
                            this.videoProjector.pause();
                            this.speakers.restart();
                            this.speakers.pause();
                            break;
                            
                        case LOOP:
                            this.seekWithoutClearingBuffer(0);
                            continue;
                        default:
                            break;
                    }
                }
            } else {
                sleep(1);
            }
        }
        // print("DecoderWithSound decodeLoop ended");
    }

    @Override
    public boolean isRunning() {
        return this.running;
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

    public int getCurrentVideoTextureIdDoNotUpdatePts() {
        while (textureBuffer.isEmpty()) sleep(1); 

        synchronized(textureBuffer) {
            textureBuffer.update();
        }
        return currentVideoTextureId;
    }

    public float getVideoFps() { return videoFps; }

    public void start(long startUs) {
        if (this.running) return;
        // print("Starting DecoderWithSound for file", videoFilePath);
        this.running = true;

        this.ctxPtr = FFmpeg.openCtx(this.videoFilePath, this.width, this.height, startUs);
        // print("Opened FFmpeg ctx, ptr =", this.ctxPtr);

        if (this.ctxPtr == 0) throw new RuntimeException("Failed to initiate FFmpeg ctx context for " + this.videoFilePath);

        this.videoDurationSeconds = FFmpeg.getDurationSeconds(this.ctxPtr);
        this.videoDurationUs = FFmpeg.getDurationUs(this.ctxPtr);

        this.videoFps = FFmpeg.getVideoFps(this.ctxPtr);
        this.spf = 1 / this.videoFps;
        // print("Video Framerate =", videoFps);
        // print("Video Duration=", videoDurationSeconds);
        // print("Video DurationUs=", videoDurationUs);

        // boolean isRGBA = FFmpeg.isRGBA(this.ctxPtr);
        // print("isRGBA=", isRGBA);
        // this.textureBuffer = isRGBA ? new RGBATextureBuffer(30) : new TextureBuffer(30);
        this.textureBuffer = new RGBATextureBuffer(30);
        this.textureBuffer.initTexStorage(this.width, this.height);
        this.currentVideoTextureId = this.textureBuffer.getTextureId();
        // this.textureBuffer = isRGBA ? new RGBATextureBufferList() : new TextureBufferList();

        this.audioChannels = FFmpeg.getAudioChannels(this.ctxPtr);
        this.audioSampleRate = FFmpeg.getAudioSampleRate(this.ctxPtr);
        // print("Audio Channels=", audioChannels);
        // print("Audio Sample Rate=", audioSampleRate);

        this.videoDecodeThread = new Thread(this::decodeLoop, "DecoderWithSound-decodeLoop");
        this.videoDecodeThread.start();
        while(!this.textureBuffer.isFull() && !this.audioBuffer.isFull()) sleep(1);

        // print("DecoderWithSound decoderLoop thread started");
        return;
    }

    public void finish() {
        print("Stopping DecoderWithSound thread");
        this.running = false;
        this.timeAccumulator = 0f;
        this.videoFps = 0f;

        // print("Joining DecoderWithSound thread");
        try {
            this.videoDecodeThread.join();
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (this.ctxPtr != 0) {
            // print("Closing FFmpeg ctx");
            FFmpeg.closeCtx(this.ctxPtr);
            this.ctxPtr = 0;
        }

        // print("Clearing Texture/Video Buffer");
        synchronized(this.textureBuffer) {
            this.textureBuffer.clear();
            this.textureBuffer.cleanupTexStorage();
        }
        synchronized(this.audioBuffer) {
            this.audioBuffer.clear();
        }
    }

    public void stop() {
        this.PLAY_MODE = PlayMode.STOPPED;
        this.timeAccumulator = 0f;
        seek(0);
    }

    public void restart(long startUs) {
        finish();
        start(startUs);
    }

    public void seek(long targetUs) {
        synchronized(this.seekLock) {
            // print("Seeking to", targetUs, "µs");

            FFmpeg.seek(this.ctxPtr, targetUs);
    
            synchronized(this.textureBuffer) {
                this.textureBuffer.clear();
            }
            synchronized(this.audioBuffer) {
                this.audioBuffer.clear();
            }
            this.speakers.stop();
            this.speakers.notifySeek(targetUs);

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

    public AudioFrameBuffer getAudioBuffer() {
        return this.audioBuffer;
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
        return FFmpeg.getErrorStatus(this.ctxPtr);
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

    public String getVideoFilePath() {
        return this.videoFilePath;
    }
}