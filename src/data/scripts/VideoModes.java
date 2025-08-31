package data.scripts;

// these need to be separated for their specific uses i think

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
