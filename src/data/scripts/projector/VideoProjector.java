package data.scripts.projector;

import java.util.List;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import data.scripts.VideoMode;
import data.scripts.decoder.Decoder;

public abstract class VideoProjector implements CustomUIPanelPlugin {
    public void init(PositionAPI panelPos, CustomPanelAPI panel) {}

    public abstract void start();
    public abstract void play();
    public abstract void pause();
    public abstract void unpause();
    public abstract void stop();
    public abstract void restart();
    public abstract void finish();

    public abstract boolean paused();

    public abstract Decoder getDecoder();

    public abstract int getWidth();
    public abstract int getHeight();

    public abstract VideoMode getMode();
    public abstract void setMode(VideoMode mode);
    public abstract void setCurrentTextureId(int id);

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
