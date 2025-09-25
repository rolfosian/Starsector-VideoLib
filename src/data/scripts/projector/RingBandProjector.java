package data.scripts.projector;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.RingBandAPI;
import com.fs.starfarer.campaign.RingBand;

import data.scripts.VideoModes.EOFMode;
import data.scripts.VideoModes.PlayMode;

import data.scripts.VideoPaths;
import data.scripts.decoder.Decoder;
import data.scripts.decoder.MuteDecoder;

import data.scripts.playerui.PlayerControlPanel;
import data.scripts.speakers.Speakers;

import data.scripts.util.TexReflection;
import data.scripts.util.VideoUtils;

public class RingBandProjector implements EveryFrameScript, Projector {
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

    private int currentTextureId;

    private RingBand ringBand;
    private Object originalTexObj;
    // originalTexId not used since we restore by object
    // private String originalTexId;

    private Object ourTexObj;

    /**
     * Projects a video onto a {@link RingBand}'s texture by swapping in a dynamic texture object.
     * Restores the original texture on {@link #finish()}.
     *
     * @param ringBand   ring band to project onto
     * @param videoId    id of the video asset defined in settings.json
     * @param width      decoded video width in pixels
     * @param height     decoded video height in pixels
     * @param startVideoUs initial start position in microseconds
     */
    public RingBandProjector(RingBand ringBand, String videoId, int width, int height, long startVideoUs) {
        this.videoFilePath = VideoPaths.getVideoPath(videoId);
        this.width = width;
        this.height = height;
        this.ringBand = ringBand;

        this.MODE = PlayMode.PLAYING;
        this.EOF_MODE = EOFMode.LOOP;
        this.decoder = new MuteDecoder(this, videoFilePath, width, height, MODE, EOF_MODE);
        this.decoder.start(startVideoUs);

        this.originalTexObj = TexReflection.getRingBandTexObj(ringBand);

        this.ourTexObj = TexReflection.instantiateTexObj(GL11.GL_TEXTURE_2D, 0);
        TexReflection.setRingBandTexObj(ringBand, ourTexObj);

        currentTextureId = decoder.getCurrentVideoTextureId();
        TexReflection.setTexObjId(ourTexObj, currentTextureId);

        VideoUtils.getRingBandAndSpriteProjectors().add(this);
    }

    /**
     * Projects a video onto a {@link RingBand}'s texture by swapping in a dynamic texture object.
     * Restores the original texture on {@link #finish()}.
     *
     * @param ringBand   ring band to project onto
     * @param videoId    id of the video asset defined in settings.json
     * @param width      decoded video width in pixels
     * @param height     decoded video height in pixels
     * @param startVideoUs initial start position in microseconds
     */
    public RingBandProjector(RingBandAPI ringBand, String videoId, int width, int height, long startVideoUs) {
        this.videoFilePath = VideoPaths.getVideoPath(videoId);
        this.width = width;
        this.height = height;
        this.ringBand = (RingBand) ringBand;

        this.MODE = PlayMode.PLAYING;
        this.EOF_MODE = EOFMode.LOOP;
        this.decoder = new MuteDecoder(this, videoFilePath, width, height, MODE, EOF_MODE);
        this.decoder.start(startVideoUs);

        this.originalTexObj = TexReflection.getRingBandTexObj(this.ringBand);

        this.ourTexObj = TexReflection.instantiateTexObj(GL11.GL_TEXTURE_2D, 0);
        TexReflection.setRingBandTexObj(this.ringBand, ourTexObj);

        currentTextureId = decoder.getCurrentVideoTextureId();
        TexReflection.setTexObjId(ourTexObj, currentTextureId);

        VideoUtils.getRingBandAndSpriteProjectors().add(this);
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
    public void restart() {
        TexReflection.setRingBandTexObj(ringBand, ourTexObj);

        this.decoder.start(decoder.getCurrentVideoPts());
        currentTextureId = decoder.getCurrentVideoTextureId();
        TexReflection.setTexObjId(ourTexObj, currentTextureId);

        Global.getSector().addTransientScript(this);
        VideoUtils.getRingBandAndSpriteProjectors().add(this);
    }

    @Override
    public void finish() {
        TexReflection.setRingBandTexObj(ringBand, originalTexObj);
        // TexReflection.setRingBandTexId(ringBand, originalTexId);

        if (currentTextureId != 0) {
            GL11.glDeleteTextures(currentTextureId);
            currentTextureId = 0;
        }

        isDone = true;
        decoder.finish();
        Global.getSector().removeTransientScript(this);
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
        TexReflection.setRingBandTexObj(ringBand, ourTexObj);
        paused = false;
    }

    @Override
    public void stop() {
        TexReflection.setRingBandTexObj(ringBand, originalTexObj);
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

    public RingBand getRingBand() {
        return this.ringBand;
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
