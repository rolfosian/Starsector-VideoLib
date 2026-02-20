package videolib.console;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.campaign.BaseLocation;

import videolib.VideoPlayerFactory;
import videolib.entities.CampaignBillboard;
import videolib.entities.RotationalTargeter;
import videolib.entities.CampaignBillboard.TargetDelegate;

public class VideoLibBillboardTest implements BaseCommand {
    @Override
    public CommandResult runCommand(String arg0, CommandContext arg1) {
        if (Global.getCurrentState() != GameState.CAMPAIGN) {
            Console.showMessage("This command is only applicable to Campaign");
            return CommandResult.WRONG_CONTEXT;
        }

        List<String> args = Arrays.asList(arg0.split(" "));

        String type = getType(args);
        String name = getName(args);
        String faction = getFaction(args);

        args = args.stream().map(String::toLowerCase).toList();
        boolean faceFleet = args.contains("facefleet");
        boolean faceOrbitFocus = args.contains("faceorbitfocus");
        boolean noOrbit = args.contains("noorbit");
        boolean contested = args.contains("contested");

        LocationAPI location = Global.getSector().getPlayerFleet().getContainingLocation();
        Vector2f playerLoc = Global.getSector().getPlayerFleet().getLocation();

        Map<String, Object> params = new HashMap<>();
        Map<String, String> factionSpriteMap = new HashMap<>();
        factionSpriteMap.put("hegemony", "graphics/billboards/vl_example_360p.png");
        params.put("factionSpriteMap", factionSpriteMap);

        CampaignBillboard testBillboard = VideoPlayerFactory.addCampaignBillboard(
            (BaseLocation)location,
            null,
            name,
            type,
            faction, // holo uses faction color by default, can be set manually
            -1f,
            -1f,
            params,
            -1f,
            -1f,
            null,
            null
        );

        testBillboard.setLocation(playerLoc.x, playerLoc.y);

        if (location instanceof StarSystemAPI system) {
            if (!noOrbit && system.getPlanets().size() != 0) {
                PlanetAPI closest = null;

                float closestDistance = Float.MAX_VALUE;
                for (PlanetAPI planet : system.getPlanets()) {
                    float dist = RotationalTargeter.distanceBetween(planet.getLocation(), playerLoc);
                    if (dist < closestDistance) {
                        closest = planet;
                        closestDistance = dist;
                    }
                }

                Vector2f planetLoc = closest.getLocation();

                float angle = RotationalTargeter.angleBetween(playerLoc, planetLoc);
                testBillboard.setCircularOrbit(closest, angle, RotationalTargeter.distanceBetween(playerLoc, closest.getLocation()), 15f);

                if (faceOrbitFocus) testBillboard.setAngleDelegate(new RotationalTargeter.PointAtOrbitFocus());
            }
        }

        if (faceFleet) {
            testBillboard.setAngleDelegate(new RotationalTargeter(new TargetDelegate() {
                @Override
                public SectorEntityToken target(CampaignBillboard billboard) {
                    return RotationalTargeter.getNearestFleet(billboard.getBillboardEntity());
                }
            }));
        }

        if (contested) {
            testBillboard.setContested(true);
        }

        Console.showMessage("Billboard of type " + type + " added to player location.");
        return CommandResult.SUCCESS;

    }

    private static String getType(List<String> args) {
        for (String arg : args) {
            if (arg.startsWith("type")) {
                return arg.split(":")[1];
            }
        }
        return "vl_billboard_example";
    }

    private static String getName(List<String> args) {
        for (String arg : args) {
            if (arg.startsWith("name")) {
                return arg.split(":")[1];
            }
        }
        return "Test Billboard";
    }

    private static String getFaction(List<String> args) {
        List<FactionAPI> factions = Global.getSector().getAllFactions();
        for (String arg : args) {
            for (FactionAPI faction : factions) {
                if (arg.startsWith("faction") && arg.split(":")[1].equals(faction.getId())) return faction.getId();
            }
        }
        return factions.get(ThreadLocalRandom.current().nextInt(factions.size())).getId();
    }
}
