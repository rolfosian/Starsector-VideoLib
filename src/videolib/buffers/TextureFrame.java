package videolib.buffers;

public class TextureFrame {
    public int id;
    public long pts;

    public TextureFrame(int id, long pts) {
        this.id = id;
        this.pts = pts;
    }
}