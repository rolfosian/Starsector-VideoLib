package data.scripts.buffers;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;

import data.scripts.ffmpeg.VideoFrame;

public class TextureBufferList implements TexBuffer {
    private static final Logger logger = Logger.getLogger(TextureBufferList.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    private final List<VideoFrame> videoFrames;
    private final List<TextureFrame> textures;

    private final int maxActiveTextures;
    protected int activeTextures = 0;

    public TextureBufferList(int maxActiveTextures) {
        this.videoFrames = new ArrayList<>();
        this.textures = new ArrayList<>();
        this.maxActiveTextures = maxActiveTextures;
    }

    public int size() {
        return videoFrames.size();
    }

    public boolean isEmpty() {
        return videoFrames.isEmpty();
    }

    public boolean isFull() {
        return false;
    }

    public void add(VideoFrame frame) {
        videoFrames.add(frame);
        textures.add(null);
    }

    // remember to glDeleteTextures with removed.id later (and only call this on the main thread)
    public TextureFrame popFront(int width, int height) {
        if (isEmpty()) return null;
        
        TextureFrame removed = textures.get(0);

        if (removed == null && videoFrames.get(0) != null) {
            removed = new TextureFrame(
                createGLTextureFromFrame(videoFrames.get(0).buffer, width, height),
                videoFrames.get(0).pts
            );
            videoFrames.get(0).freeBuffer();
        }
        
        videoFrames.remove(0);
        textures.remove(0);
        
        return removed;
    }

    public void convertSome(int width, int height, int maxConversions) {
        for (int i = 0; i < videoFrames.size() && maxConversions > 0; i++) {
            if (activeTextures >= maxActiveTextures) break;
            if (videoFrames.get(i) != null && textures.get(i) == null) {
                textures.set(i, new TextureFrame(
                    createGLTextureFromFrame(videoFrames.get(i).buffer, width, height),
                    videoFrames.get(i).pts
                ));
                
                videoFrames.get(i).freeBuffer();
                videoFrames.set(i, null);
                maxConversions--;
            }
        }
    }

    public void convertAll(int width, int height) {
        for (int i = 0; i < videoFrames.size(); i++) {
            if (videoFrames.get(i) != null && textures.get(i) == null) {
                textures.set(i, new TextureFrame(
                    createGLTextureFromFrame(videoFrames.get(i).buffer, width, height),
                    videoFrames.get(i).pts
                ));

                videoFrames.get(i).freeBuffer();
                videoFrames.set(i, null);
            }
        }
    }

    public void convertFront(int width, int height) {
        if (isEmpty() || activeTextures >= maxActiveTextures) return;
        
        if (videoFrames.get(0) != null && textures.get(0) == null) {
            textures.set(0, new TextureFrame(
                createGLTextureFromFrame(videoFrames.get(0).buffer, width, height),
                videoFrames.get(0).pts
            ));

            videoFrames.get(0).freeBuffer();
            videoFrames.set(0, null);
        }
    }

    public void clear() {
        for (int i = 0; i < videoFrames.size(); i++) {
            TextureFrame tFrame = textures.get(i);
            if (tFrame != null) {
                deleteTexture(tFrame.id);
            }
            VideoFrame vFrame = videoFrames.get(i);
            if (vFrame != null) {
                vFrame.freeBuffer();
            }
        }
        videoFrames.clear();
        textures.clear();
    }

    protected int createGLTextureFromFrame(ByteBuffer frameBuffer, int width, int height) {
        if (frameBuffer == null) return -1;
        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, width, height, 0,
                          GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, frameBuffer);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        activeTextures++;
        return textureId;
    }

    @Override
    public void deleteTexture(int id) {
        GL11.glDeleteTextures(id);
        activeTextures--;
    }
}