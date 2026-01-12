package data.scripts.ffmpeg;

import java.nio.ByteBuffer;

public class AudioFrame extends Frame {
    public final int size;
    public final int samplesPerChannel;

    public AudioFrame(ByteBuffer buffer, long pts, int size, int samplesPerChannel) {
        super(buffer, pts);
        this.size = size;
        this.samplesPerChannel = samplesPerChannel;
    }

    public long durationMillis() {
        return (samplesPerChannel * 1000L) / FFmpeg.AUDIO_SAMPLE_RATE;
    }
}