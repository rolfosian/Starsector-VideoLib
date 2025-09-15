package data.scripts;

public class VideoModes {

    public static enum EOFMode {
        LOOP,
        PAUSE,
        PLAY_UNTIL_END,
        // FINISH, TODO IMPL
    }

    public static enum PlayMode {
        PLAYING,
        SEEKING,
        PAUSED,
        STOPPED,
        // FINISHED TODO IMPL
    }
}
