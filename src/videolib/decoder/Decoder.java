package videolib.decoder;

import com.fs.starfarer.api.Global;

import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;
import videolib.buffers.TexBuffer;

public interface Decoder {
    public long getFFmpegCtxPtr();
    public int getErrorStatus();
    public void seek(long targetUs);
    public void seekWithoutClearingBuffer(long targetUs);
    
    public double getDurationSeconds();
    public long getDurationUs();

    public float getSpf();
    public float getVideoFps();
    public String getVideoFilePath();
    public void setVideoFilePath(String path);
    public long getCurrentVideoPts();
    public int getCurrentVideoTextureId();
    public int getCurrentVideoTextureId(float deltaTime);
    public TexBuffer getTextureBuffer();

    public int getSampleRate();
    public int getAudioChannels();
    
    public PlayMode getPlayMode();
    public void setPlayMode(PlayMode mode);

    public EOFMode getEOFMode();
    public void setEOFMode(EOFMode mode);

    public void start(long startUs);
    public void stop();
    public void restart(long startUs);
    public void finish();

    public void setWidth(int width);
    public void setHeight(int height);
}
