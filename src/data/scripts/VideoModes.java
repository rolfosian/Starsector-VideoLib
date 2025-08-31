package data.scripts;

public class VideoModes {

    public static enum EOFMode {
        LOOP,
        PAUSE,
        PLAY_UNTIL_END,
        FINISH,
    }

    public static enum PlayMode {
        PLAYING,
        SEEKING,
        PAUSED,
        STOPPED,
        FINISHED
    }
}
