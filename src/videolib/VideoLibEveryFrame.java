package videolib;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

public final class VideoLibEveryFrame implements EveryFrameScript {
    public static float campaignDt;
    public static float phaseDelta;

    @Override
    public void advance(float dt) {
        campaignDt = Global.getSector().getCampaignUI().isFastForward() ? dt * Global.getSettings().getFloat("campaignSpeedupMult") : dt;
        phaseDelta = campaignDt * 10f;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }
    
}
