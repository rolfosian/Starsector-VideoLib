package data.scripts.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import com.fs.starfarer.api.EveryFrameScript;
import data.scripts.projector.PlanetProjector;

public class VideoUtils {
    public static String formatTime(long us) {
        double totalSeconds = us / 1_000_000.0;

        if (totalSeconds < 3600) {
            long minutes = (long) (totalSeconds / 60);
            double seconds = totalSeconds % 60;
            return String.format("%02d:%s", minutes, formatSeconds(seconds));
        } else if (totalSeconds <= 36000) {
            long hours = (long) (totalSeconds / 3600);
            long minutes = (long) ((totalSeconds % 3600) / 60);
            double seconds = totalSeconds % 60;
            return String.format("%d:%02d:%s", hours, minutes, formatSeconds(seconds));
        } else {
            long hours = (long) (totalSeconds / 3600);
            long minutes = (long) ((totalSeconds % 3600) / 60);
            double seconds = totalSeconds % 60;
            return String.format("%d*:%02d:%s", hours, minutes, formatSeconds(seconds));
        }
    }

    public static String formatTimeNoDecimals(long us) {
        double totalSeconds = us / 1_000_000.0;

        if (totalSeconds < 3600) {
            long minutes = (long) (totalSeconds / 60);
            double seconds = totalSeconds % 60;
            return String.format("%02d:%s", minutes, formatSecondsNoDecimals(seconds));
        } else if (totalSeconds <= 36000) {
            long hours = (long) (totalSeconds / 3600);
            long minutes = (long) ((totalSeconds % 3600) / 60);
            double seconds = totalSeconds % 60;
            return String.format("%d:%02d:%s", hours, minutes, formatSecondsNoDecimals(seconds));
        } else {
            long hours = (long) (totalSeconds / 3600);
            long minutes = (long) ((totalSeconds % 3600) / 60);
            double seconds = totalSeconds % 60;
            return String.format("%d:%02d:%s", hours, minutes, formatSecondsNoDecimals(seconds));
        }
    }

    public static String formatSeconds(double seconds) {
        if (Math.abs(seconds - Math.round(seconds)) < 1e-9) {
            return String.format("%02d", (int) Math.round(seconds));
        } else {
            return String.format("%04.1f", seconds);
        }
    }

    public static String formatSecondsNoDecimals(double seconds) {
        return String.format("%02d", (int) seconds);
    }

    public static String formatTimeNoDecimalsWithRound(long us) {
        double totalSeconds = us / 1_000_000.0;

        if (totalSeconds < 3600) {
            long minutes = (long) (totalSeconds / 60);
            double seconds = totalSeconds % 60;
            return String.format("%02d:%s", minutes, formatSecondsNoDecimalsWithRound(seconds));
        } else if (totalSeconds <= 36000) {
            long hours = (long) (totalSeconds / 3600);
            long minutes = (long) ((totalSeconds % 3600) / 60);
            double seconds = totalSeconds % 60;
            return String.format("%d:%02d:%s", hours, minutes, formatSecondsNoDecimalsWithRound(seconds));
        } else {
            long hours = (long) (totalSeconds / 3600);
            long minutes = (long) ((totalSeconds % 3600) / 60);
            double seconds = totalSeconds % 60;
            return String.format("%d:%02d:%s", hours, minutes, formatSecondsNoDecimalsWithRound(seconds));
        }
    }

    public static String formatSecondsNoDecimalsWithRound(double seconds) {
        return String.format("%02d", (int) Math.round(seconds));
    }

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Map<String, PlanetProjector> videoLibPlanetTexIds = new HashMap<>();

    public static String generateRandomPlanetProjectorId(PlanetProjector projector) {
        StringBuilder sb = new StringBuilder(6);
        
        for (int i = 0; i < 6; i++) {
            int index = ThreadLocalRandom.current().nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        String result = "vl_" + sb.toString();

        if (videoLibPlanetTexIds.containsKey(result)) return generateRandomPlanetProjectorId(projector);
        videoLibPlanetTexIds.put(result, projector);

        return result;
    }

    public static void removeId(String id) {
        videoLibPlanetTexIds.remove(id);
    }

    public static PlanetProjector getPlanetProjector(String id) {
        return videoLibPlanetTexIds.get(id);
    }

    public static Collection<PlanetProjector> getPlanetProjectors() {
        return videoLibPlanetTexIds.values();
    }


    private static Set<EveryFrameScript> ringBandAndSpriteProjectors = new HashSet<>();

    public static Set<EveryFrameScript> getRingBandAndSpriteProjectors() {
        return ringBandAndSpriteProjectors;
    }

    private static final Object prefs;
    private static final MethodHandle prefsGetHandle;
    
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> prefsClass = Class.forName("java.util.prefs.Preferences", false, Class.class.getClassLoader());
            MethodHandle userRootHandle = lookup.findStatic(prefsClass, "userRoot", MethodType.methodType(prefsClass));
            MethodHandle nodeHandle = lookup.findVirtual(prefsClass, "node", MethodType.methodType(prefsClass, String.class));
            prefsGetHandle = lookup.findVirtual(prefsClass, "get", MethodType.methodType(String.class, String.class, String.class));

            Object userRoot = userRootHandle.invoke();
            prefs = nodeHandle.invoke(userRoot, "/com/fs/starfarer");

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static float getSoundVolumeMult() {
        try {
            String spl = ((String)prefsGetHandle.invoke(prefs, "gameplaySettings", "{}")).split("soundVolume")[1].split(",")[0].split(":")[1];
            return Float.valueOf(spl);
        } catch (Throwable ignored) {
            return 1f;
        }
    }

    public static void init() {}

}
