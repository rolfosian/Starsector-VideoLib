package data.scripts.buffers;

import java.util.*;
import data.scripts.ffmpeg.VideoFrame;

/**probably dont use this it will take like 5GB of vram for a single 30fps 1 min long 720p video*/
public class CachingTextureBuffer extends TextureBuffer {
    private int currentFrameIndex;
    private boolean ready = false;

    public CachingTextureBuffer(int width, int height, List<VideoFrame> frames) {
        super(frames.size());
        for (int i = 0; i < textures.length; i++) add(frames.get(i));
        currentFrameIndex = 0;
        ready = true;
    }

    public boolean ready() {
        return ready;
    }

    @Override
    public TextureFrame popFront(int width, int height) {
        if (isEmpty()) return null;
    
        TextureFrame currentFrame = textures[currentFrameIndex];
    
        if (currentFrameIndex < textures.length - 1) {
            currentFrameIndex++;
        } else {
            currentFrameIndex = 0;
        }
    
        return currentFrame;
    }

    public void seek(long targetPts) {
        int left = 0;
        int right = textures.length - 1;
    
        while (left <= right) {
            int mid = (left + right) >>> 1;
            long midPts = textures[mid].pts;
    
            if (midPts == targetPts) {
                currentFrameIndex = mid % capacity;
                return;
            } else if (midPts < targetPts) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
    
        int nearestIndex;
        if (left >= textures.length) {
            nearestIndex = textures.length - 1;
        } else if (right < 0) {
            nearestIndex = 0;
        } else {
            long leftDiff = Math.abs(textures[left].pts - targetPts);
            long rightDiff = Math.abs(textures[right].pts - targetPts);
            nearestIndex = leftDiff < rightDiff ? left : right;
        }
    
        currentFrameIndex = nearestIndex % capacity;
    }
}