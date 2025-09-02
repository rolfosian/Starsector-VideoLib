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
}
