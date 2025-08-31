package data.scripts.decoder;

import data.scripts.VideoModes.EOFMode;
import data.scripts.VideoModes.PlayMode;;

public interface Decoder {
    public double getDurationSeconds();
    public long getDurationUs();
    public float getVideoFps();
    public long getCurrentVideoPts();

    public void seek(long targetUs);

    public PlayMode getPlayMode();
    public void setPlayMode(PlayMode mode);

    public EOFMode getEOFMode();
    public void setEOFMode(EOFMode mode);

    public int getCurrentVideoTextureId();
    public int getCurrentVideoTextureId(float deltaTime);

    public int getErrorStatus();
}
