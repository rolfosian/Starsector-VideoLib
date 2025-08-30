package data.scripts.ffmpeg;

import java.nio.ByteBuffer;

public abstract class Frame {
    public final long pts;
    public final ByteBuffer buffer;

    protected Frame(ByteBuffer buffer, long pts) {
        this.pts = pts;
        this.buffer = buffer;

        FFmpeg.cleaner.register(this, () -> FFmpeg.freeBuffer(buffer));
    }

    public final void freeBuffer() {
        FFmpeg.freeBuffer(buffer);
    }
}