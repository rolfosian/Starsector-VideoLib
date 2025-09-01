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
    private static Map<String, String> map = new HashMap<>();

    protected static void populate() {
        if (populated) return;
        Logger logger = Logger.getLogger(VideoPaths.class);

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
                JSONObject filePaths = settings.getJSONObject(modId);
                
                Iterator<String> fileIds = filePaths.keys();
                while (fileIds.hasNext()) {
                    String fileId = fileIds.next();

                    if (map.containsKey(fileId)) {
                        throw new IllegalArgumentException("Duplicate video file ID " + fileId + " for mod id " + modId + " already located at " + map.get(fileId));
                    }

                    String relativePath = filePaths.getString(fileId);
                    map.put(fileId, modPath + "/" + relativePath);

                    logger.info("Loaded video file id " + fileId + " at " + modPath + "/" + relativePath);
                }
            }

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        populated = true;
    }

    public static String get(String key) {
        String result = map.get(key);
        if (result == null) throw new RuntimeException("VideoLib attempted to resolve absolute path for file id " + key + ", but returned null");
        return result;
    }
}
