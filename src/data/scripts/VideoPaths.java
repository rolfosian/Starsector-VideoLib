package data.scripts;

import java.util.*;

import org.json.JSONObject;
import org.json.JSONException;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.ModSpecAPI;

@SuppressWarnings("unchecked")
public class VideoPaths {
    public static boolean populated = false;

    private static Map<String, String> videoMap = new HashMap<>();
    private static String[] videoKeys;

    private static Map<String, String> imageMap = new HashMap<>();
    private static String[] imageKeys;

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

                JSONObject videoFilePaths = pathData.getJSONObject("videos");
                Iterator<String> fileIds = videoFilePaths.keys();

                while (fileIds.hasNext()) {
                    String fileId = fileIds.next();

                    if (videoMap.containsKey(fileId)) {
                        throw new IllegalArgumentException("Duplicate video file ID " + fileId + " for mod id " + modId + " already located at " + videoMap.get(fileId));
                    }

                    String relativePath = videoFilePaths.getString(fileId);
                    videoMap.put(fileId, modPath + "/" + relativePath);
                    videoKeyz.add(fileId);

                    logger.info("Resolved absolute path for video file id " + fileId + " at " + modPath + "/" + relativePath);
                }

                JSONObject imageFilePaths = pathData.getJSONObject("images");
                fileIds = imageFilePaths.keys();

                while (fileIds.hasNext()) {
                    String fileId = fileIds.next();

                    if (imageMap.containsKey(fileId)) {
                        throw new IllegalArgumentException("Duplicate image file ID " + fileId + " for mod id " + modId + " already located at " + imageMap.get(fileId));
                    }

                    String relativePath = imageFilePaths.getString(fileId);
                    imageMap.put(fileId, modPath + "/" + relativePath);
                    imageKeyz.add(fileId);

                    logger.info("Resolved absolute path for image file id " + fileId + " at " + modPath + "/" + relativePath);
                }
            }

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        String[] arr = new String[0];
        videoKeys = videoKeyz.toArray(arr);
        imageKeys = imageKeyz.toArray(arr);

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

    public static String[] imageKeys() {
        return imageKeys;
    }

    public static String[] videoKeys() {
        return videoKeys;
    }
}
