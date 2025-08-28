package data.scripts.ffmpeg;

import java.nio.ByteBuffer;

public class AudioFrame extends Frame {
    public final ByteBuffer buffer;
    public final int size;

    public AudioFrame(ByteBuffer buffer, int size, long pts) {
        super(pts);
        this.buffer = buffer;
        this.size = size;
    }
}