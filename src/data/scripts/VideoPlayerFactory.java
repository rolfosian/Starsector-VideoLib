package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.CustomPanelAPI;

import data.scripts.controlpanel.PlayerControlPanel;
import data.scripts.projector.VideoProjector;

import java.util.*;
import org.apache.log4j.Logger;

public class VideoPlayerFactory {
    private static final Logger logger = Logger.getLogger(VideoPlayerFactory.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    public static VideoPlayerWithControls addControlPanel(VideoProjector projectorPlugin, CustomPanelAPI projectorPanel) {
        float width = projectorPlugin.getWidth();
        float height = projectorPlugin.getHeight();

        int controlsHeight = (int) (height / 100 * 6.5f);
        CustomPanelAPI masterPanel = Global.getSettings().createCustom(width, height + 5f + controlsHeight, null);
        masterPanel.addComponent(projectorPanel).inTL(0f, 0f);

        PlayerControlPanel controlPanel = new PlayerControlPanel(projectorPlugin, (int) width, controlsHeight, false);
        masterPanel.addComponent(controlPanel.getControlPanel()).belowMid(projectorPanel, 5f);
        
        return new VideoPlayerWithControls(masterPanel, controlPanel);
    }
}
