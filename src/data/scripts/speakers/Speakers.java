package data.scripts.speakers;

import data.scripts.decoder.Decoder;
import data.scripts.ffmpeg.AudioFrame;

public interface Speakers {
    public long advance(AudioFrame frame);
    public long advance(AudioFrame[] frames);
    
    public void start();
    public void play();
    public void stop();
    public void pause();
    public void unpause();
    public void restart();

    public float getVolume();
    public void setVolume(float volume);
    public void mute();

    public Decoder getDecoder();

    public void finish();
}
