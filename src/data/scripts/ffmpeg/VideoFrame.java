package data.scripts.ffmpeg;

import java.nio.ByteBuffer;

public class VideoFrame extends Frame {
    public VideoFrame(ByteBuffer buffer, long pts) {
        super(buffer, pts);
    }
}