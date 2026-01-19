package videolib.projector;

import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;
import videolib.decoder.Decoder;
import videolib.speakers.Speakers;
import videolib.playerui.PlayerControlPanel;

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
