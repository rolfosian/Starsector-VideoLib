package videolib.speakers;

import org.lwjgl.util.vector.Vector2f;

import videolib.decoder.Decoder;

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

    public void setSoundDirection(Vector2f viewportLoc);
    public void resetSoundDirection();

    public int getSourceId();

    public void notifySeek(long targetUs);
}
