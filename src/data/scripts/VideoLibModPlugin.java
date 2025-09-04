package data.scripts;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

// import data.scripts.buffers.OverlayingTextureBuffer;
import data.scripts.ffmpeg.FFmpeg;
import data.scripts.planetlistener.PlanetProjectorListener;
import data.scripts.util.TexReflection;
import data.scripts.util.VideoUtils;

public class VideoLibModPlugin extends BaseModPlugin {
    public static final Logger logger = Global.getLogger(VideoLibModPlugin.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    private static Thread mainThread;

    @Override
    public void onApplicationLoad() {
        FFmpeg.init();
        TexReflection.init();

        VideoPaths.populate();
        mainThread = Thread.currentThread();

    }

    @Override
    public void onGameLoad(boolean newGame) {
        Global.getSector().addTransientListener(new PlanetProjectorListener(false));
    }

    public static Thread getMainThread() {
        return mainThread;
    }

}
