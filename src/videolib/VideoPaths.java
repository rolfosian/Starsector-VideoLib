package videolib;

import java.util.*;

import org.json.JSONObject;
import org.lwjgl.opengl.GL11;
import org.json.JSONException;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.ModSpecAPI;

import videolib.decoder.grouped.MuteDecoderGroup;
import videolib.ffmpeg.FFmpeg;
import videolib.util.TexReflection;

import videolib.projector.AutoTexProjector;
import videolib.projector.AutoTexProjector.AutoTexProjectorAPI;

@SuppressWarnings("unchecked")
public class VideoPaths {
    public static final Logger logger = Global.getLogger(VideoPaths.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }


    private static int DECODERS_PER_GROUP;

    public static boolean populated = false;

    private static Map<String, String> videoMap = new HashMap<>();
    private static Map<String, String> reverseVideoMap = new HashMap<>();
    private static String[] videoKeys;

    private static Map<String, String> imageMap = new HashMap<>();
    private static String[] imageKeys;

    private static Map<String, AutoTexProjectorAPI> autoTexMap = new HashMap<>();
    private static List<AutoTexProjectorAPI> autoTexWithCampaignSpeedup = new ArrayList<>();

    private static List<AutoTexProjectorAPI> allAutoTexOverrides = new ArrayList<>();
    private static List<AutoTexProjectorAPI> runWhilePausedCombatAutoTexOverrides = new ArrayList<>();

    private static List<MuteDecoderGroup> muteDecoderGroups = new ArrayList<>();

    protected static void populate() {
        if (populated) return;
        List<String> videoKeyz = new ArrayList<>();
        List<String> imageKeyz = new ArrayList<>();

        MuteDecoderGroup currMuteDecoderGroup = new MuteDecoderGroup();
        muteDecoderGroups.add(currMuteDecoderGroup);

        try {
            ModManagerAPI modManager = Global.getSettings().getModManager();
            JSONObject settings = Global.getSettings().getJSONObject("VideoLib");
            DECODERS_PER_GROUP = settings.getInt("decodersPerGroup");
            settings.remove("decodersPerGroup");
            
            Iterator<String> modIds = settings.keys();
            while (modIds.hasNext()) {
                String modId = modIds.next();
                ModSpecAPI modSpec = modManager.getModSpec(modId);

                if (modSpec == null)  {
                    logger.warn("ModSpecAPI.getModSpec returned null for modId " + modId + " in VideoLib settings, ignoring");
                    continue;
                }

                String modPath = modSpec.getPath();
                JSONObject pathData = settings.getJSONObject(modId);

                JSONObject videoFilePaths = getJSONObject(modId, pathData, "videos");
                if (videoFilePaths != null) {
                    Iterator<String> fileIds = videoFilePaths.keys();

                    while (fileIds.hasNext()) {
                        String fileId = fileIds.next();
    
                        if (videoMap.containsKey(fileId)) {
                            throw new IllegalArgumentException("Duplicate video file ID " + fileId + " for mod id " + modId + " already located at " + videoMap.get(fileId));
                        }
    
                        String relativePath = videoFilePaths.getString(fileId);
                        String absolutePath = modPath + "/" + relativePath;
                        if (!FFmpeg.fileExists(absolutePath)) throw new IllegalArgumentException("404 file not found: " + absolutePath);
    
                        videoMap.put(fileId, absolutePath);
                        reverseVideoMap.put(absolutePath, fileId);
                        videoKeyz.add(fileId);
    
                        logger.info("Resolved absolute path for video file id " + fileId + " at " + modPath + "/" + relativePath);
                    }
                }

                JSONObject imageFilePaths = getJSONObject(modId, pathData, "images");
                if (imageFilePaths != null) {
                    Iterator<String> fileIds = imageFilePaths.keys();

                    while (fileIds.hasNext()) {
                        String fileId = fileIds.next();
    
                        if (imageMap.containsKey(fileId)) {
                            throw new IllegalArgumentException("Duplicate image file ID " + fileId + " for mod id " + modId + " already located at " + imageMap.get(fileId));
                        }
    
                        String relativePath = imageFilePaths.getString(fileId);
                        String absolutePath = modPath + "/" + relativePath;
                        if (!FFmpeg.fileExists(absolutePath)) throw new IllegalArgumentException("404 file not found: " + absolutePath);
    
                        imageMap.put(fileId, absolutePath);
                        imageKeyz.add(fileId);
    
                        logger.info("Resolved absolute path for image file id " + fileId + " at " + modPath + "/" + relativePath);
                    }
                }
                
                JSONObject textureOverrides = getJSONObject(modId, pathData, "autoTexProjectorOverrides");
                if (textureOverrides != null) {
                    Iterator<String> texturePaths = textureOverrides.keys();

                    while (texturePaths.hasNext()) {
                        String texturePath = texturePaths.next();
                        
                        JSONObject overrideData = textureOverrides.getJSONObject(texturePath);    
                        String videoId = overrideData.getString("videoId");

                        if (!TexReflection.texObjectMap.containsKey(texturePath)) {
                            throw new IllegalArgumentException(texturePath + " not found in Starsector texture repository for video override: " + videoId);
                        }
                        Object original = TexReflection.texObjectMap.get(texturePath);

                        boolean campaignRunWhilePaused = isRunWhilePaused(overrideData, "campaign");
                        boolean combatRunWhilePaused = isRunWhilePaused(overrideData, "combat");
                        boolean isOverWriteInVram = isOverWriteInVram(overrideData);
                        boolean useCampaignSpeedup = useCampaignSpeedup(overrideData);

                        int width;
                        int height;

                        int texId;

                        if (isOverWriteInVram) {
                            texId = TexReflection.getTexObjId(original);

                            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
                            width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
                            height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
                            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

                        } else {
                            texId = 0;
                            width = getWidth(overrideData, original);
                            height = getHeight(overrideData, original);
                        }

                        if (autoTexMap.containsKey(texturePath)) {
                            throw new IllegalArgumentException("video override already found for texture " + texturePath);
                        }
                        
                        if (currMuteDecoderGroup.size() >= DECODERS_PER_GROUP) {
                            currMuteDecoderGroup = new MuteDecoderGroup();
                            muteDecoderGroups.add(currMuteDecoderGroup);
                        }

                        AutoTexProjectorAPI ours = AutoTexProjector.instantiator.instantiate(
                            currMuteDecoderGroup,
                            videoId,
                            width,
                            height,
                            texId,
                            campaignRunWhilePaused,
                            combatRunWhilePaused,
                            isOverWriteInVram,
                            useCampaignSpeedup
                        );
                        if (useCampaignSpeedup) autoTexWithCampaignSpeedup.add(ours);

                        ours.setOriginalTexture(texturePath, original);
                        TexReflection.transplantTexFields(original, ours);

                        if (!isOverWriteInVram) TexReflection.setTexObjId(ours, ours.getCurrentTextureId());

                        TexReflection.texObjectMap.put(texturePath, ours);
                        autoTexMap.put(texturePath, ours);

                        logger.info("Instantiated transient video override for texture " + texturePath + " using videoId " + videoId + " located at " + videoMap.get(videoId));
                    }
                }
            }

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        String[] arr = new String[0];
        videoKeys = videoKeyz.toArray(arr);
        imageKeys = imageKeyz.toArray(arr);

        logger.info("Total MuteDecoder groups: " + String.valueOf(muteDecoderGroups.size()) + " | " + "Max Decoders per group: " + String.valueOf(DECODERS_PER_GROUP));
        populated = true;
    }

    public static String getVideoPath(String key) {
        String result = videoMap.get(key);
        if (result == null) throw new RuntimeException("VideoLib attempted to resolve absolute path for video file id " + key + ", but returned null");
        return result;
    }

    public static String getVideoId(String path) {
        return reverseVideoMap.get(path);
    }

    public static String getImagePath(String key) {
        String result = imageMap.get(key);
        if (result == null) throw new RuntimeException("VideoLib attempted to resolve absolute path for image file id " + key + ", but returned null");
        return result;
    }

    public static List<AutoTexProjectorAPI> getAutoTexProjectorsWithCampaignSpeedup() {
        return new ArrayList<>(autoTexWithCampaignSpeedup);
    }

    public static AutoTexProjectorAPI getAutoTexProjectorOverride(String texturePath) {
        return autoTexMap.get(texturePath);
    }

    public static void removeAutoTexOverride(String texturePath, AutoTexProjectorAPI autoTex) {
        autoTexMap.remove(texturePath);
        autoTexWithCampaignSpeedup.remove(autoTex);
        allAutoTexOverrides.remove(autoTex);
        if (autoTex.combatRunWhilePaused()) 
            runWhilePausedCombatAutoTexOverrides.remove(autoTex);
    }

    public static void timeoutAutoTexOverride(AutoTexProjectorAPI autoTex) {
        Global.getSector().removeTransientScript(autoTex);
        allAutoTexOverrides.remove(autoTex);
        if (autoTex.combatRunWhilePaused()) 
            runWhilePausedCombatAutoTexOverrides.remove(autoTex);
    }

    public static void unTimeoutAutoTexOverride(AutoTexProjectorAPI autoTex) {
        Global.getSector().addTransientScript(autoTex);
        allAutoTexOverrides.add(autoTex);
        if (autoTex.combatRunWhilePaused())
            runWhilePausedCombatAutoTexOverrides.add(autoTex);
        
    }

    protected static List<AutoTexProjectorAPI> getAutoTexOverrides(boolean isPaused) {
        return isPaused ? new ArrayList<>(runWhilePausedCombatAutoTexOverrides) : new ArrayList<>(allAutoTexOverrides) ;
    }

    public static List<MuteDecoderGroup> getMuteDecoderGroups() {
        return muteDecoderGroups;
    }

    public static String[] imageKeys() {
        return imageKeys;
    }

    public static String[] videoKeys() {
        return videoKeys;
    }

    private static JSONObject getJSONObject(String modId, JSONObject toGetFrom, String key) {
        try {
            return toGetFrom.getJSONObject(key);
        } catch (JSONException e) {
            logger.warn("Error for VideoLib path resolution for mod id " + modId + ": ", e);
            return null;
        }
    }

    private static boolean isRunWhilePaused(JSONObject autoTexOverrideData, String prefix) {
        try {
            return autoTexOverrideData.getBoolean(prefix + "RunWhilePaused");
        } catch (JSONException ignored) {
            return true;
        }
    }

    private static boolean useCampaignSpeedup(JSONObject autoTexOverrideData) {
        try {
            return autoTexOverrideData.getBoolean("useCampaignSpeedup");
        } catch (JSONException ignored) {
            return false;
        }
    }

    private static boolean isAudio(JSONObject autoTexOverrideData) {
        try {
            return Global.getSettings().isSoundEnabled() ? autoTexOverrideData.getBoolean("playAudio") : false;
        } catch (JSONException ignored) {
            return false;
        }
    }

    private static boolean isOverWriteInVram(JSONObject autoTexOverrideData) {
        try {
            return autoTexOverrideData.getBoolean("overwriteInVram");
        } catch (JSONException ignored) {
            return false;
        }
    }
    
    private static int getWidth(JSONObject autoTexOverrideData, Object original) {
        try {
            return autoTexOverrideData.getInt("width");
        } catch (JSONException ignored) {
            return TexReflection.getTexObjSourceWidth(original);
        }
    }

    private static int getHeight(JSONObject autoTexOverrideData, Object original) {
        try {
            return autoTexOverrideData.getInt("height");
        } catch (JSONException ignored) {
            return TexReflection.getTexObjSourceHeight(original);
        }
    }
}
