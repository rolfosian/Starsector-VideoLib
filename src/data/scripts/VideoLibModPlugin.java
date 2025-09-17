package data.scripts;

import java.nio.IntBuffer;
import java.util.*;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.*;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.characters.PersonAPI;

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
        if (Global.getSettings().isSoundEnabled()) {
            ALCcontext context = ALC10.alcGetCurrentContext();
            ALCdevice device = ALC10.alcGetContextsDevice(context);

            IntBuffer buffer = BufferUtils.createIntBuffer(1);
            ALC10.alcGetInteger(device, ALC10.ALC_FREQUENCY, buffer);
            int sampleRate = buffer.get(0);
            FFmpeg.init(sampleRate);
            
        } else {
            FFmpeg.init(0);
        }
        
        TexReflection.init();
        VideoUtils.init();

        VideoPaths.populate();
        mainThread = Thread.currentThread();
    }

    private static List<PlanetProjector> planetProjectors = new ArrayList<>();
    private static List<Object> ringBandAndSpriteProjectors = new ArrayList<>();
    private static PersonAPI playerPerson;

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
        playerPerson = Global.getSector().getPlayerPerson();

    }

    private boolean isSameSave() {
        PersonAPI sectorPerson = Global.getSector().getPlayerPerson();
        if (playerPerson != null && playerPerson.getId().equals(sectorPerson.getId())
            && playerPerson.getName().getFullName().equals(sectorPerson.getName().getFullName())) return true;
        return false;
    }

    @Override
    public void onGameLoad(boolean newGame) {
        Global.getSector().addTransientListener(new PlanetProjectorListener(false));
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
        playerPerson = Global.getSector().getPlayerPerson();
    }

    public static Thread getMainThread() {
        return mainThread;
    }

}
