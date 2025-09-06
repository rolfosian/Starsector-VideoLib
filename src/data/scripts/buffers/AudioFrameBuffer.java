package data.scripts.buffers;

import java.util.*;

import data.scripts.ffmpeg.AudioFrame;

public class AudioFrameBuffer {
    private AudioFrame[] buffer;
    private int head = 0;
    private int tail = 0;
    private int count = 0;
    public int capacity;

    public AudioFrameBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new AudioFrame[capacity];
    }

    public void add(AudioFrame frame) {
        buffer[tail] = frame;
        tail = (tail + 1) % capacity;
        if (count < capacity) count++;
        else head = (head + 1) % capacity;
    }

    public AudioFrame get(int index) {
        if (index < 0 || index >= count) return null;
        return buffer[(head + index) % capacity];
    }

    public void clear() {
        for (int i = 0; i < capacity; i++) buffer[i] = null;
        head = tail = count = 0;
    }

    public AudioFrame pop() {
        if (count == 0) return null;
        AudioFrame frame = buffer[head];
        buffer[head] = null;
        head = (head + 1) % capacity;
        count--;
        return frame;
    }

    public AudioFrame[] pop(int numFrames) {
        if (count == 0 || numFrames <= 0) return null;
        
        int framesToPop = Math.min(numFrames, count);
        AudioFrame[] frames = new AudioFrame[framesToPop];
        
        for (int i = 0; i < framesToPop; i++) {
            frames[i] = buffer[head];
            buffer[head] = null;
            head = (head + 1) % capacity;
            count--;
        }
        
        return frames;
    }

    public int size() { return count; }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean isFull() {
        return count == capacity;
    }
}
