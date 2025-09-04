package data.scripts.planetlistener;

import data.scripts.projector.PlanetProjector;
import data.scripts.util.VideoUtils;

import java.util.*;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.combat.CombatEngine;
import com.fs.starfarer.api.combat.ViewportAPI;

import com.fs.starfarer.api.input.InputEventAPI;

import com.fs.starfarer.combat.entities.terrain.Planet;
import com.fs.util.container.repo.ObjectRepository;

public class CombatPlanetChecker implements EveryFrameCombatPlugin {
    private static final Logger logger = Logger.getLogger(CombatPlanetChecker.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    private CombatEngine engine;
    private Set<Planet> planetSet = new HashSet<>();

    @Override
    public void init(CombatEngineAPI arg0) {
        engine = (CombatEngine) arg0;
    }

    @Override
    public void advance(float arg0, List<InputEventAPI> arg1) {
        if (Global.getCurrentState() != GameState.COMBAT || engine.isPaused()) return;
        planetSet.addAll(engine.getObjects().getList(Planet.class));

        for (Planet planet : planetSet) {
            for(String tag : planet.getSpec().getTags()) {
                if (tag.startsWith("vl")) {
                    PlanetProjector projector = VideoUtils.getPlanetProjector(tag);
                    if (projector != null) projector.advance(arg0);
                    break;
                }
            }
        }
    }

    @Override
    public void processInputPreCoreControls(float arg0, List<InputEventAPI> arg1) {

    }

    @Override
    public void renderInUICoords(ViewportAPI arg0) {

    }

    @Override
    public void renderInWorldCoords(ViewportAPI arg0) {

    }
}
