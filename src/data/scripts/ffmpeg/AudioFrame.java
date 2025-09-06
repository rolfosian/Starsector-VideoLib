package data.scripts.ffmpeg;

import java.nio.ByteBuffer;

public class AudioFrame extends Frame {
    public final int size;
    public final int samples;

    public AudioFrame(ByteBuffer buffer, int size, int samples, long pts) {
        super(buffer, pts);
        this.size = size;
        this.samples = samples;
    }
}