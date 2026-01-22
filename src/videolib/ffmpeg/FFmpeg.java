package videolib.ffmpeg;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.apache.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCcontext;
import org.lwjgl.openal.ALCdevice;

import com.fs.starfarer.api.Global;

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

    public static final boolean IS_LINUX;
    public static int AUDIO_SAMPLE_RATE;

    public static final int AVERROR_BSF_NOT_FOUND      = -1179861752;
    public static final int AVERROR_BUG                = -558323010;
    public static final int AVERROR_BUFFER_TOO_SMALL   = -1397118274;
    public static final int AVERROR_DECODER_NOT_FOUND  = -1128613112;
    public static final int AVERROR_DEMUXER_NOT_FOUND  = -1296385272;
    public static final int AVERROR_ENCODER_NOT_FOUND  = -1129203192;
    public static final int AVERROR_EOF                = -541478725;
    public static final int AVERROR_EXIT               = -1414092869;
    public static final int AVERROR_EXTERNAL           = -542398533;
    public static final int AVERROR_FILTER_NOT_FOUND   = -1279870712;
    public static final int AVERROR_INVALIDDATA        = -1094995529;
    public static final int AVERROR_MUXER_NOT_FOUND    = -1481985528;
    public static final int AVERROR_OPTION_NOT_FOUND   = -1414549496;
    public static final int AVERROR_PATCHWELCOME       = -1163346256;
    public static final int AVERROR_PROTOCOL_NOT_FOUND = -1330794744;
    public static final int AVERROR_STREAM_NOT_FOUND   = -1381258232;
    public static final int AVERROR_BUG2               = -541545794;
    public static final int AVERROR_UNKNOWN            = -1313558101;
    public static final int AVERROR_EXPERIMENTAL       = -733130664;
    public static final int AVERROR_INPUT_CHANGED      = -1668179713;
    public static final int AVERROR_OUTPUT_CHANGED     = -1668179714;
    public static final int AVERROR_HTTP_BAD_REQUEST   = -808465656;
    public static final int AVERROR_HTTP_UNAUTHORIZED  = -825242872;
    public static final int AVERROR_HTTP_FORBIDDEN     = -858797304;
    public static final int AVERROR_HTTP_NOT_FOUND     = -875574520;
    public static final int AVERROR_HTTP_TOO_MANY_REQUESTS = -959591672;
    public static final int AVERROR_HTTP_OTHER_4XX     = -1482175736;
    public static final int AVERROR_HTTP_SERVER_ERROR  = -1482175992;

    public static boolean hasError(long ptr) {
        return getErrorStatus(ptr) != 0;
    }

    public static void printError(long ptr) {
        print(getErrorMessage(ptr));
    }

    public static void printError(int errorCode) {
        print(getErrorMessage(errorCode));
    }

    public static String getErrorMessage(long ptr) {
        return getErrorMessage(getErrorStatus(ptr));
    }

    public static String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case AVERROR_EOF:
                return "End of file";
            case AVERROR_EXIT:
                return "Immediate exit requested";
            case AVERROR_BUG:
            case AVERROR_BUG2:
                return "Internal bug";
            case AVERROR_INVALIDDATA:
                return "Invalid data found when processing input";
            case AVERROR_BUFFER_TOO_SMALL:
                return "Buffer too small";
            case AVERROR_DECODER_NOT_FOUND:
                return "Decoder not found";
            case AVERROR_ENCODER_NOT_FOUND:
                return "Encoder not found";
            case AVERROR_DEMUXER_NOT_FOUND:
                return "Demuxer not found";
            case AVERROR_MUXER_NOT_FOUND:
                return "Muxer not found";
            case AVERROR_STREAM_NOT_FOUND:
                return "Stream not found";
            case AVERROR_OPTION_NOT_FOUND:
                return "Option not found";
            case AVERROR_FILTER_NOT_FOUND:
                return "Filter not found";
            case AVERROR_PROTOCOL_NOT_FOUND:
                return "Protocol not found";
            case AVERROR_BSF_NOT_FOUND:
                return "Bitstream filter not found";
            case AVERROR_EXTERNAL:
                return "Error in external library";
            case AVERROR_UNKNOWN:
                return "Unknown error";
            case AVERROR_EXPERIMENTAL:
                return "Feature flagged experimental";
            case AVERROR_INPUT_CHANGED:
                return "Input changed, reconfiguration required";
            case AVERROR_OUTPUT_CHANGED:
                return "Output changed, reconfiguration required";
            case AVERROR_HTTP_BAD_REQUEST:
                return "HTTP 400 Bad Request";
            case AVERROR_HTTP_UNAUTHORIZED:
                return "HTTP 401 Unauthorized";
            case AVERROR_HTTP_FORBIDDEN:
                return "HTTP 403 Forbidden";
            case AVERROR_HTTP_NOT_FOUND:
                return "HTTP 404 Not Found";
            case AVERROR_HTTP_TOO_MANY_REQUESTS:
                return "HTTP 429 Too Many Requests";
            case AVERROR_HTTP_OTHER_4XX:
                return "HTTP Other 4XX";
            case AVERROR_HTTP_SERVER_ERROR:
                return "HTTP 5XX Server Error";
            default:
                return "Unrecognized error code: " + errorCode;
        }
    }

    public static void init() {}
    static {
        String osName = System.getProperty("os.name").toLowerCase();
        String rootModPath = Global.getSettings().getModManager().getModSpec("video_lib").getPath();

        if (osName.startsWith("windows")) {
            System.load(rootModPath + "/ffmpeg-jni/bin/windows/ffmpegjni.dll");
            IS_LINUX = false;

        } else if (osName.startsWith("linux")) {
            System.load(rootModPath + "/ffmpeg-jni/bin/linux/ffmpegjni.so");
            IS_LINUX = true;

        } else if (osName.startsWith("mac")) {
            System.load(rootModPath + "/ffmpeg-jni/bin/mac/ffmpegjni.dylib");
            IS_LINUX = false;

        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + osName);
        }

        if (Global.getSettings().isSoundEnabled()) {
            ALCcontext context = ALC10.alcGetCurrentContext();
            ALCdevice device = ALC10.alcGetContextsDevice(context);

            IntBuffer buffer = BufferUtils.createIntBuffer(1);
            ALC10.alcGetInteger(device, ALC10.ALC_FREQUENCY, buffer);
            int sampleRate = buffer.get(0);
            init(sampleRate);
            AUDIO_SAMPLE_RATE = sampleRate;

        } else {
            init(0);
        }
    }

    // Native methods
    private static native void init(int audioSampleRate); // ref AudioFrame/VideoFrame classes/constructors

    public static native void freeBuffer(long bufferPtr);
    public static native long getFilePtr(String filePath);
    public static native boolean fileExists(String filePath);
    public static native int[] getWidthAndHeight(String filepath);

    // jpeg, png, webp, gif (still frames)
    public static native long openImage(String filename, int width, int height);
    public static native void closeImage(long ptr);
    public static native ByteBuffer getImageBuffer(long ptr);
    public static native void resizeImage(long ptr, int newWidth, int newHeight);
    public static native boolean isImageRGBA(long ptr);

    // general video functions compatible with both sound and no sound context pointers
    public static native int getErrorStatus(long ptr);
    public static native float getVideoFps(long ptr);
    public static native double getDurationSeconds(long ptr);
    public static native long getDurationUs(long ptr);
    public static native boolean isRGBA(long ptr); // YUVA420P and GIF alpha channel support

    public static native void seek(long ptr, long targetUs);
    public static native void closeCtx(long ptr);

    // raw rgb frames with no sound
    public static native long openCtxNoSound(String filename, int width, int height, long startUs);
    public static native VideoFrame readFrameNoSound(long ptr);

    // with sound
    public static native long openCtx(String fileName, int width, int height, long startUs);
    public static native Frame read(long ptr);

    public static native int getAudioSampleRate(long ptr);
    public static native int getAudioChannels(long ptr);
    public static native long getTotalFrameCount(long ptr);
}
