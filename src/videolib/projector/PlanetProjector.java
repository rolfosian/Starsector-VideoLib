package videolib.projector;

import java.lang.invoke.VarHandle;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;


import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.combat.entities.terrain.Planet;
import com.fs.starfarer.loading.specs.PlanetSpec;

import videolib.VideoPaths;
import videolib.buffers.TexBuffer;
import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;

import videolib.decoder.Decoder;
import videolib.decoder.MuteDecoder;

import videolib.playerui.PlayerControlPanel;
import videolib.speakers.VideoProjectorSpeakers;
import videolib.util.TexReflection;
import videolib.util.VideoUtils;

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
    private TexBuffer textureBuffer;

    private final PlanetAPI campaignPlanet;
    private final Planet planet;
    private final VarHandle planetTexTypeVarHandle;

    private PlanetSpec ourPlanetSpec;
    private String ourPlanetTexObjId;
    private Object ourPlanetTexObj;

    private final Object originalPlanetTexObj;
    private final int originalPlanetTexId;
    private final PlanetSpec originalPlanetSpec;

    private int currentTextureId;

    /**
     * Projects a video onto a campaign-level {@link PlanetAPI}'s texture field by swapping in a dynamic texture.
     * Stores and restores original planet spec/texture on {@link #finish()}.
     *
     * @param campaignPlanet   campaign planet whose texture will be replaced
     * @param videoId          id of the video asset defined in settings.json
     * @param width            decoded video width in pixels
     * @param height           decoded video height in pixels
     * @param startVideoUs     initial start position in microseconds
     * @param planetTexTypeVarHandle one of {@link PlanetProjector.PlanetTexType} VarHandles indicating which texture to replace
     */
    public PlanetProjector(PlanetAPI campaignPlanet, String videoId, int width, int height, long startVideoUs, VarHandle planetTexTypeVarHandle) {
        PlanetProjector possibleProj = (PlanetProjector) campaignPlanet.getMemory().get(PLANET_PROJECTOR_MEM_KEY);
        if (possibleProj != null) possibleProj.finish();
        campaignPlanet.getMemory().set(PLANET_PROJECTOR_MEM_KEY, this);

        this.width = width;
        this.height = height;

        this.campaignPlanet = campaignPlanet;
        this.planetTexTypeVarHandle = planetTexTypeVarHandle;
        this.planet = TexReflection.getPlanetFromCampaignPlanet(campaignPlanet);

        this.originalPlanetSpec = (PlanetSpec) campaignPlanet.getSpec();
        this.originalPlanetTexObj = planetTexTypeVarHandle.get(planet);
        if (originalPlanetTexObj != null) this.originalPlanetTexId = TexReflection.getTexObjId(originalPlanetTexObj);
        else this.originalPlanetTexId = 0;

        this.ourPlanetSpec = originalPlanetSpec.clone();
        this.ourPlanetTexObjId = VideoUtils.generateRandomPlanetProjectorId(this);
        this.ourPlanetSpec.addTag(ourPlanetTexObjId);
        this.ourPlanetTexObj = TexReflection.instantiateTexObj(GL11.GL_TEXTURE_2D, 0);

        TexReflection.texObjectMap.put(ourPlanetTexObjId, ourPlanetTexObj);
        TexReflection.setPlanetSpecTextureId(PlanetTexType.VARHANDLE_MAP.get(planetTexTypeVarHandle), ourPlanetTexObjId,  ourPlanetSpec);

        TexReflection.setPlanetSpec(campaignPlanet, ourPlanetSpec);
        planet.setSpec(ourPlanetSpec);

        this.videoId = videoId;
        this.videoFilePath = VideoPaths.getVideoPath(videoId);

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
        this.decoder = new MuteDecoder(this, videoFilePath, width, height, this.MODE, this.EOF_MODE);
        this.decoder.start(startVideoUs);
        this.textureBuffer = decoder.getTextureBuffer();

        currentTextureId = decoder.getCurrentVideoTextureId();
        TexReflection.setTexObjId(ourPlanetTexObj, currentTextureId);
    }

    /**
     * Projects a video onto a runtime {@link Planet} instance's texture field by swapping in a dynamic texture.
     * Stores and restores original planet spec/texture on {@link #finish()}.
     *
     * @param planet           planet instance whose texture will be replaced
     * @param videoId          id of the video asset defined in settings.json
     * @param width            decoded video width in pixels
     * @param height           decoded video height in pixels
     * @param startVideoUs     initial start position in microseconds
     * @param planetTexTypeVarHandle one of {@link PlanetProjector.PlanetTexType} VarHandles indicating which texture to replace
     */
    public PlanetProjector(Planet planet, String videoId, int width, int height, long startVideoUs, VarHandle planetTexTypeVarHandle) {
        PlanetProjector possibleProj = getPossibleProjector(planet);
        if (possibleProj != null) {
            possibleProj.finish();
        }

        this.width = width;
        this.height = height;

        this.campaignPlanet = null;
        this.planetTexTypeVarHandle = planetTexTypeVarHandle;
        this.planet = planet;

        this.originalPlanetSpec = planet.getSpec();
        this.originalPlanetTexObj = this.planetTexTypeVarHandle.get(this.planet);
        if (originalPlanetTexObj != null) this.originalPlanetTexId = TexReflection.getTexObjId(originalPlanetTexObj);
        else this.originalPlanetTexId = 0;

        this.ourPlanetSpec = originalPlanetSpec.clone();
        this.ourPlanetTexObjId = VideoUtils.generateRandomPlanetProjectorId(this);
        this.ourPlanetSpec.addTag(ourPlanetTexObjId);
        this.ourPlanetTexObj = TexReflection.instantiateTexObj(GL11.GL_TEXTURE_2D, 0);

        TexReflection.texObjectMap.put(ourPlanetTexObjId, ourPlanetTexObj);
        TexReflection.setPlanetSpecTextureId(PlanetTexType.VARHANDLE_MAP.get(planetTexTypeVarHandle), ourPlanetTexObjId,  ourPlanetSpec);

        planet.setSpec(ourPlanetSpec);

        this.videoId = videoId;
        this.videoFilePath = VideoPaths.getVideoPath(videoId);

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
        this.decoder = new MuteDecoder(this, videoFilePath, width, height, this.MODE, this.EOF_MODE);
        this.decoder.start(startVideoUs);
        this.textureBuffer = decoder.getTextureBuffer();

        currentTextureId = decoder.getCurrentVideoTextureId();
        TexReflection.setTexObjId(ourPlanetTexObj, currentTextureId);
    }
    
    @Override
    public void advance(float deltaTime) {
        if (paused) return;

        int newId = decoder.getCurrentVideoTextureId(deltaTime);
        if (newId != currentTextureId) {
            TexReflection.setTexObjId(ourPlanetTexObj, newId);
            
            if (currentTextureId != 0) textureBuffer.deleteTexture(currentTextureId);
            currentTextureId = newId;
        }
    }

    public void resetPlanetState() {
        if (originalPlanetTexObj == null) planetTexTypeVarHandle.set(planet, null);
        else TexReflection.setTexObjId(this.originalPlanetTexObj, this.originalPlanetTexId);

        TexReflection.texObjectMap.remove(ourPlanetTexObjId);
        VideoUtils.removeId(ourPlanetTexObjId);

        Set<String> ourTags = ourPlanetSpec.getTags();
        ourTags.remove(ourPlanetTexObjId);

        Set<String> originalTags = originalPlanetSpec.getTags();
        originalTags.removeAll(originalTags);
        originalTags.addAll(ourTags);

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
        TexReflection.setPlanetSpecTextureId(PlanetTexType.VARHANDLE_MAP.get(planetTexTypeVarHandle), ourPlanetTexObjId,  ourPlanetSpec);

        if (campaignPlanet != null) {
            campaignPlanet.getMemory().set(PLANET_PROJECTOR_MEM_KEY, this);
            TexReflection.setPlanetSpec(campaignPlanet, ourPlanetSpec);
        }
        planet.setSpec(ourPlanetSpec);

        this.decoder.start(decoder.getCurrentVideoPts());
        this.textureBuffer = decoder.getTextureBuffer();

        currentTextureId = decoder.getCurrentVideoTextureId();
        TexReflection.setTexObjId(ourPlanetTexObj, currentTextureId);

        isDone = false;
        Global.getSector().addTransientScript(this);
    }

    @Override
    public void finish() {
        resetPlanetState();

        if (currentTextureId != 0) {
            textureBuffer.deleteTexture(currentTextureId);
            currentTextureId = 0;
        }

        decoder.finish();
        isDone = true;
        Global.getSector().removeTransientScript(this);
    }

    /** Pseudo enums for different planet texture fields - This is the last parameter for the PlanetProjector's constructor and what the projector replaces with video frames. They are populated in a static block in TexReflection*/
    public static class PlanetTexType {
        public static VarHandle PLANET;
        public static VarHandle CLOUD;
        public static VarHandle SHIELD;
        public static VarHandle SHIELD2;
        public static VarHandle ATMOSPHERE;
        public static VarHandle GLOW;

        // Maps from Planet fields to PlanetSpec fields
        public static final Map<VarHandle, VarHandle> VARHANDLE_MAP = new HashMap<>();
    }

    public Object getOurTexObject() {
        return this.ourPlanetTexObj;
    }

    public Object getOriginalTexObject() {
        return this.originalPlanetTexObj;
    }

    public PlanetSpec getOriginalPlanetSpec() {
        return this.originalPlanetSpec;
    }

    public PlanetSpec getOurPlanetSpec() {
        return this.ourPlanetSpec;
    }

    public VarHandle getTexType() {
        return this.planetTexTypeVarHandle;
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

    @Override
    public VideoProjectorSpeakers getSpeakers() {
        return null;
    }

    private PlanetProjector getPossibleProjector(Planet planet) {
        for (String tag : planet.getSpec().getTags()) {
            if (tag.startsWith("vl")) return VideoUtils.getPlanetProjector(tag);
        }
        return null;
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
