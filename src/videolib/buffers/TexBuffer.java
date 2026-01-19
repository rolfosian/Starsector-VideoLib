package videolib.buffers;

import videolib.ffmpeg.VideoFrame;

public interface TexBuffer {
    public int size();
    public boolean isEmpty();
    public boolean isFull();
    public void add(VideoFrame frame);
    public long peekPts();
    public TextureFrame pop(int width, int height);
    public void convertSome(int width, int height, int maxConversions);
    public void convertAll(int width, int height);
    public void convertFront(int width, int height);
    public void clear();
    public void deleteTexture(int id);
}
