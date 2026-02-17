package videolib;

import java.util.*;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import videolib.decoder.grouped.DeltaTimeDelegator;
import videolib.projector.AutoTexProjector.AutoTexProjectorAPI;

import static videolib.VideoLibModPlugin.print;

public class VideoLibCombatPlugin extends BaseEveryFrameCombatPlugin {
    @Override
    public void init(CombatEngineAPI engine) {
        for (AutoTexProjectorAPI projector : VideoPaths.getAutoTexProjectorsWithCampaignSpeedup())  {
            ((DeltaTimeDelegator)projector.getDecoder()).setCombat();
        }
    }

    @Override
    public void advance(float dt, List<InputEventAPI> events) {
        GameState state = Global.getCurrentState();
        if (state == GameState.COMBAT || state == GameState.TITLE) {
            for (AutoTexProjectorAPI projector : VideoPaths.getAutoTexOverrides(Global.getCombatEngine().isPaused())) {
                projector.advance(dt);
            }
        }
    }
}
