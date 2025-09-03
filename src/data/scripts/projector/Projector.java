package data.scripts.projector;

import data.scripts.decoder.Decoder;
import data.scripts.playerui.PlayerControlPanel;

public interface Projector {
    public boolean isRendering();

    public boolean paused();
    public void play();
    public void stop();
    public void pause();
    public void unpause();
    public void setIsRendering(boolean isRendering);
    public void finish();
    public Decoder getDecoder();

    public PlayerControlPanel getControlPanel();
}
