package data.scripts.player_ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.VideoMode;
import data.scripts.projector.VideoProjector;

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

    private ButtonAPI playButton;
    private ButtonAPI pauseButton;
    private ButtonAPI stopButton;

    private Color seekButtonColor = Misc.getDarkPlayerColor();
    private float seekLineRed = seekButtonColor.getRed() / 255f;
    private float seekLineGreen = seekButtonColor.getGreen() / 255f;
    private float seekLineBlue = seekButtonColor.getBlue() / 255f;

    public void init() {
        playerControls.init(controlPanel.getPosition());
        seekBarPlugin.init(seekBarPanel.getPosition());
    }

    public PlayerControlPanel(VideoProjector projector, int width, int height, boolean withSound) {
        this.projector = projector;
        this.playerControls = new PlayerControls();
        this.controlPanel = Global.getSettings().createCustom(width, height, playerControls);

        this.seekBarPlugin = new SeekBarPlugin();
        this.seekBarPanel = Global.getSettings().createCustom(width - 10, height / 3, seekBarPlugin);
        
        controlPanel.addComponent(seekBarPanel).inMid().setYAlignOffset(-(height / 2));

        CustomPanelAPI playPauseStopPanel = Global.getSettings().createCustom(width, height - height / 3 - 10f, new BaseCustomUIPanelPlugin() {
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
        TooltipMakerAPI buttonHolder = playPauseStopPanel.createUIElement(width, height - height / 3 - 10f, false);

        playButton = buttonHolder.addButton("Play", "PLAY", 30f, 30f, 5f);
        pauseButton = buttonHolder.addButton("Pause", "PAUSE", 30f, 30f, 0f);
        stopButton = buttonHolder.addButton("Stop", "STOP", 30f, 30f, 0f);

        playPauseStopPanel.addUIElement(buttonHolder);
        pauseButton.getPosition().rightOfMid(playButton, 5f);
        stopButton.getPosition().rightOfMid(pauseButton, 5f);

        controlPanel.addComponent(playPauseStopPanel).inTL(0f, controlPanel.getPosition().getHeight());
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

    // TODO Needs polish
    private class SeekBarPlugin extends BaseCustomUIPanelPlugin {
        private double durationSeconds; // seconds in 0.000 format
        private long durationUs;
        private long currentVideoPts; // µs

        private int timeAccumulator = 0;
        private static final int SEEK_APPLY_THRESHOLD = 10;

        private long pendingSeekTarget = -1;
        private long oldSeekTarget = -1;

        public boolean seeking = false;
        private boolean isAdvanced = false;
        private boolean hasSeeked = false;

        private float seekX;

        private VideoMode oldProjectorMode;
        private VideoMode oldDecoderMode;
        boolean wasPaused;

        private float seekButtonOffset;
        private float seekButtonY;

        private PositionAPI seekBarPanelPosition;
        private float seekBarPanelX;
        private float seekBarPanelY;
        private float seekPanelWidth;
        private float seekPanelHeight;

        private float seekBarPanelLeftBound;
        private float seekBarPanelRightBound;
        private float seekbarPanelYBoundTolerance = 25f;

        private float seekLineY; // for in rendering method - mid of panelY
        
        // Transition zone boundaries for smooth seek button positioning
        private float transitionStart;
        private float transitionEnd;
        
        private float adjustedLeftBound;
        private float adjustedRightBound;

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

            this.hasSeeked = true;
            this.oldSeekTarget = this.pendingSeekTarget;
            this.timeAccumulator = 0;
            this.currentVideoPts = this.pendingSeekTarget;
        }

        @Override
        public void advance(float deltaTime) {
            this.currentVideoPts = projector.getDecoder().getCurrentVideoPts();

            if (this.seeking) {
                this.seekX = Math.max(seekBarPanelLeftBound, Math.min(this.seekX, this.seekBarPanelRightBound));
                this.pendingSeekTarget = getSeekPositionFromX(this.seekX);
                
                float newX = getButtonXFromSeekPosition(this.pendingSeekTarget);
                seekButton.getPosition().inTL(newX, this.seekButtonY);
                
                timeAccumulator++;
                if (timeAccumulator >= SEEK_APPLY_THRESHOLD) {
                    if (!(this.oldSeekTarget == this.pendingSeekTarget)) {
                        this.seek();
                        projector.setCurrentTextureId(projector.getDecoder().getCurrentVideoTextureId());
                    }
                }
        
            } else {
                if (this.pendingSeekTarget >= 0 && timeAccumulator >= SEEK_APPLY_THRESHOLD) {
                    if (!(this.oldSeekTarget == this.pendingSeekTarget)) {
                        projector.getDecoder().setMode(VideoMode.SEEKING);
                        projector.getDecoder().seek(this.pendingSeekTarget);
                        projector.setMode(VideoMode.SEEKING);
                        this.oldSeekTarget = this.pendingSeekTarget;
                    }

                    this.pendingSeekTarget = -1;
                    this.timeAccumulator = 0;
                    this.currentVideoPts = pendingSeekTarget;
                }
                
                if (!projector.paused()) {
                    float newX = getButtonXFromSeekPosition(this.currentVideoPts);
                    seekButton.getPosition().inTL(newX, this.seekButtonY);
                }
            }
            
            if (!this.isAdvanced) this.isAdvanced = true;
        }

        @Override
        public void processInput(List<InputEventAPI> events) {
            for (int i = 0; i < events.size(); i++) {
                InputEventAPI event = events.get(i);

                if (event.isMouseEvent()) {

                    if (event.isMouseDownEvent() && !this.seeking && isInSeekLineBounds(event.getX(), event.getY())) {
                        this.seeking = true;

                        this.oldProjectorMode = projector.getMode();
                        this.oldDecoderMode = projector.getDecoder().getMode();

                        this.wasPaused = projector.paused();
                        projector.pause();
                        projector.setMode(VideoMode.SEEKING);
                        projector.getDecoder().setMode(VideoMode.SEEKING);
                        seekButton.setEnabled(false);

                        this.seekX = event.getX();
                        this.pendingSeekTarget = getSeekPositionFromX(this.seekX);
                
                        float newX = getButtonXFromSeekPosition(this.pendingSeekTarget);
                        seekButton.getPosition().inTL(newX, this.seekButtonY);

                        this.seek();

                        event.consume();
                        this.isAdvanced = false;
                        continue;
                    }

                    if (this.seeking && this.isAdvanced && event.isMouseUpEvent()) {
                        this.seeking = false;

                        projector.setMode(oldProjectorMode);
                        projector.getDecoder().setMode(oldDecoderMode);
                        seekButton.setEnabled(true);
                        if (!this.wasPaused) projector.unpause();

                        event.consume();
                        continue;
                    }

                    if (seeking) {
                        this.seekX = event.getX();
                        event.consume();
                    }
                }
            }
        }

        @Override
        public void positionChanged(PositionAPI seekBarPanelPos) {
            this.seekBarPanelX = seekBarPanelPos.getX();
            this.seekBarPanelY = seekBarPanelPos.getY();
        
            this.seekPanelWidth = seekBarPanelPos.getWidth();
            this.seekPanelHeight = seekBarPanelPos.getHeight();
        
            this.seekLineY = seekBarPanelPos.getCenterY();
            

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
            this.seekBarPanelPosition = seekBarPanelPos;
            this.seekBarPanelX = seekBarPanelPos.getX();
            this.seekBarPanelY = seekBarPanelPos.getY();
        
            this.seekPanelWidth = seekBarPanelPos.getWidth();
            this.seekPanelHeight = seekBarPanelPos.getHeight();
        
            this.seekLineY = seekBarPanelPos.getCenterY();
        
            this.durationSeconds = projector.getDecoder().getDurationSeconds();
            this.durationUs = projector.getDecoder().getDurationUs();
        
            seekBarTt = seekBarPanel.createUIElement(seekBarPanelPos.getWidth() - 10, seekBarPanelPos.getHeight() / 3, false);

            seekButton = seekBarTt.addButton("", null, 30f, 30f, 0f);
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
            this.timeAccumulator = 0;
        
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
