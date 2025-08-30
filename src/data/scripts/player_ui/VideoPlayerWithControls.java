package data.scripts.player_ui;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import data.scripts.projector.VideoProjector;

public class VideoPlayerWithControls {
    public final CustomPanelAPI masterPanel;
    public final CustomPanelAPI projectorPanel;

    public final VideoProjector projector;
    public final PlayerControlPanel controlPanel;

    public VideoPlayerWithControls(CustomPanelAPI masterPanel, PlayerControlPanel controlPanel, VideoProjector projector, CustomPanelAPI projectorPanel) {
        this.masterPanel = masterPanel;
        this.controlPanel = controlPanel;
        this.projector = projector;
        this.projectorPanel = projectorPanel;
    }

    /** Must be called AFTER masterPanel is added to target parent AND positioned on the display*/
    public void init() {
        this.controlPanel.init();
        this.projector.init(this.projectorPanel.getPosition(), this.projectorPanel);
        ((PlayerPanelPlugin)this.masterPanel.getPlugin()).init(masterPanel.getPosition());
    }
}