package data.scripts;

import java.util.*;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;

// import data.scripts.buffers.OverlayingTextureBuffer;
import data.scripts.ffmpeg.FFmpeg;
import data.scripts.planetlistener.PlanetProjectorListener;
import data.scripts.projector.PlanetProjector;
import data.scripts.projector.Projector;
import data.scripts.util.TexReflection;
import data.scripts.util.VideoUtils;

public class VideoLibModPlugin extends BaseModPlugin {
    public static final Logger logger = Global.getLogger(VideoLibModPlugin.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    private static Thread mainThread;

    @Override
    public void onApplicationLoad() {
        FFmpeg.init();
        TexReflection.init();

        VideoPaths.populate();
        mainThread = Thread.currentThread();
    }

    private static List<PlanetProjector> planetProjectors = new ArrayList<>();
    private static List<Object> ringBandAndSpriteProjectors = new ArrayList<>();

    @Override
    public void beforeGameSave() {
        for (PlanetProjector projector : VideoUtils.getPlanetProjectors()) planetProjectors.add(projector);
        for (PlanetProjector projector : planetProjectors) projector.finish();

        for (EveryFrameScript projector : VideoUtils.getRingBandAndSpriteProjectors()) ringBandAndSpriteProjectors.add(projector);
        for (Object projector : ringBandAndSpriteProjectors) ((Projector)projector).finish();
    }

    @Override
    public void afterGameSave() {
        for (PlanetProjector projector : planetProjectors) {
            if (projector.getCampaignPlanet() != null) {
                PlanetAPI target = (PlanetAPI) Global.getSector().getEntityById(projector.getCampaignPlanet().getId());

                Global.getSector().addTransientScript(
                    new PlanetProjector(target, projector.getVideoId(),
                        projector.getWidth(), projector.getHeight(),
                        projector.getDecoder().getCurrentVideoPts(),
                        projector.getTexType())
                );
                
            } else {
                projector.restart();
            }

        }
        planetProjectors.clear();

        for (Object projector : ringBandAndSpriteProjectors) {
            ((Projector)projector).restart();
        }
        ringBandAndSpriteProjectors.clear();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        Global.getSector().addTransientListener(new PlanetProjectorListener(false));
        Collection<PlanetProjector> projectors = VideoUtils.getPlanetProjectors();

        if (!projectors.isEmpty()) {
            for (PlanetProjector projector : new ArrayList<>(projectors)) {
                projector.finish();
    
                if (projector.getCampaignPlanet() != null) {
                    PlanetAPI target = (PlanetAPI) Global.getSector().getEntityById(projector.getCampaignPlanet().getId());
    
                    Global.getSector().addTransientScript(
                        new PlanetProjector(target, projector.getVideoId(),
                            projector.getWidth(), projector.getHeight(),
                            projector.getDecoder().getCurrentVideoPts(),
                            projector.getTexType())
                    );
                }
            }
        }

        for (EveryFrameScript projector : VideoUtils.getRingBandAndSpriteProjectors()) {
            ((Projector)projector).restart();
        }
    }

    public static Thread getMainThread() {
        return mainThread;
    }

}
