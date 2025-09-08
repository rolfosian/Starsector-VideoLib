package data.scripts.projector;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.fs.graphics.Sprite;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import data.scripts.VideoPaths;
import data.scripts.VideoModes.EOFMode;
import data.scripts.VideoModes.PlayMode;
import data.scripts.buffers.TextureBuffer;
import data.scripts.decoder.Decoder;
import data.scripts.decoder.MuteDecoder;
import data.scripts.playerui.PlayerControlPanel;
import data.scripts.speakers.Speakers;
import data.scripts.util.TexReflection;

public class SpriteProjector extends BaseEveryFrameCombatPlugin implements EveryFrameScript, Projector {
    private boolean isDone = false;
    private boolean runWhilePaused = false;

    private String videoId;
    private String videoFilePath;
    private int width;
    private int height;

    private boolean paused = false;
    private boolean isRendering = true;

    private PlayMode MODE;
    private EOFMode EOF_MODE;

    private final Decoder decoder;

    private int currentTextureId;

    private Sprite sprite;
    private Object originalTexObj;
    private String originalTexId;

    private Object ourTexObj;

    public SpriteProjector(Sprite sprite, String videoId, int width, int height, long startVideoUs) {
        this.videoFilePath = VideoPaths.getVideoPath(videoId);
        this.videoId = videoId;
        this.width = width;
        this.height = height;
        this.sprite = sprite;

        this.MODE = PlayMode.PLAYING;
        this.EOF_MODE = EOFMode.LOOP;
        this.decoder = new MuteDecoder(this, videoFilePath, width, height, MODE, EOF_MODE);
        this.decoder.start(startVideoUs);

        this.sprite = sprite;
        this.originalTexObj = TexReflection.getSpriteTexObj(sprite);
        this.originalTexId = TexReflection.getSpriteTexId(sprite);
        TexReflection.setSpriteTexId(sprite, null);

        this.ourTexObj = TexReflection.instantiateTexObj(GL11.GL_TEXTURE_2D, 0);
        TexReflection.setSpriteTexObj(sprite, ourTexObj);

        currentTextureId = decoder.getCurrentVideoTextureId();
        TexReflection.setTexObjId(ourTexObj, currentTextureId);
    }

    @Override
    public void advance(float deltaTime, List<InputEventAPI> arg1) {
        if (Global.getCurrentState() != GameState.COMBAT || Global.getCombatEngine().isPaused() || paused ) return;

        int newId = decoder.getCurrentVideoTextureId(deltaTime);
        if (newId != currentTextureId) {
            TexReflection.setTexObjId(ourTexObj, newId);
            
            if (currentTextureId != 0) GL11.glDeleteTextures(currentTextureId);
            currentTextureId = newId;
        }
    }

    @Override
    public void advance(float deltaTime) {
        if (paused) return;

        int newId = decoder.getCurrentVideoTextureId(deltaTime);
        if (newId != currentTextureId) {
            TexReflection.setTexObjId(ourTexObj, newId);
            
            if (currentTextureId != 0) GL11.glDeleteTextures(currentTextureId);
            currentTextureId = newId;
        }
    }

    @Override
    public void finish() {
        TexReflection.setSpriteTexObj(sprite, originalTexObj);
        TexReflection.setSpriteTexId(sprite, originalTexId);

        if (currentTextureId != 0) {
            GL11.glDeleteTextures(currentTextureId);
            currentTextureId = 0;
        }

        isDone = true;
        decoder.finish();
        Global.getSector().removeTransientScript(this);
        Global.getCombatEngine().removePlugin(this);
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public boolean runWhilePaused() {
        return runWhilePaused;
    }

    @Override
    public boolean isRendering() {
        return isRendering;
    }

    @Override
    public boolean paused() {
        return this.paused;
    }

    @Override
    public void play() {
        TexReflection.setSpriteTexObj(sprite, ourTexObj);
        paused = false;
    }

    @Override
    public void stop() {
        TexReflection.setSpriteTexObj(sprite, originalTexObj);
        paused = true;
    }

    @Override
    public void pause() {
        this.paused = true;
    }

    @Override
    public void unpause() {
        this.paused = false;
    }

    @Override
    public void setIsRendering(boolean isRendering) {
        this.isRendering = isRendering;
    }

    @Override
    public Decoder getDecoder() {
        return decoder;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public Speakers getSpeakers() {
        return null;
    }

    @Override
    public PlayerControlPanel getControlPanel() {
        return null;
    }

    @Override
    public void processInputPreCoreControls(float arg0, List<InputEventAPI> arg1) {}

    @Override
    public void renderInUICoords(ViewportAPI arg0) {}

    @Override
    public void renderInWorldCoords(ViewportAPI arg0) {}

    public Sprite getSprite() {
        return this.sprite;
    }

    public Object getOurTexObject() {
        return this.ourTexObj;
    }

    public Object getOriginalTexObject() {
        return this.originalTexObj;
    }
    
}
