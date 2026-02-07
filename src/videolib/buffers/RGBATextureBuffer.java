package videolib.buffers;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;

public class RGBATextureBuffer extends TextureBuffer {
    public RGBATextureBuffer(int capacity, int textureId, int width, int height) {
        super(capacity);

        this.textureId = textureId;
        this.width = width;
        this.height = height;
    }

    public RGBATextureBuffer(int capacity) {
        super(capacity);
    }

    @Override
    public void initTexStorage(int width, int height) {
        if (this.textureId != 0) GL11.glDeleteTextures(this.textureId);
        this.textureId = GL11.glGenTextures();

        this.width = width;
        this.height = height;

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA,
            width,
            height,
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            (ByteBuffer) null
        );
        
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    @Override
    protected void updateTexture(ByteBuffer frameBuffer) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        GL11.glTexSubImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            0,
            0,
            width,
            height,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            frameBuffer
        );
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
}
