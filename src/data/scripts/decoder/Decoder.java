package data.scripts.decoder;

import data.scripts.VideoModes.EOFMode;
import data.scripts.VideoModes.PlayMode;;

public interface Decoder {
    public float getSpf();
    public double getDurationSeconds();
    public long getDurationUs();
    public float getVideoFps();
    public long getCurrentVideoPts();
    public long getFFmpegPipePtr();

    public void seek(long targetUs);

    public PlayMode getPlayMode();
    public void setPlayMode(PlayMode mode);

    public EOFMode getEOFMode();
    public void setEOFMode(EOFMode mode);

    public void setVideoFilePath(String path);
    public int getCurrentVideoTextureId();
    public int getCurrentVideoTextureId(float deltaTime);

    public int getErrorStatus();
    public void restart();

    public void setWidth(int width);
    public void setHeight(int height);
}
