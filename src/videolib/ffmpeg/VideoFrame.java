package videolib.ffmpeg;

import java.nio.ByteBuffer;

public class VideoFrame extends Frame {
    public VideoFrame(ByteBuffer buffer, long pts, long bufferPtr) {
        super(buffer, pts, bufferPtr);
    }
}