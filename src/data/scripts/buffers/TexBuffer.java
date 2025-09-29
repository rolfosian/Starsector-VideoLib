package data.scripts.buffers;

import data.scripts.ffmpeg.VideoFrame;

public interface TexBuffer {
    public int size();
    public boolean isEmpty();
    public boolean isFull();
    public void add(VideoFrame frame);
    public TextureFrame popFront(int width, int height);
    public void convertSome(int width, int height, int maxConversions);
    public void convertAll(int width, int height);
    public void convertFront(int width, int height);
    public void clear();
}
