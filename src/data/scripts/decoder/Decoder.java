package data.scripts.decoder;

import data.scripts.VideoMode;

public interface Decoder {
    public double getDuration();
    public float getVideoFps();
    public long getCurrentVideoPts();
    
    public void seek(long targetUs);
    public void seek(double targetSecond);

    public VideoMode getMode();
    public void setMode(VideoMode mode);
}
