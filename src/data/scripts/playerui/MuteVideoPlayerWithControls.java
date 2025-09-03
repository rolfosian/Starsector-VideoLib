package data.scripts.playerui;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import data.scripts.VideoPaths;
import data.scripts.VideoModes.PlayMode;
import data.scripts.decoder.Decoder;
import data.scripts.projector.VideoProjector;
import data.scripts.util.VideoUtils;

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

    public void openNewVideo(String videoId, int width, int height) { // this is too much debug with all the control panel components, easier to just scrap the thing and make a new one, may revisit this
        // Decoder decoder = this.projector.getDecoder();
        // decoder.setVideoFilePath(VideoPaths.get(videoId));
        // decoder.setWidth(width);
        // decoder.setHeight(height);
        // decoder.restart();

        // this.projectorPanel.getPosition().setSize(width, height);
        // this.projector.setWidth(width);
        // this.projector.setHeight(height);
        
        // this.masterPanel.getPosition().setSize(width, this.controlPanel.setSize(width, height) + height + 5f);

        // this.projector.setCurrentTextureId(this.projector.getDecoder().getCurrentVideoTextureId());
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

    @Override
    public Decoder getDecoder() {
        return this.projector.getDecoder();
    }

    public PlayerControlPanel getControls() {
        return this.controlPanel;
    }

    public CustomPanelAPI getMasterPanel() {
        return this.masterPanel;
    }

    public float getWidth() {
        return masterPanel.getPosition().getWidth();
    }

    public float getHeight() {
        return masterPanel.getPosition().getHeight();
    }
}