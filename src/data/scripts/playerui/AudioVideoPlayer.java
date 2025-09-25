package data.scripts.playerui;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import data.scripts.VideoPaths;
import data.scripts.decoder.Decoder;
import data.scripts.projector.VideoProjector;
import data.scripts.speakers.Speakers;

public class AudioVideoPlayer implements VideoPlayer {
    private final CustomPanelAPI projectorPanel;
    private final VideoProjector projector;
    private final Speakers speakers;

    public AudioVideoPlayer(CustomPanelAPI playerPanel, VideoProjector projector) {
        this.projectorPanel = playerPanel;
        this.projector = projector;
        this.speakers = this.projector.getSpeakers();
    }

    @Override
    public PositionAPI addTo(UIPanelAPI parent) {
        return parent.addComponent(projectorPanel);
    }

    @Override
    public void init() {
        this.projector.init(projectorPanel.getPosition(), projectorPanel);
    }

    @Override
    public CustomPanelAPI getMasterPanel() {
        return projectorPanel;
    }

    @Override
    public CustomPanelAPI getProjectorPanel() {
        return projectorPanel;
    }

    @Override
    public VideoProjector getProjector() {
        return projector;
    }

    @Override
    public Decoder getDecoder() {
        return projector.getDecoder();
    }

    @Override
    public void setClickToPause(boolean clickToPause) {
        projector.setClickToPause(clickToPause);
    }

    @Override
    public void openNewVideo(String videoId, int width, int height) {
        String videoFilePath = VideoPaths.getVideoPath(videoId);

        Decoder decoder = this.projector.getDecoder();
        decoder.setVideoFilePath(videoFilePath);
        decoder.setWidth(width);
        decoder.setHeight(height);
        decoder.restart(0);

        this.projectorPanel.getPosition().setSize(width, height);
        this.projector.setVideoFilePath(videoFilePath);
        this.projector.setWidth(width);
        this.projector.setHeight(height);
        this.projector.setIsRendering(true);

        if (this.projector.paused()) {
            this.projector.setCurrentTextureId(this.projector.getDecoder().getCurrentVideoTextureId());
            
        } 
    }

    @Override
    public float getWidth() {
        return projector.getWidth();
    }

    @Override
    public float getHeight() {
        return projector.getHeight();
    }
    @Override
    public Speakers getSpeakers() {
        return speakers;
    }

    @Override
    public PlayerControlPanel getControls() {
        return null;
    }
}
