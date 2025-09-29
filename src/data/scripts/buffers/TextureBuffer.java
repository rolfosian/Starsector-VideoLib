package data.scripts.buffers;

import java.nio.ByteBuffer;
import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;

import data.scripts.ffmpeg.VideoFrame;

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

    protected final VideoFrame[] videoFrames;
    protected final TextureFrame[] textures;

    protected int size;
    protected final int capacity;
    protected int head;
    protected int tail;

    public TextureBuffer(int capacity) {
        this.capacity = capacity;
        this.textures = new TextureFrame[capacity];
        this.videoFrames = new VideoFrame[capacity];
        this.size = 0;
        this.head = 0;
        this.tail = 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == capacity;
    }

    public void add(VideoFrame frame) {
        videoFrames[tail] = frame;
        textures[tail] = null;
        tail = (tail + 1) % capacity;
        size++;
    }

    // remember to glDeleteTextures with removed.id later (and only call this on the main thread)
    public TextureFrame popFront(int width, int height) {
        if (isEmpty()) return null;
        TextureFrame removed = textures[head];

        if (removed == null && videoFrames[head] != null) {
            removed = new TextureFrame(
                createGLTextureFromFrame(videoFrames[head].buffer, width, height),
                videoFrames[head].pts
            );
            videoFrames[head].freeBuffer();
            videoFrames[head] = null;
        }
        textures[head] = null;
        
        head = (head + 1) % capacity;
        size--;
        return removed;
    }

    public void convertSome(int width, int height, int maxConversions) {
        int idx = head;
        for (int i = 0; i < size && maxConversions > 0; i++) {
            if (videoFrames[idx] != null && textures[idx] == null) {
                textures[idx] = new TextureFrame(
                    createGLTextureFromFrame(videoFrames[idx].buffer, width, height),
                    videoFrames[idx].pts
                );
                
                videoFrames[idx].freeBuffer();
                videoFrames[idx] = null;
                maxConversions--;
            }
            idx = (idx + 1) % capacity;
        }
    }    

    // should probably do something about this, it might choke the main thread if too much is goign on, need a new method with dynamic conversion amount based on meta deltaTime between videofps, gamefps, and maximum gamefps
    public void convertAll(int width, int height) {
        int idx = head;
        for (int i = 0; i < size; i++) {
            if (videoFrames[idx] != null && textures[idx] == null) {
                textures[idx] = new TextureFrame(
                    createGLTextureFromFrame(videoFrames[idx].buffer, width, height),
                    videoFrames[idx].pts
                );

                videoFrames[idx].freeBuffer();
                videoFrames[idx] = null;
            }
            idx = (idx + 1) % capacity;
        }
    }

    public void convertFront(int width, int height) {
        if (isEmpty()) return;
        if (videoFrames[head] != null && textures[head] == null) {
            textures[head] = new TextureFrame(
                createGLTextureFromFrame(videoFrames[head].buffer, width, height),
                videoFrames[head].pts
            );

            videoFrames[head].freeBuffer();
            videoFrames[head] = null;
        }
    }

    public void clear() {
        int idx = head;
        for (int i = 0; i < size; i++) {
            if (textures[idx] != null) {
                GL11.glDeleteTextures(textures[idx].id);
                textures[idx] = null;
            }
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

    // this can only be called on the main thread as we need the thread's context to upload and render these textures on the main thread also GL11 is not thread safe
    protected int createGLTextureFromFrame(ByteBuffer frameBuffer, int width, int height) {
        if (frameBuffer == null) return -1;
        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, width, height, 0,
                          GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, frameBuffer);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return textureId;
    }
}