package data.scripts.projector;

import java.nio.FloatBuffer;
import java.util.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import data.scripts.VideoModes.EOFMode;
import data.scripts.VideoModes.PlayMode;
import data.scripts.VideoPaths;

import data.scripts.decoder.Decoder;
import data.scripts.decoder.MuteDecoder;

import data.scripts.playerui.PlayerControlPanel;
import data.scripts.buffers.TextureBuffer;

import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

public class MuteVideoProjector extends VideoProjector {
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
    private int width, height;

    private PlayMode MODE;
    private PlayMode OLD_MODE;
    private EOFMode EOF_MODE;

    private CustomPanelAPI panel;
    private MuteDecoder decoder;
    private PlayerControlPanel controlPanel = null;

    // private final int vboId;
    // private final FloatBuffer quadBuffer;

    // playback/texture state
    private int currentTextureId = 0;
    private boolean isRendering = false;
    private boolean paused = false;

    private float x = 0f;
    private float y = 0f;

    public int advancingValue = 0;
    private int checkAdvancing = 0;

    private boolean clickToPause = false;
    private float leftBound;
    private float rightBound;
    private float topBound;
    private float bottomBound;

    public MuteVideoProjector(String videoId, int width, int height, PlayMode startingPlayMode, EOFMode startingEOFMode) {
        this.videoFilePath = VideoPaths.get(videoId);
        this.MODE = startingPlayMode;
        this.EOF_MODE = startingEOFMode;

        this.width = width;
        this.height = height;

        this.decoder = new MuteDecoder(this, videoFilePath, width, height, startingPlayMode, startingEOFMode);
        this.decoder.start();

        // this.vboId = textureBuffer.getVboId();
        // this.quadBuffer = textureBuffer.getQuadBuffer();

        Global.getSector().addTransientScript(new EveryFrameScript() {
            private boolean isDone = false;

            @Override
            public void advance(float arg0) {
                checkAdvancing ^= 1;

                if (!(checkAdvancing == advancingValue)) { // as soon as this becomes misaligned with the value that is flipped in projector's advance then we finish and clean up
                    finish();
                    isDone = true;
                    Global.getSector().removeTransientScript(this);
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

    public void init(PositionAPI panelPos, CustomPanelAPI panel) {
        this.x = panelPos.getX();
        this.y = panelPos.getY();
        this.panel = panel;

        this.leftBound = panelPos.getCenterX() - panelPos.getWidth() / 2;
        this.rightBound = panelPos.getCenterX() + panelPos.getWidth() / 2;
        this.bottomBound = panelPos.getCenterY() + panelPos.getHeight() / 2;
        this.topBound = panelPos.getCenterY() - panelPos.getHeight() / 2 ;

        if (this.MODE == PlayMode.PLAYING) {
            this.isRendering = true;

        } else if (this.MODE == PlayMode.PAUSED) {
            this.isRendering = true;
            this.paused = true;
            this.currentTextureId = decoder.getCurrentVideoTextureId();
        }
    }

    private boolean isInBounds(float mouseX, float mouseY) {            
        return mouseX >= this.leftBound && mouseX <= this.rightBound &&
               mouseY >= this.topBound && mouseY <= this.bottomBound;
    }

    public void setClickToPause(boolean clickToPause) {
        this.clickToPause = clickToPause;
    }

    public PlayerControlPanel getControlPanel() {
        return this.controlPanel;
    }

    public void setControlPanel(PlayerControlPanel controlPanel) {
        this.controlPanel = controlPanel;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        for (int i = 0; i < events.size(); i++) {
            InputEventAPI event = events.get(i);

            if (!event.isConsumed() && event.isLMBDownEvent() && clickToPause && isInBounds(event.getX(), event.getY())) {
                if (controlPanel != null) {
                    if (paused) controlPanel.play();
                    else controlPanel.pause();
                    event.consume();
                    return;
                }
                
                if (paused) unpause(); else pause();
                event.consume();
            }
        }
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.x = position.getX();
        this.y = position.getY();
        this.width = (int) position.getWidth();
        this.height = (int) position.getHeight();

        this.leftBound = position.getCenterX() - this.width / 2;
        this.rightBound = position.getCenterX() + this.width / 2;
        this.bottomBound = position.getCenterY() + this.height / 2;
        this.topBound = position.getCenterY() - this.height / 2 ;
    }

    @Override
    public void advance(float amount) {
    	// advance is called by game once per frame
        advancingValue ^= 1;
        if (paused) {
            if (MODE == PlayMode.SEEKING) {
                currentTextureId = decoder.getCurrentVideoTextureId();
                MODE = OLD_MODE;
            }
            return;
        } 
        currentTextureId = decoder.getCurrentVideoTextureId(amount);
    }

    @Override
    public void render(float alphaMult) {
        if (!isRendering) return;

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTextureId);
    
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

        // GL11.glEnable(GL11.GL_TEXTURE_2D);
        // GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTextureId);
    
        // GL11.glEnable(GL11.GL_BLEND);
        // GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        // GL11.glColor4f(1f, 1f, 1f, alphaMult);
    
        // quadBuffer.clear();
        // quadBuffer.put(new float[] {
        //     x, y + height, 0f, 0f,         // top-left
        //     x + width, y + height, 1f, 0f, // top-right
        //     x + width, y, 1f, 1f,          // bottom-right
        //     x, y, 0f, 1f                   // bottom-left
        // });
        // quadBuffer.flip();
    
        // GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        // GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, quadBuffer);
    
        // GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        // GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
    
        // GL11.glVertexPointer(2, GL11.GL_FLOAT, 4 * Float.BYTES, 0);
        // GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 4 * Float.BYTES, 2 * Float.BYTES);
    
        // GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);
    
        // GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        // GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
    
        // GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        // GL11.glColor4f(1f, 1f, 1f, 1f);
        // GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        // GL11.glDisable(GL11.GL_BLEND);
        // GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    public void pause() {
        paused = true;
        this.MODE = PlayMode.PAUSED;
    }

    public void unpause() {
        paused = false;
    }

    public void play() {
        paused = false;

        this.MODE = PlayMode.PLAYING;
        decoder.setEOFMode(this.EOF_MODE);
        isRendering = true;
    }

    public void stop() {
        isRendering = false;
        paused = true;

        if (currentTextureId != 0) GL11.glDeleteTextures(currentTextureId);
        decoder.stop();
        currentTextureId = decoder.getCurrentVideoTextureId();
        isRendering = false;
    }

    public void restart() {
        if (currentTextureId != 0) {
            GL11.glDeleteTextures(currentTextureId);
            currentTextureId = 0;
        }
    }

    public void finish() {
        isRendering = false;

        if (currentTextureId != 0) {
            GL11.glDeleteTextures(currentTextureId);
            currentTextureId = 0;
        }

        decoder.finish();
	}

    public Decoder getDecoder() {
        return this.decoder;
    }

    public boolean paused() {
        return this.paused;
    }

    public void setCurrentTextureId(int id) {
        if (id == currentTextureId) return;
        this.currentTextureId = id;
    }

    public PlayMode getPlayMode() {
        return this.MODE;
    }

    public void setPlayMode(PlayMode mode) {
        this.OLD_MODE = this.MODE;
        this.MODE = mode;
    }

    public EOFMode getEOFMode() {
        return this.EOF_MODE;
    }

    public void setEOFMode(EOFMode mode) {
        this.EOF_MODE = mode;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
    
    public void setVideoFilePath(String path) {
        this.videoFilePath = path;
    }

    public void setIsRendering(boolean isRendering) {
        this.isRendering = isRendering;
    }

    public boolean isRendering() {
        return this.isRendering;
    }
}