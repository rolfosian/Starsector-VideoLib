package data.scripts.ffmpeg;

import java.nio.ByteBuffer;

public abstract class Frame {
    public final long pts;
    public final ByteBuffer buffer;

    protected Frame(ByteBuffer buffer, long pts) {
        FFmpeg.cleaner.register(this, () -> FFmpeg.freeBuffer(buffer));

        this.pts = pts;
        this.buffer = buffer;
    }

    public final void freeBuffer() {
        FFmpeg.freeBuffer(buffer);
    }
}