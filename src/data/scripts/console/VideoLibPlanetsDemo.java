package data.scripts.console;

import data.scripts.projector.PlanetProjector;
import data.scripts.projector.PlanetProjector.PlanetTexType;

import data.scripts.VideoPaths;
import data.scripts.ffmpeg.FFmpeg;

import java.util.*;

import org.lazywizard.console.BaseCommand;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.util.IntervalUtil;


public class VideoLibPlanetsDemo implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (context != CommandContext.CAMPAIGN_MAP) return CommandResult.WRONG_CONTEXT;

        LocationAPI playerLoc = Global.getSector().getPlayerFleet().getContainingLocation();
        if (!(playerLoc instanceof StarSystemAPI) || playerLoc.isHyperspace() || playerLoc.isDeepSpace()) return CommandResult.WRONG_CONTEXT;

        List<String> splitArgs = Arrays.asList(args.split(" "));

        String fileId = null;
        for (String id : VideoPaths.videoKeys()) {
            if (splitArgs.contains(id)) {
                fileId = id;
                break;
            }
        }

        int videoWidth = 0;
        int videoHeight = 0;
        Object planetTexType = PlanetTexType.SHIELD2;

        for (String arg : splitArgs) {
            if (arg.startsWith("width:")) {
                videoWidth = Integer.parseInt(arg.split(":")[1]);

            } else if (arg.startsWith("height:")) {
                videoHeight = Integer.parseInt(arg.split(":")[1]);

            } else if (arg.startsWith("textype:")) {
                switch(arg.split(":")[1].toLowerCase()) {
                    case "planet":
                        planetTexType = PlanetTexType.PLANET;
                        break;
                        
                    case "atmosphere":
                        planetTexType = PlanetTexType.ATMOSPHERE;
                        break;
                        
                    case "cloud":
                        planetTexType = PlanetTexType.CLOUD;
                        break;
                        
                    case "shield":
                        planetTexType = PlanetTexType.SHIELD;
                        break;
                        
                    case "shield2":
                        planetTexType = PlanetTexType.SHIELD2;
                        break;
                        
                    case "glow":
                        planetTexType = PlanetTexType.GLOW;
                        break;
                    
                    default:
                        break;
                }
            }
        }

        if (fileId == null)  {
            fileId = "vl_ufos";
            videoWidth = 512;
            videoHeight = 256;
        }

        if (videoWidth == 0 || videoHeight == 0) {
            int[] dimensions = FFmpeg.getWidthAndHeight(VideoPaths.getVideoPath(fileId));
            videoWidth = dimensions[0];
            videoHeight = dimensions[1];
        }

        List<PlanetProjector> projectors = new ArrayList<>();
        
        for (PlanetAPI planet : playerLoc.getPlanets()) {
            PlanetProjector projector = new PlanetProjector(planet, fileId, videoWidth, videoHeight, 0, planetTexType);

            Global.getSector().addTransientScript(projector);
            projectors.add(projector);
        }

        // VERY IMPORTANT:
        // Stop projectors and their decoders, close ffmpeg pipes and clean up when player leaves system, or otherwise via some other method
        // Or leak the memory and leave the decoder thread running with the file open im not your boss
        Global.getSector().addTransientScript(new EveryFrameScript() {
            private boolean isDone = false;
            private IntervalUtil interval = new IntervalUtil(0.2f, 0.2f);
            private ViewportAPI viewPort = Global.getSector().getViewport();

            @Override
            public void advance(float arg0) {
                interval.advance(arg0);
                if (interval.intervalElapsed()) {
                    if (Global.getSector().getPlayerFleet().getContainingLocation() != playerLoc) {
                        for (PlanetProjector projector : projectors) {
                            projector.finish();
                        }
                        isDone = true;
                        Global.getSector().removeTransientScript(this);
                        return;
                    }
                }

                // minimize overhead by pausing projectors not near viewport
                for (PlanetProjector projector : projectors) {
                    boolean isNearViewPort = viewPort.isNearViewport(projector.getPlanet().getLocation(), 150f);
                    boolean paused = projector.paused();

                    if (paused && isNearViewPort) {
                        projector.unpause();
                    } else if (!paused && !isNearViewPort) {
                        projector.pause();
                    }
                }
            }

            @Override
            public boolean isDone() {
                return isDone;
            }

            @Override
            public boolean runWhilePaused() {
                return false;
            }
        });

        return CommandResult.SUCCESS;
    }
    
}
