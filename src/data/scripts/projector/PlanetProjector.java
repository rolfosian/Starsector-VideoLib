package data.scripts.projector;

import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.combat.entities.terrain.Planet;

import data.scripts.VideoPaths;
import data.scripts.VideoModes.EOFMode;
import data.scripts.VideoModes.PlayMode;

import data.scripts.decoder.Decoder;
import data.scripts.decoder.MuteDecoder;

import data.scripts.playerui.PlayerControlPanel;
import data.scripts.util.TexReflection;

/**It is imperative to call this class's finish() method if the player leaves the system its planet is in or something to stop the decoder, close the ffmpeg pipe and clean up. Or just leak memory I'm not your boss*/
public class PlanetProjector implements EveryFrameScript, Projector {
    private static final Logger logger = Logger.getLogger(VideoProjector.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    private boolean isDone;

    private String videoFilePath;

    private boolean paused = false;

    private PlayMode MODE;
    private EOFMode EOF_MODE;

    private final Decoder decoder;

    private final Planet planet;
    private final Object planetTexTypeField;
    private final int originalPlanetTexId;
    private Object planetTexObj;
    private boolean resetToNull = false;

    private int currentTextureId;

    public PlanetProjector(PlanetAPI campaignPlanet, String videoId, int width, int height, Object planetTexTypeField) {
        this(TexReflection.getPlanetFromCampaignPlanet(campaignPlanet), videoId, width, height, planetTexTypeField);
    }

    public PlanetProjector(Planet planet, String videoId, int width, int height, Object planetTexTypeField) {
        this.videoFilePath = VideoPaths.get(videoId);

        this.planetTexTypeField = planetTexTypeField;
        this.planet = planet;
        this.planetTexObj = TexReflection.getPlanetTex(this.planet, this.planetTexTypeField);
        if (planetTexObj == null) {
            this.planetTexObj = TexReflection.instantiateTexObj(GL11.GL_TEXTURE_2D, 0);
            TexReflection.setPrivateVariable(planetTexTypeField, planet, planetTexObj);
            this.resetToNull = true;
        }
        this.originalPlanetTexId = (int) TexReflection.getPrivateVariable(TexReflection.texObjectIdField, planetTexObj);

        this.MODE = PlayMode.PLAYING;
        this.EOF_MODE = EOFMode.LOOP;
        this.decoder = new MuteDecoder(this, videoFilePath, width, height, this.MODE, this.EOF_MODE);
        this.decoder.start();

        // TexReflection.invalidateTokens(planet);
        currentTextureId = decoder.getCurrentVideoTextureId();
        TexReflection.setTexObjId(planetTexObj, currentTextureId);
    }
    
    @Override
    public void advance(float deltaTime) {
        if (paused) return;

        int newId = decoder.getCurrentVideoTextureId(deltaTime);
        if (newId != currentTextureId) {
            TexReflection.setTexObjId(planetTexObj, newId);
            
            if (currentTextureId != 0) GL11.glDeleteTextures(currentTextureId);
            currentTextureId = newId;
        }
    }

    @Override
    public void finish() {
        if (resetToNull) TexReflection.setPrivateVariable(planetTexTypeField, planet, null);
        else TexReflection.setTexObjId(this.planetTexObj, this.originalPlanetTexId);

        if (currentTextureId != 0) {
            GL11.glDeleteTextures(currentTextureId);
            currentTextureId = 0;
        }

        decoder.finish();
        isDone = true;
        Global.getSector().removeTransientScript(this);
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public boolean isRendering() {
        return true;
    }

    @Override
    public void play() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void pause() {
        this.paused = true;
    }

    public void unpause() {
        this.paused = false;
    }

    @Override
    public boolean paused() {
        return this.paused;
    }

    @Override
    public void setIsRendering(boolean isRendering) {}

    @Override
    public Decoder getDecoder() {
        return this.decoder;
    }

    @Override
    public PlayerControlPanel getControlPanel() {
        return null;
    }

    /** Pseudo enums for different planet texture fields - This is the last parameter for the PlanetProjector's constructor. They are populated in a static block in TexReflection*/
    public static class PlanetTexType {
        public static Object PLANET;
        public static Object CLOUD;
        public static Object SHIELD;
        public static Object SHIELD2;
        public static Object ATMOSPHERE;
        public static Object GLOW;
    }
}
