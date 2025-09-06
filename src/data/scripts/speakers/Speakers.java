package data.scripts.speakers;

import data.scripts.decoder.Decoder;
import data.scripts.ffmpeg.AudioFrame;

public interface Speakers {   
    public void start();
    public void play();
    public void stop();
    public void pause();
    public void unpause();
    public void restart();

    public float getVolume();
    public void setVolume(float volume);
    public void mute();

    public long getCurrentAudioPts();
    public Decoder getDecoder();

    public void finish();
}
