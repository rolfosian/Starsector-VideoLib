package data.scripts.playerui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.VideoModes.PlayMode;
import data.scripts.projector.VideoProjector;
import data.scripts.util.VideoUtils;

import java.awt.Color;
import java.util.*;

import org.lwjgl.opengl.GL11;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class PlayerControlPanel {
    private static final Logger logger = Logger.getLogger(PlayerControlPanel.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    private static final float BTN_SIZE = 30f;
    private static final float pseudoSeekTtOffset = BTN_SIZE / 2;

    private PlayerControlPanel self = this;
    private final VideoProjector projector;
    private final CustomPanelAPI controlPanel;
    private final PlayerControls playerControls;

    private CustomPanelAPI seekBarPanel;
    private SeekBarPlugin seekBarPlugin;
    private TooltipMakerAPI seekBarTt;
    private ButtonAPI seekButton;

    // private CustomPanelAPI volumePanel;
    // private VolumePlugin volumePlugin;

    CustomPanelAPI playPauseStopPanel;
    TooltipMakerAPI playPauseStopHolder;
    private ButtonAPI playButton;
    private ButtonAPI pauseButton;
    private ButtonAPI stopButton;

    private Color textColor;
    private Color bgButtonColor;
    private float seekLineRed;
    private float seekLineGreen;
    private float seekLineBlue;

    public void init() {
        playerControls.init(controlPanel.getPosition());
        seekBarPlugin.init(seekBarPanel.getPosition());
    }

    public PlayerControlPanel(VideoProjector projector, int width, int height, boolean withSound, Color textColor, Color buttonBgColor) {
        this.projector = projector;
        this.playerControls = new PlayerControls();
        this.controlPanel = Global.getSettings().createCustom(width, height, this.playerControls);

        this.textColor = textColor;
        this.bgButtonColor = buttonBgColor;
        this.seekLineRed = buttonBgColor.getRed() / 255f;
        this.seekLineGreen = buttonBgColor.getGreen() / 255f;
        this.seekLineBlue = buttonBgColor.getBlue() / 255f;

        this.playPauseStopPanel = Global.getSettings().createCustom(width, BTN_SIZE, new BaseCustomUIPanelPlugin() {
            @Override
            public void render(float alphaMult) {}

            @Override
            public void buttonPressed(Object buttonId) {
                switch((String)buttonId) {
                    case "PLAY":
                        play();
                        return;

                    case "PAUSE":
                        pause();
                        return;

                    case "STOP":
                        stop();
                        return;

                    default:
                        return;
                }
            }
        });
        this.playPauseStopHolder = this.playPauseStopPanel.createUIElement(width, BTN_SIZE, false);

        this.playButton = this.playPauseStopHolder.addButton("Play", "PLAY", textColor, buttonBgColor, BTN_SIZE, BTN_SIZE, 5f);
        this.pauseButton = this.playPauseStopHolder.addButton("Pause", "PAUSE", textColor, buttonBgColor, BTN_SIZE, BTN_SIZE, 0f);
        this.stopButton = this.playPauseStopHolder.addButton("Stop", "STOP", textColor, buttonBgColor, BTN_SIZE, BTN_SIZE, 0f);

        this.playPauseStopPanel.addUIElement(playPauseStopHolder);
        this.pauseButton.getPosition().rightOfMid(this.playButton, 5f);
        this.stopButton.getPosition().rightOfMid(this.pauseButton, 5f);

        if (projector.getPlayMode() == PlayMode.PAUSED) this.pauseButton.setEnabled(false);
        else if (projector.getPlayMode() == PlayMode.PLAYING) this.playButton.setEnabled(false);

        this.controlPanel.addComponent(this.playPauseStopPanel).inTL(0f, 0f);

        this.seekBarPlugin = new SeekBarPlugin();
        this.seekBarPanel = Global.getSettings().createCustom(width - 10, BTN_SIZE, this.seekBarPlugin);
        
        this.controlPanel.addComponent(seekBarPanel).inTL(10f, 0f);

        // if (withSound) {

        // }
    }

    public PlayerControlPanel(VideoProjector projector, int width, int height, boolean withSound) {
        this.projector = projector;
        this.playerControls = new PlayerControls();
        this.controlPanel = Global.getSettings().createCustom(width, height, this.playerControls);

        this.textColor = Misc.getTextColor();
        this.bgButtonColor = Misc.getDarkPlayerColor();
        this.seekLineRed = bgButtonColor.getRed() / 255f;
        this.seekLineGreen = bgButtonColor.getGreen() / 255f;
        this.seekLineBlue = bgButtonColor.getBlue() / 255f;

        this.playPauseStopPanel = Global.getSettings().createCustom(width, BTN_SIZE, new BaseCustomUIPanelPlugin() {
            @Override
            public void render(float alphaMult) {}

            @Override
            public void buttonPressed(Object buttonId) {
                switch((String)buttonId) {
                    case "PLAY":
                        play();
                        return;

                    case "PAUSE":
                        pause();
                        return;

                    case "STOP":
                        stop();
                        return;

                    default:
                        return;
                }
            }
        });
        this.playPauseStopHolder = this.playPauseStopPanel.createUIElement(width, BTN_SIZE, false);

        this.playButton = this.playPauseStopHolder.addButton("Play", "PLAY", textColor, bgButtonColor, BTN_SIZE, BTN_SIZE, 5f);
        this.pauseButton = this.playPauseStopHolder.addButton("Pause", "PAUSE", textColor, bgButtonColor, BTN_SIZE, BTN_SIZE, 0f);
        this.stopButton = this.playPauseStopHolder.addButton("Stop", "STOP", textColor, bgButtonColor, BTN_SIZE, BTN_SIZE, 0f);

        this.playPauseStopPanel.addUIElement(playPauseStopHolder);
        this.pauseButton.getPosition().rightOfMid(this.playButton, 5f);
        this.stopButton.getPosition().rightOfMid(this.pauseButton, 5f);

        if (projector.getPlayMode() == PlayMode.PAUSED) this.pauseButton.setEnabled(false);
        else if (projector.getPlayMode() == PlayMode.PLAYING) this.playButton.setEnabled(false);

        this.controlPanel.addComponent(this.playPauseStopPanel).inTL(0f, 0f);

        this.seekBarPlugin = new SeekBarPlugin();
        this.seekBarPanel = Global.getSettings().createCustom(width - 10, BTN_SIZE, this.seekBarPlugin);
        
        this.controlPanel.addComponent(seekBarPanel).inTL(10f, 0f);

        // if (withSound) {

        // }
    }

    public void play() {
        playButton.setEnabled(false);
        pauseButton.setEnabled(true);
        stopButton.setEnabled(true);

        self.projector.play();
    }
    
    public void pause() {
        playButton.setEnabled(true);
        pauseButton.setEnabled(false);

        self.projector.pause();
    }

    public void stop() {
        stopButton.setEnabled(false);
        pauseButton.setEnabled(false);
        playButton.setEnabled(true);
        
        seekBarPlugin.reset();
        self.projector.stop();
    }

    public CustomPanelAPI getSeekBarPanel() {
        return this.seekBarPanel;
    }

    public CustomPanelAPI getControlPanel() {
        return this.controlPanel;
    }

    private class PlayerControls extends BaseCustomUIPanelPlugin {
        private float width;
        private float height;
        private float x;
        private float y;

        public void init(PositionAPI controlsPos) {
            this.width = controlsPos.getWidth();
            this.height = controlsPos.getHeight();
            this.x = controlsPos.getX();
            this.y = controlsPos.getY();
        }

        @Override
        public void positionChanged(PositionAPI controlsPos) {
            this.width = controlsPos.getWidth();
            this.height = controlsPos.getHeight();
            this.x = controlsPos.getX();
            this.y = controlsPos.getY();
        }
    
        @Override
        public void processInput(List<InputEventAPI> events) {
            for (int i = 0; i < events.size(); i++) {
                InputEventAPI event = events.get(i);
    
                if (!seekBarPlugin.seeking && event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_SPACE) {
                    if (projector.paused()) play();
                    else pause();
                    break;
                }
            }
        }
    
        @Override
        public void render(float alphaMult) {
            // GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            // GL11.glDisable(GL11.GL_TEXTURE_2D);
            // GL11.glEnable(GL11.GL_BLEND);
            // GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
            // GL11.glColor4f(0f, 0f, 0f, alphaMult);
        
            // GL11.glBegin(GL11.GL_QUADS);
            // GL11.glVertex2f(this.x, this.y);
            // GL11.glVertex2f(this.x + this.width, this.y);
            // GL11.glVertex2f(this.x + this.width, this.y + this.height);
            // GL11.glVertex2f(this.x, this.y + this.height);
            // GL11.glEnd();
        
            // GL11.glDisable(GL11.GL_BLEND);
            // GL11.glEnable(GL11.GL_TEXTURE_2D);
            // GL11.glPopAttrib();
        }
    }

    public void setSeekBarLineColor(Color color) {
        this.seekLineRed = color.getRed() / 255f;
        this.seekLineGreen = color.getGreen() / 255f;
        this.seekLineBlue = color.getBlue() / 255f;
    }

    private class SeekBarPlugin extends BaseCustomUIPanelPlugin {
        private double durationSeconds; // seconds in 0.000 format
        private long durationUs;
        private long currentVideoPts; // µs

        private PlayMode oldProjectorMode;
        private PlayMode oldDecoderMode;
        boolean wasPaused;

        private int seekAccumulator = 0;
        private static final int SEEK_APPLY_THRESHOLD = 15;

        private long pendingSeekTarget = -1;
        private long oldSeekTarget = -1;
        private float seekX;

        public boolean seeking = false;
        private boolean isAdvanced = false;
        private boolean hasSeeked = false;

        private float seekButtonOffset;
        private float seekButtonY;

        private float seekPanelWidth;
        private float seekBarPanelLeftBound;
        private float seekBarPanelRightBound;
        private float seekbarPanelYBoundTolerance = 25f;

        private float seekLineY;
        
        // Transition zone boundaries for smooth seek button positioning
        private float transitionStart;
        private float transitionEnd;
        
        private float adjustedLeftBound;
        private float adjustedRightBound;

        private LabelAPI seekTimePseudoTt = Global.getSettings().createLabel("00:00:00", Fonts.ORBITRON_16);
        private PositionAPI seekTimeTtPos;
        private float seekTimeTtXOffset;

        private LabelAPI currentTimeLabel = Global.getSettings().createLabel("00:00 / 00:00", Fonts.ORBITRON_16);
        private float currentTimeLabelWidth;
        private String durationString;

        public SeekBarPlugin() {
            this.seekTimePseudoTt.setColor(textColor);
            this.currentTimeLabel.setColor(textColor);
        }
        
        private void setSeekBarPanelBounds(PositionAPI panelPos) {
            this.seekBarPanelLeftBound = panelPos.getCenterX() - panelPos.getWidth() / 2;
            this.seekBarPanelRightBound = panelPos.getCenterX() + panelPos.getWidth() / 2;
            
            if (seekButton != null) {
                this.adjustedLeftBound = this.seekBarPanelLeftBound - this.seekButtonOffset;
                this.adjustedRightBound = this.seekBarPanelRightBound + this.seekButtonOffset;
            }
        }

        private long getSeekPositionFromX(float mouseX) {
            float relativeX = mouseX - this.seekBarPanelLeftBound;
            float clampedX = Math.max(0f, Math.min(relativeX, this.seekPanelWidth));
            double fraction = clampedX / this.seekPanelWidth;
            return (long) (fraction * this.durationUs);
        }

        private float getButtonXFromSeekPosition(long pts) {
            double fraction = pts / (double)(this.durationUs);
            float newX = (float) (fraction * this.seekPanelWidth);
        
            if (newX <= this.transitionStart) {
                return newX - seekButtonOffset;

            } else if (newX >= this.transitionEnd) {
                return Math.min(newX, this.seekPanelWidth - seekButtonOffset);

            } else {
                float transitionProgress = (newX - this.transitionStart) / (this.transitionEnd - this.transitionStart);
                float offsetAmount = seekButtonOffset * (1.0f - transitionProgress);
                return newX - offsetAmount;
            }
        }

        private boolean isInSeekLineBounds(float mouseX, float mouseY) {
            return mouseX >= this.adjustedLeftBound && mouseX <= this.adjustedRightBound &&
                   mouseY >= (this.seekLineY - seekbarPanelYBoundTolerance) && mouseY <= (this.seekLineY + seekbarPanelYBoundTolerance);
        }

        private void seek() {
            projector.getDecoder().seek(pendingSeekTarget);
            currentTimeLabel.setText(String.format("%s / %s", VideoUtils.formatTimeNoDecimals(pendingSeekTarget), durationString));
            this.timeAccumulator = 0;

            this.hasSeeked = true;
            this.oldSeekTarget = this.pendingSeekTarget;
            this.seekAccumulator = 0;
            this.currentVideoPts = this.pendingSeekTarget;
        }

        private float timeAccumulator = 0;
        @Override
        public void advance(float deltaTime) {
            timeAccumulator += deltaTime;
            this.currentVideoPts = projector.getDecoder().getCurrentVideoPts();

            if (timeAccumulator >= 0.25) {
                currentTimeLabel.setText(String.format("%s / %s", VideoUtils.formatTimeNoDecimals(currentVideoPts), durationString));
                timeAccumulator = 0;
            }

            if (this.seeking) {
                this.seekX = Math.max(seekBarPanelLeftBound, Math.min(this.seekX, this.seekBarPanelRightBound));
                this.pendingSeekTarget = getSeekPositionFromX(this.seekX);
                
                float newX = getButtonXFromSeekPosition(this.pendingSeekTarget);
                seekButton.getPosition().inTL(newX, this.seekButtonY);
                
                seekAccumulator++;
                if (seekAccumulator >= SEEK_APPLY_THRESHOLD) {
                    if (!(this.oldSeekTarget == this.pendingSeekTarget)) {
                        this.seek();
                        projector.setCurrentTextureId(projector.getDecoder().getCurrentVideoTextureId());
                    }
                }
        
            } else {
                if (this.pendingSeekTarget >= 0 && seekAccumulator >= SEEK_APPLY_THRESHOLD) {
                    if (!(this.oldSeekTarget == this.pendingSeekTarget)) {
                        projector.getDecoder().setPlayMode(PlayMode.SEEKING);
                        projector.getDecoder().seek(this.pendingSeekTarget);
                        projector.setPlayMode(PlayMode.SEEKING);
                        this.oldSeekTarget = this.pendingSeekTarget;
                    }

                    this.pendingSeekTarget = -1;
                    this.seekAccumulator = 0;
                    this.currentVideoPts = pendingSeekTarget;
                }
                
                if (!projector.paused()) {
                    float newX = getButtonXFromSeekPosition(this.currentVideoPts);
                    seekButton.getPosition().inTL(newX, this.seekButtonY);
                }
            }
            
            if (!this.isAdvanced) this.isAdvanced = true;
        }

        private float peekPos;
        @Override 
        public void processInput(List<InputEventAPI> events) {
            for (int i = 0; i < events.size(); i++) {
                InputEventAPI event = events.get(i);

                if (!event.isConsumed() && event.isMouseEvent()) {
                    float mouseX = event.getX();
                    float mouseY = event.getY();

                    if (isInSeekLineBounds(mouseX, mouseY)) {

                        if (event.isMouseMoveEvent() && peekPos != mouseX) {
                            peekPos = mouseX;
                            long seekPos = getSeekPositionFromX(peekPos);
                            float toX = getButtonXFromSeekPosition(seekPos);

                            seekTimePseudoTt.setText(VideoUtils.formatTime(seekPos));
                            seekTimePseudoTt.setOpacity(1);
                            seekTimeTtPos.inTL(toX - seekTimeTtXOffset, this.seekButtonY - pseudoSeekTtOffset);
                        }

                        if (event.isMouseDownEvent() && !this.seeking) {
                            this.seeking = true;
    
                            this.oldProjectorMode = projector.getPlayMode();
                            this.oldDecoderMode = projector.getDecoder().getPlayMode();
    
                            this.wasPaused = projector.paused();
                            projector.pause();
                            projector.setPlayMode(PlayMode.SEEKING);
                            projector.getDecoder().setPlayMode(PlayMode.SEEKING);
                            seekButton.setEnabled(false);
                            stopButton.setEnabled(true);
    
                            this.seekX = mouseX;
                            this.pendingSeekTarget = getSeekPositionFromX(this.seekX);
                    
                            float newX = getButtonXFromSeekPosition(this.pendingSeekTarget);
                            seekButton.getPosition().inTL(newX, this.seekButtonY);
    
                            this.seek();
    
                            event.consume();
                            this.isAdvanced = false;
                            continue;
                        }
                    } else {
                        seekTimePseudoTt.setOpacity(0);
                    }

                    if (this.seeking && this.isAdvanced && event.isMouseUpEvent()) {
                        this.seeking = false;

                        projector.setPlayMode(oldProjectorMode);
                        projector.getDecoder().setPlayMode(oldDecoderMode);
                        seekButton.setEnabled(true);
                        if (!this.wasPaused) projector.unpause();

                        event.consume();
                        continue;
                    }

                    if (seeking) {
                        this.seekX = mouseX;
                        event.consume();
                    }
                }
            }
        }

        @Override
        public void positionChanged(PositionAPI seekBarPanelPos) {
            this.seekPanelWidth = seekBarPanelPos.getWidth();
            this.seekLineY = seekBarPanelPos.getY() + seekBarPanelPos.getHeight();

            if (seekButton != null) {
                this.seekButtonY = -seekButton.getPosition().getHeight() / 2; // relative to panel top
                this.seekButtonOffset = seekButton.getPosition().getWidth() / 2;
                
                this.transitionStart = seekBarPanelPos.getWidth() - 60;
                this.transitionEnd = seekBarPanelPos.getWidth() - 30;
                
                this.adjustedLeftBound = this.seekBarPanelLeftBound - this.seekButtonOffset;
                this.adjustedRightBound = this.seekBarPanelRightBound + this.seekButtonOffset;
            }

            setSeekBarPanelBounds(seekBarPanelPos);
        }

        public void init(PositionAPI seekBarPanelPos) {
            this.seekPanelWidth = seekBarPanelPos.getWidth();
            this.seekLineY = seekBarPanelPos.getY() + seekBarPanelPos.getHeight();
        
            this.durationSeconds = projector.getDecoder().getDurationSeconds();
            this.durationUs = projector.getDecoder().getDurationUs();
        
            seekBarTt = seekBarPanel.createUIElement(seekBarPanelPos.getWidth(), seekBarPanelPos.getHeight(), false);

            seekButton = seekBarTt.addButton("", null, textColor, bgButtonColor, BTN_SIZE, BTN_SIZE, 0f);
            seekButton.setClickable(false);
            seekButton.setMouseOverSound(null);
            seekButton.setButtonPressedSound(null);

            seekBarPanel.addUIElement(seekBarTt).inTL(0f, 0f);

            this.seekButtonY = -seekButton.getPosition().getHeight() / 2;
            this.seekButtonOffset = seekButton.getPosition().getWidth() / 2;

            setSeekBarPanelBounds(seekBarPanelPos);
            
            this.transitionStart = seekBarPanelPos.getWidth() - 60;
            this.transitionEnd = seekBarPanelPos.getWidth() - 30;
            
            this.adjustedLeftBound = this.seekBarPanelLeftBound - this.seekButtonOffset;
            this.adjustedRightBound = this.seekBarPanelRightBound + this.seekButtonOffset;

            this.seekTimeTtPos = seekBarPanel.addComponent((UIComponentAPI)this.seekTimePseudoTt);
            this.seekTimeTtXOffset = seekTimePseudoTt.computeTextWidth("00:00:00") / 2 / 2;
            
            this.durationString = VideoUtils.formatTimeNoDecimalsWithRound(this.durationUs);
            this.currentTimeLabelWidth = this.currentTimeLabel.computeTextWidth(String.format("%s / %s", this.durationString, this.durationString));
            playPauseStopPanel.addComponent((UIComponentAPI)this.currentTimeLabel).inTL(playPauseStopPanel.getPosition().getWidth() - this.currentTimeLabelWidth, 5f);

            currentTimeLabel.setText(String.format("%s / %s", VideoUtils.formatTimeNoDecimals(currentVideoPts), durationString));

            this.reset();
        }

        @Override
        public void renderBelow(float alphaMult) {
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        
            GL11.glColor4f(seekLineRed, seekLineGreen, seekLineBlue, alphaMult);
            GL11.glLineWidth(4f);
        
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2f(this.seekBarPanelLeftBound, this.seekLineY);
            GL11.glVertex2f(this.seekBarPanelRightBound, this.seekLineY);
            GL11.glEnd();
        
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }

        @Override
        public void render(float alphaMult) {

        }

        public void reset() {
            this.seeking = false;
            this.pendingSeekTarget = 0;
            this.currentVideoPts = 0;
            this.seekAccumulator = 0;
        
            this.seekX = getButtonXFromSeekPosition(0);
            seekButton.getPosition().inTL(seekX, this.seekButtonY);
        }
    };

    private class VolumePlugin extends BaseCustomUIPanelPlugin {
        @Override
        public void processInput(List<InputEventAPI> events) {

        }

        @Override
        public void render(float alphaMult) {

        }
    }
}
