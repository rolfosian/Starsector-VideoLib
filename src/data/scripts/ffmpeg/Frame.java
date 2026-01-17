package data.scripts.ffmpeg;

import java.nio.ByteBuffer;

public abstract class Frame {
    public final long pts;
    public final ByteBuffer buffer;
    public final long bufferPtr;

    protected Frame(ByteBuffer buffer, long pts, long bufferPtr) {
        this.pts = pts;
        this.buffer = buffer;
        this.bufferPtr = bufferPtr;
    }

    public final void freeBuffer() {
        FFmpeg.freeBuffer(bufferPtr);
    }
}