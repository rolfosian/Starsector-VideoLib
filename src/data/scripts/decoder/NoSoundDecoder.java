package data.scripts.decoder;

import data.scripts.ffmpeg.FFmpeg;
import data.scripts.ffmpeg.VideoFrame;
import data.scripts.projector.NoSoundVideoProjector;
import data.scripts.VideoMode;
import data.scripts.buffers.TextureBuffer;
import data.scripts.buffers.TextureFrame;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.campaign.ui.marketinfo.p;

import org.apache.log4j.Logger;

public class NoSoundDecoder implements Decoder {
    private static final Logger logger = Logger.getLogger(NoSoundDecoder.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    private static int MAX_BUF_SIZE = 60;

    private final Thread mainThread;

    private volatile VideoMode MODE;
    private volatile VideoMode OLD_MODE;

    private String videoFilePath;
    private volatile boolean running = false;
    private Thread decodeThread;
    private long pipePtr;
    private TextureBuffer textureBuffer = new TextureBuffer(MAX_BUF_SIZE);

    private float gameFps = 0f;
    private float videoFps = 0f;
    private float spf = 0f;
    private double videoDurationSeconds = 0;
    private long videoDurationUs = 0;

    private volatile int currentVideoTextureId = 0;
    private volatile long currentVideoPts = 0;
    private volatile long seekedTo = 0;

    public final NoSoundVideoProjector videoProjector;

    private int width;
    private int height;

    private float timeAccumulator = 0f;

    public NoSoundDecoder(NoSoundVideoProjector videoProjector, String videoFilePath, int width, int height, VideoMode mode) {
        print("Initializing NoSoundDecoder");
        this.videoProjector = videoProjector;
        this.videoFilePath = videoFilePath;
        this.width = width;
        this.height = height;
        this.MODE = mode;
        this.mainThread = Thread.currentThread();
    }

    private void decodeLoop() {
        print("NoSoundDecoder decodeLoop started");

        outer:
        while (running) {
            if (!textureBuffer.isFull()) {
                VideoFrame f = FFmpeg.readFrameNoSound(pipePtr);
                if (f == null) { // EOF / Error
                    switch(this.MODE) {
                        case LOOP:
                            while(!textureBuffer.isEmpty()) {
                                if (!videoProjector.isPlaying()) {
                                    continue outer;
                                }
                                sleep(1);
                            }

                            if (currentVideoPts != 0) {
                                seek(0);
                                currentVideoPts = 0;
                            } else {
                                sleep(1);
                            }
                            continue;

                        case PLAY_UNTIL_END:
                            while(!textureBuffer.isEmpty()) {
                                if (!videoProjector.isPlaying()) {
                                    if (currentVideoPts != 0) {
                                        seek(0);
                                        currentVideoPts = 0;
                                    }
                                    continue outer;
                                }
                                sleep(1);
                            }

                            videoProjector.stop();
                            continue;
                        
                        case PAUSED:
                            while(!textureBuffer.isEmpty()) {
                                if (!videoProjector.isPlaying()) {
                                    continue outer;
                                }
                                sleep(1);
                            }
                            sleep(1);
                            continue;
                        
                        case STOPPED:
                            while(!textureBuffer.isEmpty()) {
                                if (!videoProjector.isPlaying()) {
                                    if (currentVideoPts != 0) {
                                        seek(0);
                                        currentVideoPts = 0;
                                    }
                                    continue outer;
                                }
                                sleep(1);
                            }

                            if (currentVideoPts != 0) {
                                seek(0);
                                currentVideoPts = 0;
                            } else {
                                sleep(1);
                            }
                            continue;
                        
                        case SEEKING:
                            sleep(1);
                            continue;
                        
                        case FINISHED:
                            break outer;

                        default:
                            throw new IllegalStateException("No mode set");
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
        print("NoSoundDecoder decodeLoop ended");
    }

    public int requestCurrentVideoTextureId(float deltaTime) {
        gameFps = 1 / deltaTime;
        timeAccumulator += deltaTime;

        synchronized(textureBuffer) {
            boolean switched = false;

            while (timeAccumulator >= spf) {
                timeAccumulator -= spf;
                
                if (currentVideoTextureId != 0) GL11.glDeleteTextures(currentVideoTextureId);
                
                TextureFrame texture = textureBuffer.popFront(width, height);

                if (texture != null) {
                    switched = true;
                    currentVideoTextureId = texture.id;
                    currentVideoPts = texture.pts;
                }
            }

            if (!switched) {

                if (gameFps <= videoFps) {
                    textureBuffer.convertFront(width, height);

                } else {
                    textureBuffer.convertSome(width, height, Math.round(gameFps / videoFps) + 2);
                }
            }
        }

        return currentVideoTextureId;
    }

    public int requestCurrentVideoTextureId() {
        while (textureBuffer.isEmpty()) {
            sleep(1);
        } 

        synchronized(textureBuffer) {
            TextureFrame texture = textureBuffer.popFront(width, height);

            if (texture != null) {
                int oldTextureId = currentVideoTextureId;

                currentVideoTextureId = texture.id;
                currentVideoPts = texture.pts;

                if (oldTextureId != 0) GL11.glDeleteTextures(oldTextureId);
            }
        }

        return currentVideoTextureId;
    }

    public float getVideoFps() { return videoFps; }

    public TextureBuffer start() {
        if (running) return null;
        print("Starting NoSoundDecoder for file", videoFilePath);
        running = true;

        pipePtr = FFmpeg.openPipe(videoFilePath, width, height, 0);
        print("Opened FFmpeg pipe, ptr =", pipePtr);

        videoDurationSeconds = FFmpeg.getDurationSeconds(pipePtr);
        videoDurationUs = FFmpeg.getDurationUs(pipePtr);

        videoFps = FFmpeg.getVideoFps(pipePtr);
        spf = 1 / videoFps;
        print("Video Framerate =", videoFps);
        print("Video Duration=", videoDurationSeconds);
        print("Video DurationUs=", videoDurationUs);

        decodeThread = new Thread(this::decodeLoop, "NoSoundDecoder");
        decodeThread.start();
        print("NoSoundDecoder decoderLoop thread started");

        while(textureBuffer.isEmpty()) sleep(1);
        synchronized(textureBuffer) {
            textureBuffer.convertFront(width, height);
        }
        return textureBuffer;
    }

    public void finish() {
        print("Stopping NoSoundDecoder decoderLoop thread");
        running = false;
        timeAccumulator = 0f;
        videoFps = 0f;
        MODE = VideoMode.FINISHED;

        print("Joining NoSoundDecoder decoderLoop thread");
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

        print("Clearing Texture/Video Buffer");
        textureBuffer.clear();
        // textureBuffer.glDeleteBuffers();
    }

    public void stop() {
        MODE = VideoMode.STOPPED;
        timeAccumulator = 0f;
        seek(0);
    }

    public void reset() {
        stop();
        start();
    }

    public void restart() {
        finish();
        start();
    }

    public void seek(long targetUs) {
        print("Seeking to", targetUs, "µs");
        synchronized (this) {
            FFmpeg.seek(pipePtr, targetUs);

            synchronized(textureBuffer) {
                textureBuffer.clear();
            }
            this.currentVideoPts = targetUs;
        }
    }

    public void seek(double targetSecond) {
        print("Seeking to", targetSecond, "seconds");
        long targetUs = (long)(targetSecond * 1_000_000);
        synchronized(this) {
            FFmpeg.seek(pipePtr, targetUs);

            synchronized(textureBuffer) {
                textureBuffer.clear();
            }
            this.currentVideoPts = targetUs;
        }
    }

    public TextureBuffer gTextureBuffer() {
        return this.textureBuffer;
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

    public VideoMode getMode() {
        return this.MODE;
    }
    
    public void setMode(VideoMode newMode) {
        print("Setting Mode", newMode);
        this.OLD_MODE = this.MODE;
        this.MODE = newMode;
    }    

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch(InterruptedException ignored) {}
    }
}
