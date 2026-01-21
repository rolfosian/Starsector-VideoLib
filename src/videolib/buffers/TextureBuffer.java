package videolib.buffers;

import java.nio.ByteBuffer;
import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;

import videolib.ffmpeg.VideoFrame;

public class TextureBuffer implements TexBuffer {
    private static final Logger logger = Logger.getLogger(TextureBuffer.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    protected final int capacity;
    protected final VideoFrame[] videoFrames;
    protected int textureId;
    protected int width;
    protected int height;

    protected int size;
    protected int head;
    protected int tail;

    protected long lastRemovedPts;

    public TextureBuffer(int capacity) {
        this.capacity = capacity;

        this.videoFrames = new VideoFrame[capacity];

        this.size = 0;
        this.head = 0;
        this.tail = 0;
    }

    @Override
    public int getTextureId() {
        return this.textureId;
    }

    @Override
    public void initTexStorage(int width, int height) {
        int previousAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        if (this.textureId != 0) GL11.glDeleteTextures(this.textureId);
        this.textureId = GL11.glGenTextures();

        this.width = width;
        this.height = height;

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGB,
            width,
            height,
            0,
            GL11.GL_RGB,
            GL11.GL_UNSIGNED_BYTE,
            (ByteBuffer) null
        );
        
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, previousAlignment);
    }
    
    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean isFull() {
        return size == capacity;
    }

    @Override
    public void add(VideoFrame frame) {
        videoFrames[tail] = frame;
        tail = (tail + 1) % capacity;
        size++;
    }

    @Override
    public long update() {
        if (size == 0) return lastRemovedPts;
        VideoFrame removed = null;

        if (videoFrames[head] != null) {
            removed = videoFrames[head];
            lastRemovedPts = removed.pts;
            updateTexture(removed.buffer);
            removed.freeBuffer();

            videoFrames[head] = null;
        }
        
        head = (head + 1) % capacity;
        size--;

        return removed == null ? lastRemovedPts : removed.pts;
    }

    @Override
    public void clear() {
        int idx = head;
        for (int i = 0; i < size; i++) {
            if (videoFrames[idx] != null) {
                videoFrames[idx].freeBuffer();
                videoFrames[idx] = null;
            }
            
            idx = (idx + 1) % capacity;
        }
        
        size = 0;
        head = 0;
        tail = 0;
    }

    protected void updateTexture(ByteBuffer frameBuffer) {
        int previousAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
    
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
    
        GL11.glTexSubImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            0,
            0,
            width,
            height,
            GL11.GL_RGB,
            GL11.GL_UNSIGNED_BYTE,
            frameBuffer
        );
    
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, previousAlignment);
    }

    public void cleanupTexStorage() {
        GL11.glDeleteTextures(textureId);
    }
}