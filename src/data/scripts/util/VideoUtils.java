package data.scripts.util;

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

    public static String formatSeconds(double seconds) {
        if (Math.abs(seconds - Math.round(seconds)) < 1e-9) {
            return String.format("%02d", (int) Math.round(seconds));
        } else {
            return String.format("%04.1f", seconds);
        }
    }

    public static float getControlsHeight(int videoHeight) {
        return videoHeight / 100 * 6.5f;
    }
}
