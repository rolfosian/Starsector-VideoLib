package data.scripts.console;

import data.scripts.projector.PlanetProjector;
import data.scripts.projector.PlanetProjector.PlanetTexType;

import data.scripts.VideoPaths;

import java.util.*;

import org.lazywizard.console.BaseCommand;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.IntervalUtil;

public class VideoLibPlanetsDemo implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (context != CommandContext.CAMPAIGN_MAP) return CommandResult.WRONG_CONTEXT;

        LocationAPI playerLoc = Global.getSector().getPlayerFleet().getContainingLocation();
        if (!(playerLoc instanceof StarSystemAPI) || playerLoc.isHyperspace() || playerLoc.isDeepSpace()) return CommandResult.WRONG_CONTEXT;

        List<String> splitArgs = Arrays.asList(args.split(" "));

        String fileId = null;
        for (String id : VideoPaths.keys()) {
            if (splitArgs.contains(id)) {
                fileId = id;
                break;
            }
        }

        int videoWidth = 512;
        int videoHeight = 256;

        if (fileId == null) fileId = "video_lib_planets_demo";

        List<PlanetProjector> projectors = new ArrayList<>();

        
        for (PlanetAPI planet : playerLoc.getPlanets()) {
            PlanetProjector projector = new PlanetProjector(planet, fileId, videoWidth, videoHeight, PlanetTexType.SHIELD);

            Global.getSector().addTransientScript(projector);
            projectors.add(projector);
        }

        // VERY IMPORTANT:
        // Stop projectors and their decoders, close ffmpeg pipes and clean up when player leaves system, or otherwise some other method. Maybe will add a console command that does this
        // Or leak the memory im not your boss
        Global.getSector().addTransientScript(new EveryFrameScript() {
            private boolean isDone = false;
            private IntervalUtil interval = new IntervalUtil(0.2f, 0.2f);

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
