package videolib.decoder;

import videolib.ffmpeg.FFmpeg;
import videolib.ffmpeg.VideoFrame;

import videolib.projector.Projector;
import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;

import videolib.buffers.CachingTextureBuffer;
import videolib.buffers.TextureFrame;

import videolib.VideoLibModPlugin;

import org.lwjgl.opengl.GL11;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**Definitely do not use this it will take like 5GB of vram for a single 30fps 1 min long 720p video lmao*/
public class CachingMuteDecoder implements Decoder {
    private static final Logger logger = Logger.getLogger(CachingMuteDecoder.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    private PlayMode PLAY_MODE;
    private EOFMode EOF_MODE;

    private String videoFilePath;
    private volatile boolean running = false;
    private Thread decodeThread;
    private long pipePtr;
    private CachingTextureBuffer textureBuffer;

    private float videoFps = 0f;
    private float spf = 0f;
    private double videoDurationSeconds = 0;
    private long videoDurationUs = 0;
    private long totalFrameCount = 0;

    private int currentVideoTextureId = 0;
    private long currentVideoPts = 0;

    public final Projector videoProjector;

    private int width;
    private int height;

    private float timeAccumulator = 0f;

    public CachingMuteDecoder(Projector videoProjector, String videoFilePath, int width, int height, PlayMode startingPlayMode, EOFMode startingEOFMode) {
        print("Initializing CachingMuteDecoder");
        this.videoProjector = videoProjector;
        this.videoFilePath = videoFilePath;
        this.width = width;
        this.height = height;
        this.PLAY_MODE = startingPlayMode;
        this.EOF_MODE = startingEOFMode;
    }

    private void decodeLoop() {
        // print("CachingMuteDecoder decodeLoop started");

        List<VideoFrame> allFrames = new ArrayList<>();
        
        while (running) {
            VideoFrame f = FFmpeg.readFrameNoSound(pipePtr);

            if (f == null) { // EOF / Error
                if (FFmpeg.getErrorStatus(pipePtr) != FFmpeg.AVERROR_EOF) {
                    logger.error("FFmpeg error for file " + videoFilePath + ": " + FFmpeg.getErrorMessage(pipePtr)
                    + ", interrupting main thread...",
                    new RuntimeException(FFmpeg.getErrorMessage(pipePtr)));

                    FFmpeg.closePipe(pipePtr);
                    pipePtr = 0;
                    VideoLibModPlugin.getMainThread().interrupt();
                    return;
                }

                // EOF reached - create CachingTextureBuffer with all frames
                // print("EOF reached, creating CachingTextureBuffer with", allFrames.size(), "frames");
                textureBuffer = new CachingTextureBuffer(width, height, allFrames);
                // print("Closing FFmpeg Pipe");
                FFmpeg.closePipe(pipePtr);
                pipePtr = 0;
                // print("CachingMuteDecoder decodeLoop ended - buffer filled");
                return;
            } else {
                allFrames.add(f);
            }
        }
        // print("CachingMuteDecoder decodeLoop ended");
    }

    public int getCurrentVideoTextureId(float deltaTime) {
        if (textureBuffer == null) return 0;
        
        timeAccumulator += deltaTime;

        while (timeAccumulator >= spf) {
            timeAccumulator -= spf;
            
            TextureFrame texture = textureBuffer.pop(width, height);

            if (texture != null) {
                if (currentVideoTextureId != 0) textureBuffer.deleteTexture(currentVideoTextureId);

                currentVideoTextureId = texture.id;
                currentVideoPts = texture.pts;
            }
        }

        return currentVideoTextureId;
    }

    public int getCurrentVideoTextureId() {
        if (textureBuffer == null) return 0;
        
        while (textureBuffer.isEmpty()) sleep(1); 

        TextureFrame texture = textureBuffer.pop(width, height);

        if (texture != null) {
            int oldTextureId = currentVideoTextureId;

            currentVideoTextureId = texture.id;
            currentVideoPts = texture.pts;

            if (oldTextureId != 0 && oldTextureId != currentVideoTextureId) textureBuffer.deleteTexture(oldTextureId);

            videoProjector.setIsRendering(true); // this is dumb, i dont like this
        }
    
        return currentVideoTextureId;
    }

    public float getVideoFps() { return videoFps; }

    public void start(long startUs) {
        if (running) return;
        // print("Starting CachingMuteDecoder for file", videoFilePath);
        running = true;

        pipePtr = FFmpeg.openPipeNoSound(videoFilePath, width, height, startUs);
        // print("Opened FFmpeg pipe, ptr =", pipePtr);

        if (pipePtr == 0) throw new RuntimeException("Failed to initiate FFmpeg pipe context for " + videoFilePath);

        videoDurationSeconds = FFmpeg.getDurationSeconds(pipePtr);
        videoDurationUs = FFmpeg.getDurationUs(pipePtr);
        totalFrameCount = FFmpeg.getTotalFrameCount(pipePtr); // Calculated on-demand, may be exact or estimated depending on video format

        videoFps = FFmpeg.getVideoFps(pipePtr);
        spf = 1 / videoFps;
        // print("Video Framerate =", videoFps);
        // print("Video Duration=", videoDurationSeconds);
        // print("Video DurationUs=", videoDurationUs);
        // print("Total Frame Count=", totalFrameCount);

        decodeThread = new Thread(this::decodeLoop, "CachingMuteDecoder");
        decodeThread.start();
        // print("CachingMuteDecoder decoderLoop thread started");

        // Wait for the buffer to be filled
        while (textureBuffer == null || !textureBuffer.ready()) sleep(1);
        textureBuffer.convertAll(width, height);
        // print("CachingTextureBuffer filled, ready for playback");
        return;
    }

    public void finish() {
        // print("Stopping CachingMuteDecoder decoderLoop thread");
        running = false;
        timeAccumulator = 0f;
        videoFps = 0f;
        textureBuffer.clear();
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
        textureBuffer.seek(targetUs);
    }

    public void seekWithoutClearingBuffer(long targetUs) {}

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public CachingTextureBuffer getTextureBuffer() {
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
