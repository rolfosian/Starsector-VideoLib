package data.scripts.projector;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.campaign.RingBand;

import data.scripts.VideoModes.EOFMode;
import data.scripts.VideoModes.PlayMode;

import data.scripts.VideoPaths;
import data.scripts.buffers.TextureBuffer;
import data.scripts.decoder.Decoder;
import data.scripts.decoder.MuteDecoder;

import data.scripts.playerui.PlayerControlPanel;
import data.scripts.speakers.Speakers;

import data.scripts.util.TexReflection;

public class RingBandProjector implements EveryFrameScript, Projector {
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

    private RingBand ringBand;
    private Object originalTexObj;
    private String originalTexId;

    private Object ourTexObj;

    public RingBandProjector(RingBand ringBand, String videoId, int width, int height, long startVideoUs) {
        this.videoFilePath = VideoPaths.get(videoId);
        this.videoId = videoId;
        this.width = width;
        this.height = height;
        this.ringBand = ringBand;

        this.MODE = PlayMode.PLAYING;
        this.EOF_MODE = EOFMode.LOOP;
        this.decoder = new MuteDecoder(this, videoFilePath, width, height, MODE, EOF_MODE);
        this.decoder.start(startVideoUs);

        this.ringBand = ringBand;
        this.originalTexObj = TexReflection.getRingBandTexObj(ringBand);
        // this.originalTexId = TexReflection.getRingBandTexId(ringBand);
        // TexReflection.setRingBandTexId(ringBand, null);

        this.ourTexObj = TexReflection.instantiateTexObj(GL11.GL_TEXTURE_2D, 0);
        TexReflection.setRingBandTexObj(ringBand, ourTexObj);

        currentTextureId = decoder.getCurrentVideoTextureId();
        TexReflection.setTexObjId(ourTexObj, currentTextureId);
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
        TexReflection.setRingBandTexObj(ringBand, originalTexObj);
        // TexReflection.setRingBandTexId(ringBand, originalTexId);

        if (currentTextureId != 0) {
            GL11.glDeleteTextures(currentTextureId);
            currentTextureId = 0;
        }

        isDone = true;
        decoder.finish();
        Global.getSector().removeTransientScript(this);
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
    
}
