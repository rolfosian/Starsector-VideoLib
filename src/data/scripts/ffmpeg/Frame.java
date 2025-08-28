package data.scripts.ffmpeg;

public abstract class Frame {
    public final long pts;
    
    protected Frame(long pts) {
        this.pts = pts;
    }
}