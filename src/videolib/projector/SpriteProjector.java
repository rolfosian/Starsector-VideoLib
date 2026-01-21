package videolib.projector;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.fs.graphics.Sprite;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import videolib.VideoPaths;
import videolib.buffers.TexBuffer;
import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;
import videolib.decoder.Decoder;
import videolib.decoder.MuteDecoder;
import videolib.playerui.PlayerControlPanel;
import videolib.speakers.Speakers;
import videolib.util.TexReflection;
import videolib.util.VideoUtils;

public class SpriteProjector extends BaseEveryFrameCombatPlugin implements EveryFrameScript, Projector {
    private boolean isDone = false;
    private boolean runWhilePaused = false;

    // videoId not used; path is resolved immediately
    // private String videoId;
    private String videoFilePath;
    private int width;
    private int height;

    private boolean paused = false;
    private boolean isRendering = true;

    private PlayMode MODE;
    private EOFMode EOF_MODE;

    private final Decoder decoder;
    private TexBuffer textureBuffer;
    private int currentTextureId;

    private Sprite sprite;
    private Object originalTexObj;
    private String originalTexId;

    private Object ourTexObj;

    /**
     * Projects a video onto a {@link Sprite}'s texture by swapping in a dynamic texture object.
     * Restores the original texture on {@link #finish()}.
     *
     * @param sprite      sprite to project onto
     * @param videoId     id of the video asset defined in settings.json
     * @param width       decoded video width in pixels
     * @param height      decoded video height in pixels
     * @param startVideoUs initial start position in microseconds
     */
    public SpriteProjector(Sprite sprite, String videoId, int width, int height, long startVideoUs) {
        this.videoFilePath = VideoPaths.getVideoPath(videoId);
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

        VideoUtils.getRingBandAndSpriteProjectors().add(this);
    }

    @Override
    public void advance(float deltaTime, List<InputEventAPI> arg1) {
        if (Global.getCurrentState() != GameState.COMBAT || Global.getCombatEngine().isPaused() || paused ) return;

        int newId = decoder.getCurrentVideoTextureId(deltaTime);
        if (newId != currentTextureId) {
            TexReflection.setTexObjId(ourTexObj, newId);
            
            currentTextureId = newId;
        }
    }

    @Override
    public void advance(float deltaTime) {
        if (paused) return;

        int newId = decoder.getCurrentVideoTextureId(deltaTime);
        if (newId != currentTextureId) {
            TexReflection.setTexObjId(ourTexObj, newId);
            
            currentTextureId = newId;
        }
    }

    @Override
    public void restart() {
        TexReflection.setSpriteTexObj(sprite, ourTexObj);

        this.decoder.start(decoder.getCurrentVideoPts());
        this.textureBuffer = decoder.getTextureBuffer();
        
        currentTextureId = decoder.getCurrentVideoTextureId();
        TexReflection.setTexObjId(ourTexObj, currentTextureId);

        Global.getSector().addTransientScript(this);
        VideoUtils.getRingBandAndSpriteProjectors().add(this);
    }

    @Override
    public void finish() {
        TexReflection.setSpriteTexObj(sprite, originalTexObj);
        TexReflection.setSpriteTexId(sprite, originalTexId);

        isDone = true;
        decoder.finish();
        Global.getSector().removeTransientScript(this);
        Global.getCombatEngine().removePlugin(this);
        VideoUtils.getRingBandAndSpriteProjectors().remove(this);
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

    @Override
    public PlayMode getPlayMode() {
        return this.MODE;
    }

    @Override
    public EOFMode getEOFMode() {
        return this.EOF_MODE;
    }

    @Override
    public void setPlayMode(PlayMode mode) {}

    @Override
    public void setEOFMode(EOFMode mode) {}
    
}
