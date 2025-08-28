package data.scripts;

import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.PositionAPI;

// original concept do not edit
// shells out to ffmpeg, very inefficient but worked somewhat. no audio support, very messy and hard to read, surprised it even worked
public class VideoProjector extends BaseCustomUIPanelPlugin {
    private static final Logger logger = Logger.getLogger(VideoProjector.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    private String videoFilePath;
    private Process ffmpegProcess;
    private InputStream ffmpegOut;
    private Thread ffmpegErrDrainer;
    private int width, height;
    private FrameBuffer frameBuffer;
	private int currentFrame = 0;

	// playback/texture state
	private int currentTextureId = 0;
	private int displayedFrameIndex = -1;
	private int pendingFrameIndex = -1;
	private float timeAccumulator = 0f;
	private static final float FALLBACK_FPS = 30f;

	private float videoFps = 0f;
	public boolean isPlaying = false;
	public float x = 0f;
	public float y = 0f;

    private static final String[] FFPROBE_GET_VIDEO_FPS_CMD = {
        "ffprobe",
        "-v", "error",
        "-select_streams", "v:0",
        "-show_entries", "stream=r_frame_rate",
        "-of", "default=noprint_wrappers=1:nokey=1"
    };
    
    private float getVideoFps(String videoFilePath) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(FFPROBE_GET_VIDEO_FPS_CMD);
        pb.command().add(videoFilePath);
        
        try {
            Process process = pb.start();

            String fpsLine = new String(process.getInputStream().readAllBytes());
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Failed to retrieve FPS: Exit code " + exitCode);
            }
    
            return parseFpsValue(fpsLine.trim());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Operation interrupted while getting FPS");
        }
    }
    
    private float parseFpsValue(String fpsStr) {
        if (fpsStr == null || fpsStr.isEmpty()) {
            return 0f;
        }
        
        try {
            if (fpsStr.contains("/")) {
                String[] parts = fpsStr.split("/", 2);

                float numerator = Float.parseFloat(parts[0]);
                float denominator = Float.parseFloat(parts[1]);

                return denominator != 0 ? (numerator / denominator) : 0f;
            }

            return Float.parseFloat(fpsStr);

        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    public VideoProjector(String videoFilePath, int width, int height, int bufferSize) {
        try {
            this.videoFilePath = videoFilePath;
            this.width = width;
            this.height = height;
            this.frameBuffer = new FrameBuffer(bufferSize);
            this.videoFps = getVideoFps(videoFilePath);
            startFfmpegProcess(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void init(PositionAPI panelPos) {
        this.x = panelPos.getX();
        this.y = panelPos.getY();
    }

    @Override
	public void advance(float amount) {
		// advance is called by game once per frame
		if (!isPlaying) return;

		float fps = videoFps > 0f ? videoFps : FALLBACK_FPS;
		if (fps <= 0f) return;

		float spf = 1f / fps;
		timeAccumulator += amount;

		while (timeAccumulator >= spf) {
			timeAccumulator -= spf;

			int base = pendingFrameIndex >= 0 ? pendingFrameIndex : displayedFrameIndex;
			pendingFrameIndex = base + 1;
		}
	}

	@Override
	public void render(float alphaMult) {
		if (!isPlaying) return;

		// swap to the next frame only when it's time, disposing the old texture
		if (pendingFrameIndex > displayedFrameIndex) {
			try {

				int newTextureId = createGLTextureFromFrame(pendingFrameIndex);
				int oldTextureId = currentTextureId;

				currentTextureId = newTextureId;
				displayedFrameIndex = pendingFrameIndex;

				if (oldTextureId != 0) {
					GL11.glDeleteTextures(oldTextureId);
				}

			} catch (IOException e) {
                throw new RuntimeException(e);
				// isPlaying = false;
				// return;
			}
		}

		if (currentTextureId != 0) {
			drawTextureAt(currentTextureId, x, y, width, height, alphaMult);
		}
	}

    public void start() {
        if (!isPlaying) {
            isPlaying = true;
        }
    }

    public void stop() {
		isPlaying = false;
		frameBuffer.clear();
		if (ffmpegProcess != null) {
			ffmpegProcess.destroy();
			ffmpegProcess = null;
			ffmpegOut = null;
			if (ffmpegErrDrainer != null) {
				ffmpegErrDrainer.interrupt();
				ffmpegErrDrainer = null;
			}
		}
		if (currentTextureId != 0) {
			GL11.glDeleteTextures(currentTextureId);
			currentTextureId = 0;
		}
		displayedFrameIndex = -1;
		pendingFrameIndex = -1;
		timeAccumulator = 0f;
		currentFrame = 0;
	}

    public void pause() {
        isPlaying = false;
    }

    private void startFfmpegProcess(int startFrame) throws IOException {
        if (ffmpegProcess != null) ffmpegProcess.destroy();
        ProcessBuilder pb = new ProcessBuilder(
            "ffmpeg",
            "-hide_banner",
            "-loglevel", "quiet",
            "-nostats",
            "-nostdin",
            "-i", videoFilePath,
            "-vf", "scale=" + width + ":" + height + ",select=gte(n\\," + startFrame + ")",
            "-f", "rawvideo",
            "-pix_fmt", "rgb24",
            "-"
        );
        ffmpegProcess = pb.start();
        ffmpegOut = ffmpegProcess.getInputStream();
        currentFrame = startFrame;
    }

    private byte[] readNextFrame() throws IOException {
        int frameSize = width * height * 3;
        byte[] frameData = new byte[frameSize];
        int read = 0;
        while (read < frameSize) {
            int r = ffmpegOut.read(frameData, read, frameSize - read);
            if (r == -1) return null;
            read += r;
        }
        frameBuffer.add(frameData);
        currentFrame++;
        return frameData;
    }

    public byte[] getFrame(int frameIndex) throws IOException {
        int bufferStart = currentFrame - frameBuffer.size();
        int bufferEnd = currentFrame - 1;

        if (frameIndex >= bufferStart && frameIndex <= bufferEnd) {
            return frameBuffer.get(frameIndex - bufferStart);

        } else if (frameIndex >= currentFrame) {
            while (currentFrame <= frameIndex) readNextFrame();
            return frameBuffer.get(frameBuffer.size() - 1);

        } else {
            // Backward beyond buffer: replace main ffmpeg process
            startFfmpegProcess(frameIndex); // destroys old process and starts new one
            frameBuffer.clear();

            int frameSize = width * height * 3;
            byte[] buf = new byte[frameSize];

            // Prefill the buffer from the new persistent process
            for (int i = 0; i < frameBuffer.capacity; i++) {
                int read = 0;
                while (read < frameSize) {
                    int r = ffmpegOut.read(buf, read, frameSize - read);
                    if (r == -1) break;
                    read += r;
                }
                if (read < frameSize) break;
                frameBuffer.add(buf.clone());
                currentFrame++;
            }
        }

        return frameBuffer.get(0); // return first frame from buffer
    }

    public int createGLTextureFromFrame(int frameIndex) throws IOException {
        byte[] frameData = getFrame(frameIndex);
        ByteBuffer buffer = BufferUtils.createByteBuffer(frameData.length);
        buffer.put(frameData).flip();

        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, width, height, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return textureId;
    }

    public void drawTextureAt(int textureId, float x, float y, float width, float height, float alphaMult) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, alphaMult);
    
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 1f);
        GL11.glVertex2f(x, y);
    
        GL11.glTexCoord2f(1f, 1f);
        GL11.glVertex2f(x + width, y);
    
        GL11.glTexCoord2f(1f, 0f);
        GL11.glVertex2f(x + width, y + height);
    
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
    
        GL11.glColor4f(1f, 1f, 1f, 1f); // reset color
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    public class FrameBuffer {
        private byte[][] buffer;
        private int head = 0;
        private int tail = 0;
        private int count = 0;
        private int capacity;

        public FrameBuffer(int capacity) {
            this.capacity = capacity;
            this.buffer = new byte[capacity][];
        }

        public void add(byte[] frame) {
            buffer[tail] = frame;
            tail = (tail + 1) % capacity;
            if (count < capacity) count++;
            else head = (head + 1) % capacity;
        }

        public byte[] get(int index) {
            if (index < 0 || index >= count) return null;
            return buffer[(head + index) % capacity];
        }

        public void clear() {
            head = tail = count = 0;
        }

        public int size() { return count; }
    }
}