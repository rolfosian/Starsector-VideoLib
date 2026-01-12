package data.scripts.ffmpeg;

import java.nio.ByteBuffer;

public class AudioFrame extends Frame {
    public AudioFrame(ByteBuffer buffer, long pts) {
        super(buffer, pts);
    }
}