package data.scripts.playerui;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import data.scripts.VideoPaths;
import data.scripts.decoder.Decoder;
import data.scripts.projector.VideoProjector;

public class MuteVideoPlayer implements VideoPlayer {
    private final CustomPanelAPI projectorPanel;
    private final VideoProjector projector;

    public MuteVideoPlayer(CustomPanelAPI playerPanel, VideoProjector projector) {
        this.projectorPanel = playerPanel;
        this.projector = projector;
    }

    public PositionAPI addTo(UIPanelAPI parent) {
        return parent.addComponent(projectorPanel);
    }

    /** Must be called after masterPanel is added to target parent AND positioned*/
    public void init() {
        this.projector.init(projectorPanel.getPosition(), projectorPanel);
    }

    public void openNewVideo(String videoId, int width, int height) {
        String videoFilePath = VideoPaths.get(videoId);

        Decoder decoder = this.projector.getDecoder();
        decoder.setVideoFilePath(videoFilePath);
        decoder.setWidth(width);
        decoder.setHeight(height);
        decoder.restart();

        this.projectorPanel.getPosition().setSize(width, height);
        this.projector.setVideoFilePath(videoFilePath);
        this.projector.setWidth(width);
        this.projector.setHeight(height);
        if (this.projector.paused()) this.projector.setCurrentTextureId(this.projector.getDecoder().getCurrentVideoTextureId());
    }

    @Override
    public CustomPanelAPI getMasterPanel() {
        return this.projectorPanel;
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

    public void setClickToPause(boolean clickToPause) {
        this.projector.setClickToPause(clickToPause);
    }

    public float getWidth() {
        return (int) projectorPanel.getPosition().getWidth();
    }

    public float getHeight() {
        return projectorPanel.getPosition().getHeight();
    }
}
