package videolib;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

public final class VideoLibEveryFrame implements EveryFrameScript {
    private static VideoLibEveryFrame instance;
    public static float campaignSpeedupMult = Global.getSettings().getFloat("campaignSpeedupMult");
    
    public static float campaignDt;
    public static float phaseDelta;

    private VideoLibEveryFrame() {}

    public static VideoLibEveryFrame getInstance() {
        if (instance != null) {
            Global.getSector().removeTransientScript(instance);
        }
        return instance = new VideoLibEveryFrame();
    }

    @Override
    public void advance(float dt) {
        campaignDt = Global.getSector().getCampaignUI().isFastForward() ? dt * campaignSpeedupMult : dt;
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
