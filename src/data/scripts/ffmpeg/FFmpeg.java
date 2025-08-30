package data.scripts.ffmpeg;

import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

public class FFmpeg {
    private static final Logger logger = Logger.getLogger(FFmpeg.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.error(sb.toString());
    }

    protected static final Cleaner cleaner = Cleaner.create();

    static {
        String osName = System.getProperty("os.name").toLowerCase();
        String cwd = System.getProperty("user.dir");

        if (osName.startsWith("windows")) {
            String binDir = cwd.replace("starsector-core", "") + "mods/VideoLib/ffmpeg-jni/bin/windows/";

            System.load(binDir + "ffmpegjni.dll");  // our bridge

        } else if (osName.startsWith("linux")) {
            String binDir = cwd + "/mods/VideoLib/ffmpeg-jni/bin/linux/";

            System.load(binDir + "ffmpegjni.so"); // our bridge

        } else if (osName.startsWith("mac")) {
            String binDir = cwd + "/mods/VideoLib/ffmpeg-jni/bin/mac/";
            // TODO: Mac static barebones ffmpeg decoder libs and jni compilation
            // See: ffmpeg-static/README.md
            System.load(binDir + "ffmpegjni.dylib"); // our bridge

        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + osName);
        }
    }

    // Native methods
    public static native void init(); // av_register_all / network init if needed
    public static native void freeBuffer(ByteBuffer toFree);

    // jpeg, png, webp, gif (still frames)
    public static native long openImage(String filename, int width, int height);
    public static native void closeImage(long ptr);
    public static native ByteBuffer getImageBuffer(long ptr);
    public static native void resizeImage(long ptr, int newWidth, int newHeight);

    // general video functions compatible with both sound and no sound context pointers
    public static native float getVideoFps(long ptr);
    public static native double getDurationSeconds(long ptr);
    public static native long getDurationUs(long ptr);
    
    public static native void seek(long ptr, long targetUs);
    public static native void closePipe(long ptr);

    // raw rgb frames with no sound
    public static native long openPipeNoSound(String filename, int width, int height, int startFrame);
    public static native VideoFrame readFrameNoSound(long ptr);

    // with sound
    public static native long openPipe(String fileName, int width, int height, int startFrame);
    public static native Frame read(long ptr);

    public static native int getAudioSampleRate(long ptr);
    public static native int getAudioChannels(long ptr);
}
