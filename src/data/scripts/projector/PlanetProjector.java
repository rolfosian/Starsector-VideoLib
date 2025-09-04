package data.scripts.projector;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;


import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.combat.entities.terrain.Planet;
import com.fs.starfarer.loading.specs.PlanetSpec;

import data.scripts.VideoPaths;
import data.scripts.VideoModes.EOFMode;
import data.scripts.VideoModes.PlayMode;

import data.scripts.buffers.TextureBuffer;
import data.scripts.decoder.Decoder;
import data.scripts.decoder.MuteDecoder;

import data.scripts.playerui.PlayerControlPanel;
import data.scripts.util.TexReflection;
import data.scripts.util.VideoUtils;

/**It is imperative to call this class's finish() method if the player leaves the system its planet is in or something else to stop the decoder, close the ffmpeg pipe and clean up. Or just leak memory and leave the decoder thread running forever; I'm not your boss*/
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

    public static final String PLANET_PROJECTOR_MEM_KEY = "$vlPlanetProjector";

    private boolean isDone = false;
    private boolean runWhilePaused = false;

    private String videoFilePath;

    private boolean paused = false;

    private PlayMode MODE;
    private EOFMode EOF_MODE;

    private final Decoder decoder;

    private final PlanetAPI campaignPlanet;
    private final Planet planet;
    private final Object planetTexTypeField;

    private PlanetSpec ourPlanetSpec;
    private String ourPlanetTexObjId;
    private Object ourPlanetTexObj;

    private final Object originalPlanetTexObj;
    private final int originalPlanetTexId;
    private final PlanetSpec originalPlanetSpec;

    private int currentTextureId;

    public PlanetProjector(PlanetAPI campaignPlanet, String videoId, int width, int height, Object planetTexTypeField) {
        PlanetProjector possibleProj = (PlanetProjector) campaignPlanet.getMemory().get(PLANET_PROJECTOR_MEM_KEY);
        if (possibleProj != null) possibleProj.finish();
        campaignPlanet.getMemory().set(PLANET_PROJECTOR_MEM_KEY, this);

        this.campaignPlanet = campaignPlanet;
        this.planetTexTypeField = planetTexTypeField;
        this.planet = TexReflection.getPlanetFromCampaignPlanet(campaignPlanet);

        this.originalPlanetSpec = (PlanetSpec) campaignPlanet.getSpec();
        this.originalPlanetTexObj = TexReflection.getPlanetTex(this.planet, this.planetTexTypeField);
        if (originalPlanetTexObj != null) this.originalPlanetTexId = (int) TexReflection.getPrivateVariable(TexReflection.texObjectIdField, originalPlanetTexObj);
        else this.originalPlanetTexId = 0;

        this.ourPlanetSpec = originalPlanetSpec.clone();
        this.ourPlanetTexObjId = VideoUtils.generateRandomId();
        this.ourPlanetTexObj = TexReflection.instantiateTexObj(GL11.GL_TEXTURE_2D, 0);

        TexReflection.texObjectMap.put(ourPlanetTexObjId, ourPlanetTexObj);
        TexReflection.setPlanetSpecTextureId(PlanetTexType.FIELD_MAP.get(planetTexTypeField), ourPlanetTexObjId,  ourPlanetSpec);

        TexReflection.setPlanetSpec(campaignPlanet, ourPlanetSpec);
        planet.setSpec(ourPlanetSpec);

        this.videoFilePath = VideoPaths.get(videoId);

        // if (planetTexTypeField == PlanetTexType.SHIELD) { // TODO: DETERMINE METHOD TO OVERLAY SHIELD TEXTURE PER VIDEO FRAME
            // this.originalPlanetTexId = (int) TexReflection.getPrivateVariable(TexReflection.texObjectIdField, planetTexObj);

            // this.MODE = PlayMode.PLAYING;
            // this.EOF_MODE = EOFMode.LOOP;
            // this.decoder = new MuteDecoder(this, new TextureBuffer(60), videoFilePath, width, height, this.MODE, this.EOF_MODE);
            // this.decoder.start();
    
            // currentTextureId = decoder.getCurrentVideoTextureId();
            // TexReflection.setTexObjId(planetTexObj, currentTextureId);
            // return;
        // }

        this.MODE = PlayMode.PLAYING;
        this.EOF_MODE = EOFMode.LOOP;
        this.decoder = new MuteDecoder(this, new TextureBuffer(60), videoFilePath, width, height, this.MODE, this.EOF_MODE);
        this.decoder.start();

        currentTextureId = decoder.getCurrentVideoTextureId();
        TexReflection.setTexObjId(ourPlanetTexObj, currentTextureId);
    }

    public PlanetProjector(Planet planet, String videoId, int width, int height, Object planetTexTypeField) {
        this.campaignPlanet = null;
        this.planetTexTypeField = planetTexTypeField;
        this.planet = TexReflection.getPlanetFromCampaignPlanet(campaignPlanet);

        this.originalPlanetSpec = (PlanetSpec) campaignPlanet.getSpec();
        this.originalPlanetTexObj = TexReflection.getPlanetTex(this.planet, this.planetTexTypeField);
        if (originalPlanetTexObj != null) this.originalPlanetTexId = (int) TexReflection.getPrivateVariable(TexReflection.texObjectIdField, originalPlanetTexObj);
        else this.originalPlanetTexId = 0;

        this.ourPlanetSpec = originalPlanetSpec.clone();
        this.ourPlanetTexObjId = VideoUtils.generateRandomId();
        this.ourPlanetTexObj = TexReflection.instantiateTexObj(GL11.GL_TEXTURE_2D, 0);

        TexReflection.texObjectMap.put(ourPlanetTexObjId, ourPlanetTexObj);
        TexReflection.setPlanetSpecTextureId(PlanetTexType.FIELD_MAP.get(planetTexTypeField), ourPlanetTexObjId,  ourPlanetSpec);

        TexReflection.setPlanetSpec(campaignPlanet, ourPlanetSpec);
        planet.setSpec(ourPlanetSpec);

        this.videoFilePath = VideoPaths.get(videoId);

        // if (planetTexTypeField == PlanetTexType.SHIELD) { // TODO: DETERMINE METHOD TO OVERLAY SHIELD TEXTURE PER VIDEO FRAME
            // this.originalPlanetTexId = (int) TexReflection.getPrivateVariable(TexReflection.texObjectIdField, planetTexObj);

            // this.MODE = PlayMode.PLAYING;
            // this.EOF_MODE = EOFMode.LOOP;
            // this.decoder = new MuteDecoder(this, new TextureBuffer(60), videoFilePath, width, height, this.MODE, this.EOF_MODE);
            // this.decoder.start();
    
            // currentTextureId = decoder.getCurrentVideoTextureId();
            // TexReflection.setTexObjId(planetTexObj, currentTextureId);
            // return;
        // }

        this.MODE = PlayMode.PLAYING;
        this.EOF_MODE = EOFMode.LOOP;
        this.decoder = new MuteDecoder(this, new TextureBuffer(60), videoFilePath, width, height, this.MODE, this.EOF_MODE);
        this.decoder.start();

        currentTextureId = decoder.getCurrentVideoTextureId();
        TexReflection.setTexObjId(ourPlanetTexObj, currentTextureId);
    }
    
    @Override
    public void advance(float deltaTime) {
        if (paused) return;

        int newId = decoder.getCurrentVideoTextureId(deltaTime);
        if (newId != currentTextureId) {
            TexReflection.setTexObjId(ourPlanetTexObj, newId);
            
            if (currentTextureId != 0) GL11.glDeleteTextures(currentTextureId);
            currentTextureId = newId;
        }
    }

    public void resetPlanetState() {
        if (originalPlanetTexObj == null) TexReflection.setPrivateVariable(planetTexTypeField, planet, null);
        else TexReflection.setTexObjId(this.originalPlanetTexObj, this.originalPlanetTexId);

        TexReflection.texObjectMap.remove(ourPlanetTexObjId);
        planet.setSpec(originalPlanetSpec);
        if (campaignPlanet != null) {
            campaignPlanet.getMemory().unset(PLANET_PROJECTOR_MEM_KEY);
            TexReflection.setPlanetSpec(campaignPlanet, originalPlanetSpec);
        }
    }

    @Override
    public void finish() {
        resetPlanetState();
        VideoUtils.removeId(ourPlanetTexObjId);

        if (currentTextureId != 0) {
            GL11.glDeleteTextures(currentTextureId);
            currentTextureId = 0;
        }

        decoder.finish();
        isDone = true;
        Global.getSector().removeTransientScript(this);
    }

    /** Pseudo enums for different planet texture fields - This is the last parameter for the PlanetProjector's constructor. They are populated in a static block in TexReflection*/
    public static class PlanetTexType {
        public static Object PLANET;
        public static Object CLOUD;
        public static Object SHIELD;
        public static Object SHIELD2;
        public static Object ATMOSPHERE;
        public static Object GLOW;

        // Maps from Planet fields to PlanetSpec fields
        public static final Map<Object, Object> FIELD_MAP = new HashMap<>();
    }

    public PlanetAPI getCampaignPlanet() {
        return this.campaignPlanet;
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    public void setRunWhilePaused(boolean runWhilePaused) {
        this.runWhilePaused = runWhilePaused;
    }

    @Override
    public boolean runWhilePaused() {
        return runWhilePaused;
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
}
