package data.scripts.projector;

import java.nio.ByteBuffer;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import data.scripts.VideoPaths;
import data.scripts.ffmpeg.FFmpeg;

/**Supports JPEG, PNG, WEBP, and GIF (stills) */
public class ImagePlugin implements CustomUIPanelPlugin {
    protected String fileId;
    protected int textureId;
    protected int width;
    protected int height;
    
    protected long ptr;
    protected String filePath;

    private float x;
    private float y;

    private int advancingValue = 0;
    private int checkAdvancing = 0;

    public ImagePlugin(String fileId, int width, int height, boolean keepAlive) {
        this.fileId = fileId;
        this.filePath = VideoPaths.getImagePath(fileId);
        this.width = width;
        this.height = height;

        this.ptr = FFmpeg.openImage(filePath, width, height);
        this.textureId = createGLTextureFromFrame(FFmpeg.getImageBuffer(ptr), width, height);

        if (!keepAlive)
        Global.getSector().addScript(new EveryFrameScript() {
            private boolean isDone = false;

            @Override
            public void advance(float arg0) {
                checkAdvancing ^= 1;

                if (!(advancingValue == checkAdvancing)) {
                    GL11.glDeleteTextures(textureId);
                    FFmpeg.closeImage(ptr);
                    isDone = true;
                    Global.getSector().removeScript(this);
                }
            }

            @Override
            public boolean isDone() {
                return isDone;
            }

            @Override
            public boolean runWhilePaused() {
                return true;
            }
        });
    }

    public void setImage(String fileId, int width, int height) {
        GL11.glDeleteTextures(textureId);
        FFmpeg.closeImage(ptr);

        this.fileId = fileId;
        this.filePath = VideoPaths.getImagePath(fileId);
        this.width = width;
        this.height = height;
        this.ptr = FFmpeg.openImage(filePath, width, height);

        this.textureId = createGLTextureFromFrame(FFmpeg.getImageBuffer(ptr), width, height);
    }

    public void resize(int width, int height) {
        GL11.glDeleteTextures(textureId);

        FFmpeg.resizeImage(ptr, width, height);
        this.textureId = createGLTextureFromFrame(FFmpeg.getImageBuffer(ptr), width, height);

        this.width = width;
        this.height = height;
    }

    public void init(PositionAPI pos, CustomPanelAPI panel) {
        this.x = pos.getX();
        this.y = pos.getY();
    }

    @Override
    public void advance(float arg0) {
        advancingValue ^= 1;
    }

    @Override
    public void buttonPressed(Object arg0) {}

    @Override
    public void positionChanged(PositionAPI arg0) {
        this.x = arg0.getX();
        this.y = arg0.getY();
    }

    @Override
    public void processInput(List<InputEventAPI> arg0) {}

    @Override
    public void render(float alphaMult) {
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

    @Override
    public void renderBelow(float arg0) {}
    
    private int createGLTextureFromFrame(ByteBuffer frameBuffer, int width, int height) {
        if (frameBuffer == null) return -1;
        
        boolean isRGBA = FFmpeg.isImageRGBA(ptr);
        
        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        
        if (isRGBA) {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                              GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, frameBuffer);
        } else {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, width, height, 0,
                              GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, frameBuffer);
        }
        
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return textureId;
    }
}
