package data.scripts.decoder;

import data.scripts.VideoModes.EOFMode;
import data.scripts.VideoModes.PlayMode;
import data.scripts.buffers.TextureBuffer;;

public interface Decoder {
    public int getErrorStatus();
    public void seek(long targetUs);
    
    public double getDurationSeconds();
    public long getDurationUs();

    public float getSpf();
    public float getVideoFps();
    public void setVideoFilePath(String path);
    public long getCurrentVideoPts();
    public int getCurrentVideoTextureId();
    public int getCurrentVideoTextureId(float deltaTime);

    public int getSampleRate();
    public int getAudioChannels();
    public long getFFmpegPipePtr();

    public PlayMode getPlayMode();
    public void setPlayMode(PlayMode mode);

    public EOFMode getEOFMode();
    public void setEOFMode(EOFMode mode);

    public void start();
    public void startFrom(long pts);
    public void stop();
    public void restart();
    public void finish();

    public void setWidth(int width);
    public void setHeight(int height);
}
