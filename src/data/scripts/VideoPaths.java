package data.scripts;

import java.util.*;

import org.json.JSONObject;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.ModSpecAPI;

@SuppressWarnings("unchecked")
public class VideoPaths {
    private static final Logger logger = Logger.getLogger(VideoPaths.class);
    
    public static boolean populated = false;
    public static final Map<String, String> map = new HashMap<>();

    protected static void populate() {
        if (populated) return;

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
                JSONArray videoFilenames = settings.getJSONArray(modId);
                
                for (int i = 0; i < videoFilenames.length(); i++) {
                    String filename = videoFilenames.getString(i);

                    if (map.containsKey(filename)) {
                        throw new RuntimeException("Duplicate video filename already located at " + map.get(filename));
                    }

                    map.put(filename, modPath + "/data/videos/" + filename);
                }
            }

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        populated = true;
    }
}
