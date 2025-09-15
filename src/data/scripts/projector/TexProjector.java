package data.scripts.projector;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import data.scripts.VideoPaths;
import data.scripts.VideoModes.EOFMode;
import data.scripts.VideoModes.PlayMode;
import data.scripts.decoder.Decoder;
import data.scripts.decoder.MuteDecoder;
import data.scripts.playerui.PlayerControlPanel;
import data.scripts.speakers.Speakers;
import data.scripts.util.TexReflection;
import data.scripts.util.VideoUtils;

public class TexProjector extends BaseEveryFrameCombatPlugin implements EveryFrameScript, Projector {
    private boolean isDone = false;
    private boolean runWhilePaused = true;

    private String videoFilePath;
    private int width;
    private int height;

    private boolean paused = false;
    private boolean isRendering = true;

    private PlayMode MODE;
    private EOFMode EOF_MODE;

    private final Decoder decoder;

    private int currentTextureId;
    private int originalTextureId;

    private Object textureWrapper;

    /**
     * Projects a video onto an arbitrary texture wrapper referenced by id in {@link TexReflection#texObjectMap}.
     * The original texture id is restored on {@link #finish()}.
     *
     * @param textureWrapperId id in {@code TexReflection.texObjectMap} whose texture object will be updated. (usually the relative path to the image eg data/graphics/illustrations/image.png)
     * @param videoId          id of the video asset defined in settings.json
     * @param width            decoded video width in pixels
     * @param height           decoded video height in pixels
     * @param startVideoUs     initial start position in microseconds
     */
    public TexProjector(String textureWrapperId, String videoId, int width, int height, long startVideoUs) {
        this.videoFilePath = VideoPaths.getVideoPath(videoId);
        this.width = width;
        this.height = height;

        this.MODE = PlayMode.PLAYING;
        this.EOF_MODE = EOFMode.LOOP;
        this.decoder = new MuteDecoder(this, videoFilePath, width, height, MODE, EOF_MODE);
        this.decoder.start(startVideoUs);

        this.textureWrapper = TexReflection.texObjectMap.get(textureWrapperId);
        this.originalTextureId = TexReflection.getTexObjId(textureWrapper);

        currentTextureId = decoder.getCurrentVideoTextureId();
        TexReflection.setTexObjId(textureWrapper, currentTextureId);

        VideoUtils.getRingBandAndSpriteProjectors().add(this);
    }

    @Override
    public void advance(float deltaTime, List<InputEventAPI> arg1) {
        if (Global.getCurrentState() != GameState.COMBAT || Global.getCombatEngine().isPaused() || paused ) return;

        int newId = decoder.getCurrentVideoTextureId(deltaTime);
        if (newId != currentTextureId) {
            TexReflection.setTexObjId(textureWrapper, newId);
            
            if (currentTextureId != 0) GL11.glDeleteTextures(currentTextureId);
            currentTextureId = newId;
        }
    }

    @Override
    public void advance(float deltaTime) {
        if (paused) return;

        int newId = decoder.getCurrentVideoTextureId(deltaTime);
        if (newId != currentTextureId) {
            TexReflection.setTexObjId(textureWrapper, newId);
            
            if (currentTextureId != 0) GL11.glDeleteTextures(currentTextureId);
            currentTextureId = newId;
        }
    }

    @Override
    public void restart() {
        this.decoder.start(decoder.getCurrentVideoPts());
        currentTextureId = decoder.getCurrentVideoTextureId();
        TexReflection.setTexObjId(textureWrapper, currentTextureId);

        Global.getSector().addTransientScript(this);
        VideoUtils.getRingBandAndSpriteProjectors().add(this);
    }

    @Override
    public void finish() {
        TexReflection.setTexObjId(textureWrapper, originalTextureId);

        if (currentTextureId != 0) {
            GL11.glDeleteTextures(currentTextureId);
            currentTextureId = 0;
        }

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
        TexReflection.setTexObjId(textureWrapper, currentTextureId);
        paused = false;
    }

    @Override
    public void stop() {
        TexReflection.setTexObjId(textureWrapper, originalTextureId);
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

    public Object getTextureWrapper() {
        return this.textureWrapper;
    }

    public int getOriginalTextureId() {
        return this.originalTextureId;
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
