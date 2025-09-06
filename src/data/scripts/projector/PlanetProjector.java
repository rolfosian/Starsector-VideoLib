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
import data.scripts.speakers.VideoProjectorSpeakers;
import data.scripts.util.TexReflection;
import data.scripts.util.VideoUtils;

/**It is imperative to call this class's finish() method if the player leaves the system its planet is in or something else to stop the decoder, close the ffmpeg pipe and reset the planet fields. Or just leak memory and leave the decoder thread running forever; I'm not your boss*/
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

    private String videoId;
    private String videoFilePath;
    private int width;
    private int height;

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

    public PlanetProjector(PlanetAPI campaignPlanet, String videoId, int width, int height, long startVideoUs, Object planetTexTypeField) {
        PlanetProjector possibleProj = (PlanetProjector) campaignPlanet.getMemory().get(PLANET_PROJECTOR_MEM_KEY);
        if (possibleProj != null) possibleProj.finish();
        campaignPlanet.getMemory().set(PLANET_PROJECTOR_MEM_KEY, this);

        this.width = width;
        this.height = height;

        this.campaignPlanet = campaignPlanet;
        this.planetTexTypeField = planetTexTypeField;
        this.planet = TexReflection.getPlanetFromCampaignPlanet(campaignPlanet);

        this.originalPlanetSpec = (PlanetSpec) campaignPlanet.getSpec();
        this.originalPlanetTexObj = TexReflection.getPlanetTex(this.planet, this.planetTexTypeField);
        if (originalPlanetTexObj != null) this.originalPlanetTexId = (int) TexReflection.getPrivateVariable(TexReflection.texObjectIdField, originalPlanetTexObj);
        else this.originalPlanetTexId = 0;

        this.ourPlanetSpec = originalPlanetSpec.clone();
        this.ourPlanetTexObjId = VideoUtils.generateRandomPlanetProjectorId(this);
        this.ourPlanetSpec.addTag(ourPlanetTexObjId);
        this.ourPlanetTexObj = TexReflection.instantiateTexObj(GL11.GL_TEXTURE_2D, 0);

        TexReflection.texObjectMap.put(ourPlanetTexObjId, ourPlanetTexObj);
        TexReflection.setPlanetSpecTextureId(PlanetTexType.FIELD_MAP.get(planetTexTypeField), ourPlanetTexObjId,  ourPlanetSpec);

        TexReflection.setPlanetSpec(campaignPlanet, ourPlanetSpec);
        planet.setSpec(ourPlanetSpec);

        this.videoId = videoId;
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
        this.decoder.start(startVideoUs);

        currentTextureId = decoder.getCurrentVideoTextureId();
        TexReflection.setTexObjId(ourPlanetTexObj, currentTextureId);
    }

    public PlanetProjector(Planet planet, String videoId, int width, int height, long startVideoUs, Object planetTexTypeField) {
        PlanetProjector possibleProj = getPossibleProjector(planet);
        if (possibleProj != null) {
            possibleProj.finish();
        }

        this.width = width;
        this.height = height;

        this.campaignPlanet = null;
        this.planetTexTypeField = planetTexTypeField;
        this.planet = planet;

        this.originalPlanetSpec = planet.getSpec();
        this.originalPlanetTexObj = TexReflection.getPlanetTex(this.planet, this.planetTexTypeField);
        if (originalPlanetTexObj != null) this.originalPlanetTexId = (int) TexReflection.getPrivateVariable(TexReflection.texObjectIdField, originalPlanetTexObj);
        else this.originalPlanetTexId = 0;

        this.ourPlanetSpec = originalPlanetSpec.clone();
        this.ourPlanetTexObjId = VideoUtils.generateRandomPlanetProjectorId(this);
        this.ourPlanetSpec.addTag(ourPlanetTexObjId);
        this.ourPlanetTexObj = TexReflection.instantiateTexObj(GL11.GL_TEXTURE_2D, 0);

        TexReflection.texObjectMap.put(ourPlanetTexObjId, ourPlanetTexObj);
        TexReflection.setPlanetSpecTextureId(PlanetTexType.FIELD_MAP.get(planetTexTypeField), ourPlanetTexObjId,  ourPlanetSpec);

        planet.setSpec(ourPlanetSpec);

        this.videoId = videoId;
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
        this.decoder.start(startVideoUs);

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
        VideoUtils.removeId(ourPlanetTexObjId);

        ourPlanetSpec.getTags().remove(ourPlanetTexObjId);
        originalPlanetSpec.getTags().addAll(ourPlanetSpec.getTags()); // in case a tag was added while this projector was active. this is a set so we don't need to worry about duplication

        planet.setSpec(originalPlanetSpec);
        if (campaignPlanet != null) {
            campaignPlanet.getMemory().unset(PLANET_PROJECTOR_MEM_KEY);
            TexReflection.setPlanetSpec(campaignPlanet, originalPlanetSpec);
        }

        TexReflection.invalidateTokens(planet);
    }

    public void restart() {
        TexReflection.invalidateTokens(planet);

        this.ourPlanetTexObjId = VideoUtils.generateRandomPlanetProjectorId(this);
        this.ourPlanetSpec.addTag(ourPlanetTexObjId);

        TexReflection.texObjectMap.put(ourPlanetTexObjId, ourPlanetTexObj);
        TexReflection.setPlanetSpecTextureId(PlanetTexType.FIELD_MAP.get(planetTexTypeField), ourPlanetTexObjId,  ourPlanetSpec);

        if (campaignPlanet != null) {
            campaignPlanet.getMemory().set(PLANET_PROJECTOR_MEM_KEY, this);
            TexReflection.setPlanetSpec(campaignPlanet, ourPlanetSpec);
        }
        planet.setSpec(ourPlanetSpec);

        this.decoder.start(decoder.getCurrentVideoPts());

        currentTextureId = decoder.getCurrentVideoTextureId();
        TexReflection.setTexObjId(ourPlanetTexObj, currentTextureId);

        isDone = false;
        Global.getSector().addTransientScript(this);
    }

    @Override
    public void finish() {
        resetPlanetState();

        if (currentTextureId != 0) {
            GL11.glDeleteTextures(currentTextureId);
            currentTextureId = 0;
        }

        decoder.finish();
        isDone = true;
        Global.getSector().removeTransientScript(this);
    }

    /** Pseudo enums for different planet texture fields - This is the last parameter for the PlanetProjector's constructor and what the projector replaces with video frames. They are populated in a static block in TexReflection*/
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

    public Object getTexType() {
        return this.planetTexTypeField;
    }

    public String getVideoId() {
        return this.videoId;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public Planet getPlanet() {
        return this.planet;
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

    public VideoProjectorSpeakers getSpeakers() {
        return null;
    }

    private PlanetProjector getPossibleProjector(Planet planet) {
        for (String tag : planet.getSpec().getTags()) {
            if (tag.startsWith("vl")) return VideoUtils.getPlanetProjector(tag);
        }
        return null;
    }
}
