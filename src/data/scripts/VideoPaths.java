package data.scripts;

import java.util.*;

import org.json.JSONObject;
import org.json.JSONException;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.ModSpecAPI;

@SuppressWarnings("unchecked")
public class VideoPaths {
    public static boolean populated = false;
    public static Map<String, String> map;

    protected static void populate() {
        if (populated) return;

        Map<String, String> mape = new HashMap<>();
        try {
            ModManagerAPI modManager = Global.getSettings().getModManager();
            JSONObject settings = Global.getSettings().getJSONObject("VideoLib");
            
            Iterator<String> modIds = settings.keys();
            while (modIds.hasNext()) {
                String modId = modIds.next();
                ModSpecAPI modSpec = modManager.getModSpec(modId);

                if (modSpec == null)  {
                    Global.getLogger(VideoPaths.class).warn("ModSpecAPI.getModSpec returned null for modId " + modId + " in VideoLib settings, ignoring");
                    continue;
                }

                String modPath = modSpec.getPath();
                JSONObject filePaths = settings.getJSONObject(modId);
                
                Iterator<String> fileIds = filePaths.keys();
                while (fileIds.hasNext()) {
                    String fileId = fileIds.next();

                    if (mape.containsKey(fileId)) {
                        throw new IllegalArgumentException("Duplicate video file ID " + fileId + " for mod id " + modId + " already located at " + mape.get(fileId));
                    }

                    String relativePath = filePaths.getString(fileId);
                    mape.put(fileId, modPath + "/" + relativePath);
                }
            }

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        map = Collections.unmodifiableMap(mape);
        populated = true;
    }
}
