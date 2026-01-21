package videolib.buffers;

import videolib.ffmpeg.VideoFrame;

public interface TexBuffer {
    public void initTexStorage(int width, int height);
    public int getTextureId();
    public int size();
    public boolean isEmpty();
    public boolean isFull();
    public void add(VideoFrame frame);
    public long update();
    public void clear();
    public void cleanupTexStorage();
}
