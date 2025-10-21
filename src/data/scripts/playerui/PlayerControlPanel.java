package data.scripts.playerui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.VideoModes.EOFMode;
import data.scripts.VideoModes.PlayMode;
import data.scripts.decoder.Decoder;
import data.scripts.projector.VideoProjector;
import data.scripts.speakers.Speakers;
import data.scripts.util.VideoUtils;

import java.awt.Color;
import java.util.*;

import org.lwjgl.opengl.GL11;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;

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
    private final Decoder decoder;

    private CustomPanelAPI seekBarPanel;
    private SeekBarPlugin seekBarPlugin;
    private TooltipMakerAPI seekBarTt;
    private ButtonAPI seekButton;

    private Speakers speakers;
    private CustomPanelAPI volumePanel;
    private VolumePlugin volumePlugin;

    private CustomPanelAPI playPauseStopPanel;
    private TooltipMakerAPI playPauseStopHolder;
    private ButtonAPI playButton;
    private ButtonAPI pauseButton;
    private ButtonAPI stopButton;
    private ButtonAPI loopButton;
    private PlayBackButtonOverlay[] playbackButtonOverlays = new PlayBackButtonOverlay[3];

    private Color textColor;
    private Color bgButtonColor;
    private float seekLineRed;
    private float seekLineGreen;
    private float seekLineBlue;

    public void init() {
        this.controlPanel.addComponent(seekBarPanel).inTL(5f, 0f);
        this.controlPanel.addComponent(this.playPauseStopPanel).inTL(0, 0f);
        this.playPauseStopPanel.addUIElement(playPauseStopHolder).inTL(0f, 25f); // MAGICAL NUMBERS LMAO ME GRUG ME TRIAL AND ERROR
        for (PlayBackButtonOverlay overlay : this.playbackButtonOverlays) overlay.init();
        this.seekBarPlugin.init(seekBarPanel.getPosition());
        
        if (volumePanel != null) {
            this.playPauseStopPanel.addComponent(volumePanel).inTL(4f * 30f + 60f, 0f).setYAlignOffset(-40f);
            this.volumePlugin.init(volumePanel.getPosition());
        }
    }

    public PlayerControlPanel(VideoProjector projector, int width, int height, Speakers speakers, Color textColor, Color buttonBgColor) {
        this.projector = projector;
        this.decoder = projector.getDecoder();
        this.playerControls = new PlayerControls();
        this.controlPanel = Global.getSettings().createCustom(width, height, this.playerControls);

        this.textColor = textColor;
        this.bgButtonColor = buttonBgColor;
        this.seekLineRed = buttonBgColor.getRed() / 255f;
        this.seekLineGreen = buttonBgColor.getGreen() / 255f;
        this.seekLineBlue = buttonBgColor.getBlue() / 255f;

        this.playPauseStopPanel = Global.getSettings().createCustom(width, BTN_SIZE, new BaseCustomUIPanelPlugin() {
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

                    case "LOOP":
                        toggleLoop();

                    default:
                        return;
                }
            }
        });
        this.playPauseStopHolder = this.playPauseStopPanel.createUIElement(width, BTN_SIZE, false);

        this.playButton = this.playPauseStopHolder.addButton("", "PLAY", textColor, buttonBgColor, Alignment.MID, CutStyle.NONE, BTN_SIZE, BTN_SIZE, 0f);
        this.playbackButtonOverlays[0] = new PlayBackButtonOverlay("PLAY");

        CustomPanelAPI playOverlay = Global.getSettings().createCustom(BTN_SIZE, BTN_SIZE, this.playbackButtonOverlays[0]);
        this.playbackButtonOverlays[0].setPanel(playOverlay);
        this.playPauseStopHolder.addCustom(playOverlay, 0f);
        playOverlay.getPosition().inTL(playButton.getPosition().getX(), 0);

        this.pauseButton = this.playPauseStopHolder.addButton("", "PAUSE", textColor, buttonBgColor, Alignment.MID, CutStyle.NONE, BTN_SIZE, BTN_SIZE, 0f);
        this.playbackButtonOverlays[1] = new PlayBackButtonOverlay("PAUSE");

        CustomPanelAPI pauseOverlay = Global.getSettings().createCustom(BTN_SIZE, BTN_SIZE, this.playbackButtonOverlays[1]);
        this.playbackButtonOverlays[1].setPanel(pauseOverlay);
        this.playPauseStopHolder.addCustom(pauseOverlay, 0f);

        this.stopButton = this.playPauseStopHolder.addButton("", "STOP", textColor, buttonBgColor, Alignment.MID, CutStyle.NONE, BTN_SIZE, BTN_SIZE, 0f);
        this.playbackButtonOverlays[2] = new PlayBackButtonOverlay("STOP");

        CustomPanelAPI stopOverlay = Global.getSettings().createCustom(BTN_SIZE, BTN_SIZE, this.playbackButtonOverlays[2]);
        this.playbackButtonOverlays[2].setPanel(stopOverlay);
        this.playPauseStopHolder.addCustom(stopOverlay, 0f);

        this.loopButton = this.playPauseStopHolder.addAreaCheckbox("Loop", "LOOP", textColor, buttonBgColor, Misc.getBrightPlayerColor(), BTN_SIZE + 8f, BTN_SIZE, 0f);
        this.loopButton.setChecked(this.decoder.getEOFMode() == EOFMode.LOOP);

        this.pauseButton.getPosition().rightOfMid(this.playButton, 5f);
        pauseOverlay.getPosition().rightOfMid(this.playButton, 5f);

        this.stopButton.getPosition().rightOfMid(this.pauseButton, 5f);
        stopOverlay.getPosition().rightOfMid(this.pauseButton, 5f);

        this.loopButton.getPosition().rightOfMid(this.stopButton, 10f);

        if (projector.getPlayMode() == PlayMode.PAUSED) {
            this.pauseButton.setEnabled(false);
            playbackButtonOverlays[1].setWhite();
        } else if (projector.getPlayMode() == PlayMode.PLAYING) {
            this.playButton.setEnabled(false);
            playbackButtonOverlays[1].setBlack();
        }

        this.seekBarPlugin = new SeekBarPlugin();
        this.seekBarPanel = Global.getSettings().createCustom(width - 10, BTN_SIZE, this.seekBarPlugin);

        if (speakers != null) {
            this.speakers = speakers;
            this.volumePlugin = new VolumePlugin();
            this.volumePanel = Global.getSettings().createCustom(width / 8, BTN_SIZE, this.volumePlugin);
        }
    }

    public PlayerControlPanel(VideoProjector projector, int width, int height, Speakers speakers) {
        this.projector = projector;
        this.decoder = projector.getDecoder();
        this.playerControls = new PlayerControls();
        this.controlPanel = Global.getSettings().createCustom(width, height, this.playerControls);

        this.textColor = Misc.getTextColor();
        this.bgButtonColor = Misc.getDarkPlayerColor();
        this.seekLineRed = bgButtonColor.getRed() / 255f;
        this.seekLineGreen = bgButtonColor.getGreen() / 255f;
        this.seekLineBlue = bgButtonColor.getBlue() / 255f;

        this.playPauseStopPanel = Global.getSettings().createCustom(width, BTN_SIZE, new BaseCustomUIPanelPlugin() {
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

                    case "LOOP":
                        toggleLoop();

                    default:
                        return;
                }
            }
        });
        this.playPauseStopHolder = this.playPauseStopPanel.createUIElement(width, BTN_SIZE, false);

        this.playButton = this.playPauseStopHolder.addButton("", "PLAY", textColor, bgButtonColor, Alignment.MID, CutStyle.NONE, BTN_SIZE, BTN_SIZE, 0f);
        this.playbackButtonOverlays[0] = new PlayBackButtonOverlay("PLAY");

        CustomPanelAPI playOverlay = Global.getSettings().createCustom(BTN_SIZE, BTN_SIZE, this.playbackButtonOverlays[0]);
        this.playbackButtonOverlays[0].setPanel(playOverlay);
        this.playPauseStopHolder.addCustom(playOverlay, 0f);
        playOverlay.getPosition().inTL(playButton.getPosition().getX(), 0);

        this.pauseButton = this.playPauseStopHolder.addButton("", "PAUSE", textColor, bgButtonColor, Alignment.MID, CutStyle.NONE, BTN_SIZE, BTN_SIZE, 0f);
        this.playbackButtonOverlays[1] = new PlayBackButtonOverlay("PAUSE");

        CustomPanelAPI pauseOverlay = Global.getSettings().createCustom(BTN_SIZE, BTN_SIZE, this.playbackButtonOverlays[1]);
        this.playbackButtonOverlays[1].setPanel(pauseOverlay);
        this.playPauseStopHolder.addCustom(pauseOverlay, 0f);

        this.stopButton = this.playPauseStopHolder.addButton("", "STOP", textColor, bgButtonColor, Alignment.MID, CutStyle.NONE, BTN_SIZE, BTN_SIZE, 0f);
        this.playbackButtonOverlays[2] = new PlayBackButtonOverlay("STOP");

        CustomPanelAPI stopOverlay = Global.getSettings().createCustom(BTN_SIZE, BTN_SIZE, this.playbackButtonOverlays[2]);
        this.playbackButtonOverlays[2].setPanel(stopOverlay);
        this.playPauseStopHolder.addCustom(stopOverlay, 0f);

        this.loopButton = this.playPauseStopHolder.addAreaCheckbox("Loop", "LOOP", textColor, bgButtonColor, Misc.getBrightPlayerColor(), BTN_SIZE + 8f, BTN_SIZE, 0f);
        this.loopButton.setChecked(this.decoder.getEOFMode() == EOFMode.LOOP);

        this.pauseButton.getPosition().rightOfMid(this.playButton, 5f);
        pauseOverlay.getPosition().rightOfMid(this.playButton, 5f);

        this.stopButton.getPosition().rightOfMid(this.pauseButton, 5f);
        stopOverlay.getPosition().rightOfMid(this.pauseButton, 5f);

        this.loopButton.getPosition().rightOfMid(this.stopButton, 10f);

        if (projector.getPlayMode() == PlayMode.PAUSED) {
            this.pauseButton.setEnabled(false);
            playbackButtonOverlays[1].setWhite();
        } else if (projector.getPlayMode() == PlayMode.PLAYING) {
            this.playButton.setEnabled(false);
            playbackButtonOverlays[1].setBlack();
        }

        this.controlPanel.addComponent(this.playPauseStopPanel).inTL(0f, 0f);

        this.seekBarPlugin = new SeekBarPlugin();
        this.seekBarPanel = Global.getSettings().createCustom(width - 10, BTN_SIZE, this.seekBarPlugin);
        
        this.controlPanel.addComponent(seekBarPanel).inTL(0f, 0f);

        if (speakers != null) {
            this.speakers = speakers;
            this.volumePlugin = new VolumePlugin();
            this.volumePanel = Global.getSettings().createCustom(width / 8, BTN_SIZE, this.volumePlugin);
        }
    }

    public void play() {
        playButton.setEnabled(false);
        pauseButton.setEnabled(true);
        stopButton.setEnabled(true);

        playbackButtonOverlays[0].setBlack();
        playbackButtonOverlays[1].setWhite();
        playbackButtonOverlays[2].setWhite();

        self.projector.play();
    }
    
    public void pause() {
        playButton.setEnabled(true);
        pauseButton.setEnabled(false);

        playbackButtonOverlays[0].setWhite();
        playbackButtonOverlays[1].setBlack();
        playbackButtonOverlays[2].setWhite();

        self.projector.pause();
    }

    public void stop() {
        stopButton.setEnabled(false);
        pauseButton.setEnabled(false);
        playButton.setEnabled(true);

        playbackButtonOverlays[0].setWhite();
        playbackButtonOverlays[1].setBlack();
        playbackButtonOverlays[2].setBlack();
        
        seekBarPlugin.reset();
        self.projector.stop();
    }

    public void toggleLoop() {
        if (loopButton.isChecked()) {
            decoder.setEOFMode(EOFMode.LOOP);
            projector.setEOFMode(EOFMode.LOOP);
        } else {
            decoder.setEOFMode(EOFMode.PLAY_UNTIL_END);
            projector.setEOFMode(EOFMode.PLAY_UNTIL_END);
        }
    }

    public CustomPanelAPI getSeekBarPanel() {
        return this.seekBarPanel;
    }

    public void resetSeekBar() {
        seekBarPlugin.reset();
    }

    public CustomPanelAPI getControlPanel() {
        return this.controlPanel;
    }

    public ButtonAPI getPlayButton() {
        return this.playButton;
    }

    public ButtonAPI getPauseButton() {
        return this.pauseButton;
    }

    public ButtonAPI getStopButton() {
        return this.stopButton;
    }

    private class PlayerControls extends BaseCustomUIPanelPlugin {
    
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

        }
    }

    public void setProgressDisplay(long videoPts) {
        seekBarPlugin.setCurrentTimeLabelWithRounding(videoPts);
    }

    public void setSeekBarLineColor(Color color) {
        this.seekLineRed = color.getRed() / 255f;
        this.seekLineGreen = color.getGreen() / 255f;
        this.seekLineBlue = color.getBlue() / 255f;
    }
    
    private class PlayBackButtonOverlay extends BaseCustomUIPanelPlugin {
        private interface Draw {
            public void draw(float alphaMult);
        }

        PlayBackButtonOverlay self = this;

        private float x;
        private float y;
        private float width;
        private float height;

        private int red;
        private int green;
        private int blue;

        private CustomPanelAPI panel;

        private final String type;
        private Draw draw;

        public PlayBackButtonOverlay(String type) {
            super();
            this.type = type;

            self.red = 255;
            self.green = 255;
            self.blue = 255;
        }
            
        @Override
        public void render(float alphaMult) {
            if (draw != null) draw.draw(alphaMult);
        }

        @Override
        public void positionChanged(PositionAPI pos) {
            self.x = pos.getX();
            self.y = pos.getY();

            self.width = pos.getWidth();
            self.height = pos.getHeight();

            assignDraw();
        }

        public void init() {
            PositionAPI pos = panel.getPosition();
            self.x = pos.getX();
            self.y = pos.getY();

            self.width = pos.getWidth();
            self.height = pos.getHeight();

            assignDraw();
        }

        private void assignDraw() {
            switch(self.type) {
                case "PLAY":
                    draw = new Draw() {
                        private float centerX = self.x + self.width / 2f;
                        private float centerY = self.y + self.height / 2f;
                        private float triangleWidth = self.width * 0.6f;
                        private float triangleHeight = self.height * 0.6f;

                        private float vert1X = centerX - triangleWidth / 2f;
                        private float vert1Y = centerY - triangleHeight / 2f;

                        private float vert2X = centerX + triangleWidth / 2f;
                        private float vert2Y = centerY;

                        private float vert3X = centerX - triangleWidth / 2f;
                        private float vert3Y = centerY + triangleHeight / 2f;
                        
                        public void draw(float alphaMult) {
                            GL11.glPushMatrix();
                            GL11.glDisable(GL11.GL_TEXTURE_2D);
                        
                            GL11.glEnable(GL11.GL_BLEND);
                            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                            GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
                        
                            GL11.glColor4f(self.red, self.green, self.blue, alphaMult);
                            GL11.glBegin(GL11.GL_TRIANGLES);
                        
                            GL11.glVertex2f(this.vert1X, this.vert1Y);
                            GL11.glVertex2f(this.vert2X, this.vert2Y);
                            GL11.glVertex2f(this.vert3X, this.vert3Y);
                        
                            GL11.glEnd();
                        
                            GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
                            GL11.glDisable(GL11.GL_BLEND);
                        
                            GL11.glEnable(GL11.GL_TEXTURE_2D);
                            GL11.glColor4f(1f, 1f, 1f, 1f);
                            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                            GL11.glPopMatrix();
                        }
                    };
                    break;
                
                case "STOP":
                    draw = new Draw() {
                        private float squareSize = Math.min(self.width, self.height) * 0.6f;
                        private float centerX = self.x + self.width / 2f;
                        private float centerY = self.y + self.height / 2f;
                        private float halfSize = squareSize / 2f;
                        
                        private float v1X = centerX - halfSize;
                        private float v1Y = centerY - halfSize;
                        private float v2X = centerX + halfSize;
                        private float v2Y = centerY - halfSize;
                        private float v3X = centerX + halfSize;
                        private float v3Y = centerY + halfSize;
                        private float v4X = centerX - halfSize;
                        private float v4Y = centerY + halfSize;
                        
                        public void draw(float alphaMult) {
                            GL11.glPushMatrix();
                            GL11.glDisable(GL11.GL_TEXTURE_2D);
                            GL11.glColor4f(self.red, self.green, self.blue, alphaMult);
                            GL11.glBegin(GL11.GL_QUADS);
                            
                            GL11.glVertex2f(this.v1X, this.v1Y);
                            GL11.glVertex2f(this.v2X, this.v2Y);
                            GL11.glVertex2f(this.v3X, this.v3Y);
                            GL11.glVertex2f(this.v4X, this.v4Y);
                            
                            GL11.glEnd();
                            GL11.glEnable(GL11.GL_TEXTURE_2D);
                            GL11.glColor4f(1f, 1f, 1f, 1f);
                            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                            GL11.glPopMatrix();
                        }
                    };
                    break;
                
                case "PAUSE":
                    draw = new Draw() {
                        private float centerX = self.x + self.width / 2f;
                        private float centerY = self.y + self.height / 2f;
                        private float rectWidth = self.width * 0.2f;
                        private float rectHeight = self.height * 0.6f;
                        private float gap = self.width * 0.05f; // smaller but consistent gap between rects
                    
                        private float leftCenterX = centerX - (rectWidth / 2f + gap / 2f);
                        private float rightCenterX = centerX + (rectWidth / 2f + gap / 2f);
                    
                        private float leftRect1X = leftCenterX - rectWidth / 2f;
                        private float leftRect1Y = centerY - rectHeight / 2f;
                        private float leftRect2X = leftCenterX + rectWidth / 2f;
                        private float leftRect2Y = centerY - rectHeight / 2f;
                        private float leftRect3X = leftCenterX + rectWidth / 2f;
                        private float leftRect3Y = centerY + rectHeight / 2f;
                        private float leftRect4X = leftCenterX - rectWidth / 2f;
                        private float leftRect4Y = centerY + rectHeight / 2f;
                    
                        private float rightRect1X = rightCenterX - rectWidth / 2f;
                        private float rightRect1Y = centerY - rectHeight / 2f;
                        private float rightRect2X = rightCenterX + rectWidth / 2f;
                        private float rightRect2Y = centerY - rectHeight / 2f;
                        private float rightRect3X = rightCenterX + rectWidth / 2f;
                        private float rightRect3Y = centerY + rectHeight / 2f;
                        private float rightRect4X = rightCenterX - rectWidth / 2f;
                        private float rightRect4Y = centerY + rectHeight / 2f;
                    
                        public void draw(float alphaMult) {
                            GL11.glPushMatrix();
                            GL11.glDisable(GL11.GL_TEXTURE_2D);
                            GL11.glColor4f(self.red, self.green, self.blue, alphaMult);
                            GL11.glBegin(GL11.GL_QUADS);
                    
                            GL11.glVertex2f(leftRect1X, leftRect1Y);
                            GL11.glVertex2f(leftRect2X, leftRect2Y);
                            GL11.glVertex2f(leftRect3X, leftRect3Y);
                            GL11.glVertex2f(leftRect4X, leftRect4Y);
                    
                            GL11.glVertex2f(rightRect1X, rightRect1Y);
                            GL11.glVertex2f(rightRect2X, rightRect2Y);
                            GL11.glVertex2f(rightRect3X, rightRect3Y);
                            GL11.glVertex2f(rightRect4X, rightRect4Y);
                    
                            GL11.glEnd();
                            GL11.glEnable(GL11.GL_TEXTURE_2D);
                            GL11.glColor4f(1f, 1f, 1f, 1f);
                            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                            GL11.glDisable(GL11.GL_BLEND);
                            GL11.glPopMatrix();
                        }
                    };
                    break;
                
                case "LOOP":
                    draw = new Draw() {
                        private float centerX = self.x + self.width / 2f;
                        private float centerY = self.y + self.height / 2f;
                        private float radius = Math.min(self.width, self.height) * 0.25f;
                        private float arrowSize = radius * 0.3f;
                        private float arrowAngle = (float) (Math.PI * 1.5f);
                        private float arrowX = centerX + (float) (Math.cos(arrowAngle) * radius);
                        private float arrowY = centerY + (float) (Math.sin(arrowAngle) * radius);
                        
                        private float[] arcVerticesX = new float[33];
                        private float[] arcVerticesY = new float[33];
                        private int segments = 32;

                        private float arrow1X = arrowX;
                        private float arrow1Y = arrowY;
                        private float arrow2X = arrowX - arrowSize * 0.5f;
                        private float arrow2Y = arrowY - arrowSize;
                        private float arrow3X = arrowX - arrowSize * 0.5f;
                        private float arrow3Y = arrowY + arrowSize;
                        
                        {
                            for (int i = 0; i <= this.segments; i++) {
                                float angle = (float) (i * Math.PI * 1.5f / this.segments);
                                this.arcVerticesX[i] = centerX + (float) (Math.cos(angle) * radius);
                                this.arcVerticesY[i] = centerY + (float) (Math.sin(angle) * radius);
                            }
                        }
                        
                        public void draw(float alphaMult) {
                            GL11.glPushMatrix();
                            GL11.glDisable(GL11.GL_TEXTURE_2D);
                            GL11.glColor4f(self.red, self.green, self.blue, alphaMult);
                            
                            GL11.glBegin(GL11.GL_LINE_STRIP);
                            for (int i = 0; i <= this.segments; i++) {
                                GL11.glVertex2f(this.arcVerticesX[i], this.arcVerticesY[i]);
                            }
                            GL11.glEnd();
                            
                            GL11.glBegin(GL11.GL_TRIANGLES);
                            GL11.glVertex2f(this.arrow1X, this.arrow1Y);
                            GL11.glVertex2f(this.arrow2X, this.arrow2Y);
                            GL11.glVertex2f(this.arrow3X, this.arrow3Y);
                            GL11.glEnd();
                            GL11.glEnable(GL11.GL_TEXTURE_2D);
                            GL11.glPopMatrix();
                        }
                    };
                    break;
                
                default:
                    throw new UnsupportedOperationException("Unsupported type: " + type);
            }
        }

        public void setBlack() {
            self.red = 0;
            self.green = 0;
            self.blue = 0;
        }

        public void setWhite() {
            self.red = 255;
            self.green = 255;
            self.blue = 255;
        }

        public void setPanel(CustomPanelAPI panel) {
            self.panel = panel;
        }

        public CustomPanelAPI getPanel() {
            return self.panel;
        }
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

        public void setCurrentTimeLabelWithRounding(long videoPts) {
            currentTimeLabel.setText(String.format("%s / %s", VideoUtils.formatTimeNoDecimalsWithRound(videoPts), durationString));
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
            decoder.seek(pendingSeekTarget);
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
            this.currentVideoPts = decoder.getCurrentVideoPts();

            if (timeAccumulator >= 0.25 && !projector.paused()) {
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
                        projector.setCurrentTextureId(decoder.getCurrentVideoTextureId());
                    }
                }
        
            } else {
                if (this.pendingSeekTarget >= 0 && seekAccumulator >= SEEK_APPLY_THRESHOLD) {
                    if (!(this.oldSeekTarget == this.pendingSeekTarget)) {
                        projector.setEOFMode(EOFMode.PAUSE);
                        decoder.setPlayMode(PlayMode.SEEKING);
                        decoder.seek(this.pendingSeekTarget);
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
                            this.oldDecoderMode = decoder.getPlayMode();
    
                            this.wasPaused = projector.paused();
                            projector.pause();
                            projector.setPlayMode(PlayMode.SEEKING);
                            decoder.setPlayMode(PlayMode.SEEKING);
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

                        if (!(seekX == mouseX)) {

                            this.pendingSeekTarget = getSeekPositionFromX(mouseX);
                        
                            float newX = getButtonXFromSeekPosition(this.pendingSeekTarget);
                            seekButton.getPosition().inTL(newX, this.seekButtonY);
        
                            this.seek();
                        }

                        projector.setPlayMode(oldProjectorMode);
                        decoder.setPlayMode(oldDecoderMode);
                        if (oldDecoderMode != PlayMode.PAUSED) speakers.play();
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
        
            this.durationSeconds = decoder.getDurationSeconds();
            this.durationUs = decoder.getDurationUs();
        
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
            playPauseStopHolder.addComponent((UIComponentAPI)this.currentTimeLabel).inTR(0f, 0f);//.inTL(playPauseStopPanel.getPosition().getWidth() - this.currentTimeLabelWidth, 5f);

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
            currentTimeLabel.setText(String.format("%s / %s", VideoUtils.formatTimeNoDecimals(currentVideoPts), durationString));
        }
    };

    private class VolumePlugin extends BaseCustomUIPanelPlugin {
        private float leftBound;
        private float rightBound;

        private float volumePanelWidth;

        private float volLineY;
        private float volButtonY;
        private float volButtonOffset;

        private float volumePanelLeftBound;
        private float volumePanelRightBound;
        private float volumePanelYBoundTolerance = 15f;

        private ButtonAPI volButton;
        private TooltipMakerAPI volTt;

        private int volumeAccumulator = 0;
        private static final int VOLUME_APPLY_THRESHOLD = 5;
        private float pendingVolume = -1f;
        private float oldVolume = -1f;
        
        private boolean dragging = false;
        private float dragX;

        public VolumePlugin() {
        }

        @Override
        public void advance(float deltaTime) {
            if (this.pendingVolume >= 0 && volumeAccumulator >= VOLUME_APPLY_THRESHOLD) {
                if (!(this.oldVolume == this.pendingVolume)) {
                    speakers.setVolume(this.pendingVolume);
                    this.oldVolume = this.pendingVolume;
                }
                
                this.pendingVolume = -1f;
                this.volumeAccumulator = 0;
            }
        }

        @Override
        public void processInput(List<InputEventAPI> events) {
            for (int i = 0; i < events.size(); i++) {
                InputEventAPI event = events.get(i);

                if (!event.isConsumed() && event.isMouseEvent()) {
                    float mouseX = event.getX();
                    float mouseY = event.getY();

                    if (isInVolumeLineBounds(mouseX, mouseY)) {
                        if (event.isMouseDownEvent() && !this.dragging) {
                            this.dragging = true;
                            
                            float relativeX = mouseX - this.volumePanelLeftBound;
                            float clampedX = Math.max(0f, Math.min(relativeX, this.volumePanelWidth));
                            double fraction = clampedX / this.volumePanelWidth;
                            
                            speakers.setVolume((float) fraction);
                            
                            this.pendingVolume = (float) fraction;
                            this.volumeAccumulator++;
                            
                            this.dragX = mouseX;
                            
                            float newX = (float) (fraction * this.volumePanelWidth);
                            volButton.getPosition().inTL(newX - volButtonOffset, this.volButtonY);
                            
                            event.consume();

                        } else if (event.isMouseScrollEvent()) {
                            float currentVolume = speakers.getVolume();
                            float scrollAmount = event.getEventValue() > 0 ? 0.05f : -0.05f; // 5% per scroll step
                            float newVolume = Math.max(0f, Math.min(1f, currentVolume + scrollAmount));
                            
                            speakers.setVolume(newVolume);
                            
                            float newX = (float) (newVolume * this.volumePanelWidth);
                            volButton.getPosition().inTL(newX - volButtonOffset, this.volButtonY);
                            
                            event.consume();
                        }
                    } else {
                        if (this.dragging && event.isMouseUpEvent()) {
                            this.dragging = false;
                            event.consume();
                        }
                    }

                    if (this.dragging && event.isMouseMoveEvent()) {
                        float relativeX = mouseX - this.volumePanelLeftBound;
                        float clampedX = Math.max(0f, Math.min(relativeX, this.volumePanelWidth));
                        double fraction = clampedX / this.volumePanelWidth;
                        
                        this.pendingVolume = (float) fraction;
                        this.volumeAccumulator++;
                        
                        this.dragX = mouseX;
                        
                        float newX = (float) (fraction * this.volumePanelWidth);
                        volButton.getPosition().inTL(newX - volButtonOffset, this.volButtonY);
                        
                        event.consume();
                    }

                    if (this.dragging && event.isMouseUpEvent()) {
                        if (!(dragX == mouseX)) {
                            float relativeX = mouseX - this.volumePanelLeftBound;
                            float clampedX = Math.max(0f, Math.min(relativeX, this.volumePanelWidth));
                            double fraction = clampedX / this.volumePanelWidth;
                            
                            speakers.setVolume((float) fraction);
                            
                            float newX = (float) (fraction * this.volumePanelWidth);
                            volButton.getPosition().inTL(newX - volButtonOffset, this.volButtonY);
                        }
                        
                        this.dragging = false;
                        event.consume();
                    }
                }
            }
        }

        private boolean isInVolumeLineBounds(float mouseX, float mouseY) {
            return mouseX >= this.leftBound && mouseX <= this.rightBound &&
                   mouseY >= (this.volLineY - volumePanelYBoundTolerance) && mouseY <= (this.volLineY + volumePanelYBoundTolerance);
        }

        @Override
        public void renderBelow(float alphaMult) {
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        
            GL11.glColor4f(seekLineRed, seekLineGreen, seekLineBlue, alphaMult);
            GL11.glLineWidth(3f);
        
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2f(this.leftBound, this.volLineY);
            GL11.glVertex2f(this.rightBound, this.volLineY);
            GL11.glEnd();
        
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }

        @Override
        public void positionChanged(PositionAPI pos) {
            this.volumePanelWidth = pos.getWidth();
            this.volLineY = pos.getY() + pos.getHeight();

            if (volButton != null) {
                this.volButtonY = -volButton.getPosition().getHeight() / 2; // relative to panel top
                this.volButtonOffset = volButton.getPosition().getWidth() / 2;
                
                this.leftBound = pos.getCenterX() - pos.getWidth() / 2 - this.volButtonOffset;
                this.rightBound = pos.getCenterX() + pos.getWidth() / 2 + this.volButtonOffset;
            }

            setVolumePanelBounds(pos);
        }

        private void setVolumePanelBounds(PositionAPI panelPos) {
            this.volumePanelLeftBound = panelPos.getCenterX() - panelPos.getWidth() / 2;
            this.volumePanelRightBound = panelPos.getCenterX() + panelPos.getWidth() / 2;
            
            if (volButton != null) {
                this.leftBound = this.volumePanelLeftBound - this.volButtonOffset;
                this.rightBound = this.volumePanelRightBound + this.volButtonOffset;
            }
        }

        public void init(PositionAPI panelPos) {
            this.volumePanelWidth = panelPos.getWidth();
            this.volLineY = panelPos.getY() + panelPos.getHeight();
        
            volTt = volumePanel.createUIElement(panelPos.getWidth(), panelPos.getHeight(), false);

            volButton = volTt.addButton("", null, textColor, bgButtonColor, BTN_SIZE-5, BTN_SIZE-5, 0f);
            volButton.setClickable(false);
            volButton.setMouseOverSound(null);
            volButton.setButtonPressedSound(null);

            volumePanel.addUIElement(volTt).inTL(0f, 0f);

            this.volButtonY = -volButton.getPosition().getHeight() / 2;
            this.volButtonOffset = volButton.getPosition().getWidth() / 2;

            setVolumePanelBounds(panelPos);
            
            this.leftBound = this.volumePanelLeftBound - this.volButtonOffset;
            this.rightBound = this.volumePanelRightBound + this.volButtonOffset;

            if (speakers != null) {
                float currentVolume = speakers.getVolume();
                float newX = (float) (currentVolume * this.volumePanelWidth);
                volButton.getPosition().inTL(newX - volButtonOffset, this.volButtonY);
            } else {
                volButton.getPosition().inTL(0f, this.volButtonY);
            }
        }

        public void reset() {
            this.volumeAccumulator = 0;
            this.pendingVolume = -1f;
            this.oldVolume = -1f;
            this.dragging = false;
            this.dragX = 0f;
        }
    }
}
