package data.scripts.ffmpeg;

import java.nio.ByteBuffer;

public class VideoFrame extends Frame {
    public final ByteBuffer buffer;

    public VideoFrame(ByteBuffer buffer, long pts) {
        super(pts);
        this.buffer = buffer;
    }
}