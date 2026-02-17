package videolib;

import java.util.*;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;

import videolib.decoder.grouped.DeltaTimeDelegator;
import videolib.entities.CampaignBillboard;
import videolib.ffmpeg.FFmpeg;

import videolib.planetlistener.PlanetProjectorListener;
import videolib.projector.AutoTexProjector;
import videolib.projector.PlanetProjector;
import videolib.projector.Projector;
import videolib.projector.AutoTexProjector.AutoTexProjectorAPI;
import videolib.util.TexReflection;
import videolib.util.VideoUtils;

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
        VideoUtils.init();
        AutoTexProjector.init();
        VideoPaths.populate();
        CampaignBillboard.initStatic();

        SettingsAPI settings = Global.getSettings();
        try {
            settings.loadTexture("graphics/starscape/star.png");
            settings.loadTexture("graphics/billboards/vl_lens_platform1.png");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        mainThread = Thread.currentThread();
    }

    private static List<PlanetProjector> planetProjectors = new ArrayList<>();
    private static List<Object> ringBandAndSpriteProjectors = new ArrayList<>();
    
    private static long saveTime;

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
    public void onNewGame() {
        Collection<PlanetProjector> projectors = VideoUtils.getPlanetProjectors();
        Set<EveryFrameScript> projectorz = VideoUtils.getRingBandAndSpriteProjectors();

        if (!projectors.isEmpty()) {
            for (PlanetProjector projector : new ArrayList<>(projectors)) {
                projector.finish();
            }
        }

        if (!projectorz.isEmpty()) {
            for (EveryFrameScript projector : new ArrayList<>(projectorz)) {
                ((Projector)projector).finish();
            }
        }

        saveTime = (long) Global.getSector().getPersistentData().getOrDefault("$vl_saveTime", 0L);
        if (saveTime == 0L) {
            saveTime = System.currentTimeMillis();
            Global.getSector().getPersistentData().put("$vl_saveTime", saveTime);
        }
    }

    private boolean isSameSave() {
        long newSaveTime = (long) Global.getSector().getPersistentData().getOrDefault("$vl_saveTime", 0L);
        return (newSaveTime != 0L && saveTime == newSaveTime);
    }

    @Override
    public void onGameLoad(boolean newGame) {
        Global.getSector().addTransientScript(VideoLibEveryFrame.getInstance());
        Global.getSector().addTransientListener(VideoLibCampaignListener.getInstance());
        Global.getSector().addTransientListener(new PlanetProjectorListener(false));

        for (AutoTexProjectorAPI projector : VideoPaths.getAutoTexOverrides(false)) {
            projector.timeout();
        }

        for (AutoTexProjectorAPI projector : VideoPaths.getAutoTexProjectorsWithCampaignSpeedup())  {
            ((DeltaTimeDelegator)projector.getDecoder()).setCampaign();
        }

        if (newGame) return;

        Collection<PlanetProjector> projectors = VideoUtils.getPlanetProjectors();
        Set<EveryFrameScript> projectorz = VideoUtils.getRingBandAndSpriteProjectors();

        boolean isSameSave = isSameSave();

        if (!projectors.isEmpty()) {
            for (PlanetProjector projector : new ArrayList<>(projectors)) {
                projector.finish();
    
                if (isSameSave && projector.getCampaignPlanet() != null ) {
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

        if (!projectorz.isEmpty()) {
            if (isSameSave) {
                for (EveryFrameScript projector : new ArrayList<>(projectorz)) {
                    Projector proj = (Projector) projector;
                    proj.finish();
                    proj.restart();
                }
            } else {
                for (EveryFrameScript projector : new ArrayList<>(projectorz)) {
                    Projector proj = (Projector) projector;
                    proj.finish();
                }
            }

        }

        saveTime = (long) Global.getSector().getPersistentData().getOrDefault("$vl_saveTime", 0L);
        if (saveTime == 0L) {
            saveTime = System.currentTimeMillis();
            Global.getSector().getPersistentData().put("$vl_saveTime", saveTime);
        }
    }

    public static Thread getMainThread() {
        return mainThread;
    }
}
