package data.scripts.ffmpeg;

import java.nio.ByteBuffer;

public class AudioFrame extends Frame {
    public final int size;

    public AudioFrame(ByteBuffer buffer, int size, long pts) {
        super(buffer, pts);
        this.size = size;
    }
}