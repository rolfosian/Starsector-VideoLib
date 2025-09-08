package data.scripts.buffers;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;

public class RGBATextureBuffer extends TextureBuffer {

    public RGBATextureBuffer(int capacity) {
        super(capacity);
    }

    @Override
    protected int createGLTextureFromFrame(ByteBuffer frameBuffer, int width, int height) {
        if (frameBuffer == null) return -1;
        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                          GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, frameBuffer);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return textureId;
    }
    
}
