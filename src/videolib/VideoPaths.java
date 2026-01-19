package videolib;

import java.util.*;

import org.json.JSONObject;
import org.json.JSONException;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.ModSpecAPI;

import videolib.ffmpeg.FFmpeg;
import videolib.util.TexReflection;

import videolib.projector.AutoTexProjector;
import videolib.projector.AutoTexProjector.AutoTexProjectorAPI;

@SuppressWarnings("unchecked")
public class VideoPaths {
    public static boolean populated = false;

    private static Map<String, String> videoMap = new HashMap<>();
    private static String[] videoKeys;

    private static Map<String, String> imageMap = new HashMap<>();
    private static String[] imageKeys;

    private static Map<String, AutoTexProjectorAPI> autoTexMap = new HashMap<>();
    private static List<AutoTexProjectorAPI> autoTexOverrides;

    protected static void populate() {
        if (populated) return;
        Logger logger = Logger.getLogger(VideoPaths.class);
        List<String> videoKeyz = new ArrayList<>();
        List<String> imageKeyz = new ArrayList<>();

        try {
            ModManagerAPI modManager = Global.getSettings().getModManager();
            JSONObject settings = Global.getSettings().getJSONObject("VideoLib");
            
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

                JSONObject videoFilePaths = getJSONObject(modId, pathData, "videos", logger);
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
                        videoKeyz.add(fileId);
    
                        logger.info("Resolved absolute path for video file id " + fileId + " at " + modPath + "/" + relativePath);
                    }
                }

                JSONObject imageFilePaths = getJSONObject(modId, pathData, "images", logger);
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
                
                JSONObject textureOverrides = getJSONObject(modId, pathData, "autoTexProjectorOverrides", logger);
                if (textureOverrides != null) {
                    Iterator<String> texturePaths = textureOverrides.keys();

                    while (texturePaths.hasNext()) {
                        String texturePath = texturePaths.next();
                        JSONObject overrideData = textureOverrides.getJSONObject(texturePath);
    
                        String videoId = overrideData.getString("videoId");
                        int width = overrideData.getInt("width");
                        int height = overrideData.getInt("height");
    
                        if (!TexReflection.texObjectMap.containsKey(texturePath)) {
                            throw new IllegalArgumentException(texturePath + "not found in Starsector texture repository for video override: " + videoId);
                        }
                        if (autoTexMap.containsKey(texturePath)) {
                            throw new IllegalArgumentException("video override already found for texture " + texturePath);
                        }
    
                        AutoTexProjectorAPI ours = AutoTexProjector.instantiator.instantiate(videoId, width, height);
                        int initialTex = ours.getCurrentTextureId();

                        Object original = TexReflection.texObjectMap.get(texturePath);
                        TexReflection.transplantTexFields(original, ours);

                        TexReflection.setTexObjId(ours, initialTex);
                        ours.setOriginalTexture(texturePath, textureOverrides);

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
        autoTexOverrides = new ArrayList<>(autoTexMap.values());

        populated = true;
    }

    public static String getVideoPath(String key) {
        String result = videoMap.get(key);
        if (result == null) throw new RuntimeException("VideoLib attempted to resolve absolute path for video file id " + key + ", but returned null");
        return result;
    }

    public static String getImagePath(String key) {
        String result = imageMap.get(key);
        if (result == null) throw new RuntimeException("VideoLib attempted to resolve absolute path for image file id " + key + ", but returned null");
        return result;
    }

    public static AutoTexProjectorAPI getAutoTexProjectorOverride(String texturePath) {
        return autoTexMap.get(texturePath);
    }

    public static void removeAutoTexOverride(String texturePath, AutoTexProjectorAPI autoTex) {
        autoTexMap.remove(texturePath);
        autoTexOverrides.remove(autoTex);
    }

    protected static List<AutoTexProjectorAPI> getAutoTexOverrides() {
        return autoTexOverrides;
    }

    public static String[] imageKeys() {
        return imageKeys;
    }

    public static String[] videoKeys() {
        return videoKeys;
    }

    private static JSONObject getJSONObject(String modId, JSONObject toGetFrom, String key, Logger logger) {
        try {
            return toGetFrom.getJSONObject(key);
        } catch (JSONException e) {
            logger.warn("Error for VideoLib path resolution for mod id " + modId + ": ", e);
            return null;
        }
    }
}
