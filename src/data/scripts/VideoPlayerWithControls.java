package data.scripts;

import com.fs.starfarer.api.ui.CustomPanelAPI;

import data.scripts.controlpanel.PlayerControlPanel;

public class VideoPlayerWithControls {
    public final CustomPanelAPI masterPanel;
    public final PlayerControlPanel controlPanel;

    public VideoPlayerWithControls(CustomPanelAPI masterPanel, PlayerControlPanel controlPanel) {
        this.masterPanel = masterPanel;
        this.controlPanel = controlPanel;
    }
}