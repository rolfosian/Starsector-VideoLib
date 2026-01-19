package videolib.playerui;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import videolib.decoder.Decoder;
import videolib.projector.VideoProjector;
import videolib.speakers.Speakers;

public interface VideoPlayer {
    public PositionAPI addTo(UIPanelAPI parent);

    /** Must be called after masterPanel is added to target parent AND positioned*/
    public void init();
    
    public CustomPanelAPI getMasterPanel();
    public CustomPanelAPI getProjectorPanel();
    public VideoProjector getProjector();
    public PlayerControlPanel getControls();
    public Speakers getSpeakers();
    public Decoder getDecoder();

    public void setClickToPause(boolean clickToPause);
    public void openNewVideo(String videoId, int width, int height);

    public float getWidth();
    public float getHeight();

    public void finish();
}
