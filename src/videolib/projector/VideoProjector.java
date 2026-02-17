package videolib.projector;

import java.util.List;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;
import videolib.buffers.TexBuffer;
import videolib.decoder.Decoder;
import videolib.playerui.PlayerControlPanel;
import videolib.speakers.Speakers;
import static videolib.util.UiUtil.utils;

public abstract class VideoProjector implements CustomUIPanelPlugin, Projector {
    protected CustomPanelAPI panel;
    protected boolean keepAlive;
    private UIPanelAPI topAncestor;
    private EveryFrameScript cleanupScript;

    public void init(PositionAPI panelPos, CustomPanelAPI panel) {
        if (this.cleanupScript != null) {
            Global.getSector().removeTransientScript(cleanupScript);
        }

        if (!keepAlive) {
            this.topAncestor = utils.findTopAncestor(panel);

            this.cleanupScript = new EveryFrameScript() {
                private boolean isDone = false;
                
                @Override
                public void advance(float arg0) {
                    if (topAncestor != utils.findTopAncestor(panel)) {
                        finish();
                        isDone = true;
                        Global.getSector().removeTransientScript(this);
                    }
                }

                @Override
                public boolean isDone() {
                    return isDone;
                }
    
                @Override
                public boolean runWhilePaused() {
                    return true;
                }
            };
            Global.getSector().addTransientScript(cleanupScript);
        }
    }

    public abstract void play();
    public abstract void pause();
    public abstract void unpause();
    public abstract void stop();
    public abstract void restart();
    public abstract void finish();

    public abstract boolean paused();
    public abstract void setClickToPause(boolean clickToPause);

    public abstract Decoder getDecoder();
    public abstract void setVideoFilePath(String path);

    public abstract int getWidth();
    public abstract void setWidth(int width);
    public abstract int getHeight();
    public abstract void setHeight(int height);

    public abstract PlayMode getPlayMode();
    public abstract void setPlayMode(PlayMode mode);

    public abstract EOFMode getEOFMode();
    public abstract void setEOFMode(EOFMode mode);

    public abstract void setCurrentTextureId(int id);
    public abstract void setTextureBuffer(TexBuffer buffer);

    public abstract boolean isRendering();
    public abstract void setIsRendering(boolean rendering);
    public abstract void setControlPanel(PlayerControlPanel controlPanel);
    public abstract PlayerControlPanel getControlPanel();
    public abstract Speakers getSpeakers();

    @Override
    public void render(float alphaMult) {}

    @Override
    public void positionChanged(PositionAPI position) {}

    @Override
    public void advance(float arg0) {}

    @Override
    public void buttonPressed(Object arg0) {}

    @Override
    public void processInput(List<InputEventAPI> arg0) {}

    @Override
    public void renderBelow(float arg0) {}
    
}
