package videolib.playerui;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import videolib.VideoModes.PlayMode;
import videolib.decoder.Decoder;
import videolib.projector.VideoProjector;
import videolib.speakers.Speakers;

public class AudioVideoPlayerWithControls implements VideoPlayer {
    private final CustomPanelAPI masterPanel;
    private final CustomPanelAPI projectorPanel;

    private final VideoProjector projector;
    private final Speakers speakers;
    private final PlayerControlPanel controlPanel;

    public AudioVideoPlayerWithControls(CustomPanelAPI masterPanel, PlayerControlPanel controlPanel, Speakers speakers, VideoProjector projector, CustomPanelAPI projectorPanel) {
        this.masterPanel = masterPanel;
        this.controlPanel = controlPanel;
        this.projector = projector;
        this.projectorPanel = projectorPanel;
        this.speakers = speakers;

        this.projector.setControlPanel(controlPanel);
    }

    @Override
    public PositionAPI addTo(UIPanelAPI parent) {
        return parent.addComponent(this.masterPanel);
    }
    
    @Override
    public void init() {
        this.controlPanel.init();
        this.projector.init(this.projectorPanel.getPosition(), this.projectorPanel);

        if (projector.getPlayMode() == PlayMode.PLAYING) controlPanel.play();
    }

    public void openNewVideo(String videoId, int width, int height) { // this is too much debug with all the control panel components, easier to just scrap the thing and make a new one, may revisit this
        // Decoder decoder = this.projector.getDecoder();
        // decoder.setVideoFilePath(VideoPaths.getVideoPath(videoId));
        // decoder.setWidth(width);
        // decoder.setHeight(height);
        // decoder.restart();

        // this.projectorPanel.getPosition().setSize(width, height);
        // this.projector.setWidth(width);
        // this.projector.setHeight(height);
        // this.projector.setIsrendering(true);
        
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
    
    @Override
    public PlayerControlPanel getControls() {
        return this.controlPanel;
    }
    
    @Override
    public CustomPanelAPI getMasterPanel() {
        return this.masterPanel;
    }
    
    @Override
    public float getWidth() {
        return masterPanel.getPosition().getWidth();
    }
    
    @Override
    public float getHeight() {
        return masterPanel.getPosition().getHeight();
    }
    
    @Override
    public Speakers getSpeakers() {
        return speakers;
    }

    @Override
    public void finish() {
        projector.finish();
    }
}