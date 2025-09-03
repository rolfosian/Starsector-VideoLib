package data.scripts.playerui;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import data.scripts.decoder.Decoder;
import data.scripts.projector.VideoProjector;

public interface VideoPlayer {
    public PositionAPI addTo(UIPanelAPI parent);
    public void init();
    
    public CustomPanelAPI getMasterPanel();
    public CustomPanelAPI getProjectorPanel();
    public VideoProjector getProjector();
    public Decoder getDecoder();

    public void setClickToPause(boolean clickToPause);
    public void openNewVideo(String videoId, int width, int height);

    public float getWidth();
    public float getHeight();
}
