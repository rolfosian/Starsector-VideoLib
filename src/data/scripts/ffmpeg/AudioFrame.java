package data.scripts.ffmpeg;

import java.nio.ByteBuffer;

public class AudioFrame extends Frame {
    public final long durationUs;

    public AudioFrame(ByteBuffer buffer, long pts, long durationUs) {
        super(buffer, pts);
        this.durationUs = durationUs;
    }
}