package videolib.buffers.unused;

import java.util.*;

import videolib.ffmpeg.VideoFrame;

public class VideoFrameBuffer {
    private VideoFrame[] buffer;
    private int head = 0;
    private int tail = 0;
    private int count = 0;
    public int capacity;

    public VideoFrameBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new VideoFrame[capacity];
    }

    public void add(VideoFrame frame) {
        buffer[tail] = frame;
        tail = (tail + 1) % capacity;
        if (count < capacity) count++;
        else head = (head + 1) % capacity;
    }

    public VideoFrame get(int index) {
        if (index < 0 || index >= count) return null;
        return buffer[(head + index) % capacity];
    }

    public void clear() {
        for (int i = 0; i < capacity; i++) buffer[i] = null;
        head = tail = count = 0;
    }

    public VideoFrame pop() {
        if (count == 0) return null;
        VideoFrame frame = buffer[head];
        buffer[head] = null;
        head = (head + 1) % capacity;
        count--;
        return frame;
    }

    public int size() { return count; }

    public boolean full() {
        return count == capacity;
    }
}
