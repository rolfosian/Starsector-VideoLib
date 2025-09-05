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

@SuppressWarnings("unchecked")
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

    private Set<Planet> planetSet = new HashSet<>();

    @Override /*  TODO: This requires optimization. PlanetProjectors are EveryFrameScript implementations - they do not run in combat. Also, the planet objects (and their specs) being rendered in combat
    are *clones* of their originals. When the planets and their specs are cloned only the spec's tags are carried over, the planet's customData map is not. Therefore we cannot map directly to the projectors
    and have to do this abhorrent iteration. Also, in case a new projector is instantiated during combat, we need to keep iterating like this.*/
    public void advance(float arg0, List<InputEventAPI> arg1) {
        CombatEngine engine = (CombatEngine) Global.getCombatEngine();
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

    @Override
    public void init(CombatEngineAPI arg0) {}
}
