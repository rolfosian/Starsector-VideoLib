package data.scripts.buffers;

import java.util.*;
import data.scripts.ffmpeg.VideoFrame;

import java.util.*;
import data.scripts.ffmpeg.VideoFrame;

public class CachingTextureBuffer extends TextureBuffer {
    private int currentFrameIndex;
    private final List<VideoFrame> frames;

    public CachingTextureBuffer(int width, int height, List<VideoFrame> frames) {
        super(frames.size());
        this.frames = new ArrayList<>(frames);
        for (int i = 0; i < frames.size(); i++) add(frames.get(i));

        convertAll(width, height);
        currentFrameIndex = 0;
    }

    @Override
    public TextureFrame popFront(int width, int height) {
        if (isEmpty()) return null;
        TextureFrame currentFrame = textures[currentFrameIndex];
        currentFrameIndex = (currentFrameIndex + 1) % capacity;
        return currentFrame;
    }

    public void seek(long targetPts) {
        int nearestIndex = 0;
        long minDiff = Long.MAX_VALUE;

        for (int i = 0; i < frames.size(); i++) {
            long diff = Math.abs(frames.get(i).pts - targetPts);
            if (diff < minDiff) {
                minDiff = diff;
                nearestIndex = i;
            }
        }

        currentFrameIndex = nearestIndex % capacity;
    }
}