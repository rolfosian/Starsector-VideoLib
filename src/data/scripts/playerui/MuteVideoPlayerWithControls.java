package data.scripts.playerui;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import data.scripts.VideoModes.PlayMode;
import data.scripts.projector.VideoProjector;

public class MuteVideoPlayerWithControls implements VideoPlayer {
    private final CustomPanelAPI masterPanel;
    private final CustomPanelAPI projectorPanel;

    private final VideoProjector projector;
    private final PlayerControlPanel controlPanel;

    public MuteVideoPlayerWithControls(CustomPanelAPI masterPanel, PlayerControlPanel controlPanel, VideoProjector projector, CustomPanelAPI projectorPanel) {
        this.masterPanel = masterPanel;
        this.controlPanel = controlPanel;
        this.projector = projector;
        this.projectorPanel = projectorPanel;

        this.projector.setControlPanel(controlPanel);
    }

    @Override
    public PositionAPI addTo(UIPanelAPI parent) {
        return parent.addComponent(this.masterPanel);
    }

    /** Must be called after masterPanel is added to target parent AND positioned*/
    @Override
    public void init() {
        this.controlPanel.init();
        this.projector.init(this.projectorPanel.getPosition(), this.projectorPanel);
        ((PlayerPanelPlugin)this.masterPanel.getPlugin()).init(masterPanel.getPosition());

        if (projector.getPlayMode() == PlayMode.PLAYING) controlPanel.play();
    }

    @Override
    public void setClickToPause(boolean clickToPause) {
        this.projector.setClickToPause(clickToPause);
    }

    @Override
    public CustomPanelAPI getProjectorPanel() {
        return this.projectorPanel;
    }

    @Override
    public VideoProjector getProjector() {
        return this.projector;
    }

    public PlayerControlPanel getControls() {
        return this.controlPanel;
    }

    public CustomPanelAPI getMasterPanel() {
        return this.masterPanel;
    }
}