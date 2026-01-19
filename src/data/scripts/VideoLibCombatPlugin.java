package data.scripts;

import java.util.*;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.input.InputEventAPI;

import data.scripts.projector.AutoTexProjector.AutoTexProjectorAPI;

public class VideoLibCombatPlugin extends BaseEveryFrameCombatPlugin {
    @Override
    public void advance(float dt, List<InputEventAPI> events) {
        GameState state = Global.getCurrentState();
        if (state == GameState.COMBAT || state == GameState.TITLE) {
            for (AutoTexProjectorAPI projector : VideoPaths.getAutoTexOverrides()) {
                projector.advance(dt);
            }
        }
    }
}
