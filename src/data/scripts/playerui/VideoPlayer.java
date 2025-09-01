package data.scripts.playerui;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import data.scripts.projector.VideoProjector;

public interface VideoPlayer {
    public PositionAPI addTo(UIPanelAPI parent);
    public void init();
    public CustomPanelAPI getProjectorPanel();
    public VideoProjector getProjector();
    public void setClickToPause(boolean clickToPause);
}
