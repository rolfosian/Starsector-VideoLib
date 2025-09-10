package data.scripts.projector;

import data.scripts.VideoModes.EOFMode;
import data.scripts.VideoModes.PlayMode;
import data.scripts.decoder.Decoder;
import data.scripts.speakers.Speakers;
import data.scripts.playerui.PlayerControlPanel;

public interface Projector {
    public boolean isRendering();

    public boolean paused();
    public void play();
    public void stop();
    public void pause();
    public void unpause();
    public void setIsRendering(boolean isRendering);
    public void restart();
    public void finish();

    public Decoder getDecoder();

    public PlayMode getPlayMode();
    public void setPlayMode(PlayMode mode);
    public EOFMode getEOFMode();
    public void setEOFMode(EOFMode mode);

    public int getWidth();
    public int getHeight();

    public Speakers getSpeakers();
    public PlayerControlPanel getControlPanel();
}
