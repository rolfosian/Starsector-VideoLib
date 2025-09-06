package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import data.scripts.VideoModes.EOFMode;
import data.scripts.VideoModes.PlayMode;

import data.scripts.playerui.AudioVideoPlayer;
import data.scripts.playerui.MuteVideoPlayer;
import data.scripts.playerui.PlayerControlPanel;
import data.scripts.playerui.PlayerPanelPlugin;
import data.scripts.playerui.MuteVideoPlayerWithControls;
import data.scripts.playerui.AudioVideoPlayerWithControls;

import data.scripts.projector.AudioVideoProjector;
import data.scripts.projector.MuteVideoProjector;
import data.scripts.projector.VideoProjector;
import data.scripts.speakers.Speakers;

import java.awt.Color;
import java.util.*;
import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;

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

    public static MuteVideoPlayerWithControls createMutePlayerWithControls(String videoId, int width, int height, PlayMode startingPlayMode, EOFMode startingEOFMode) {
        VideoProjector projectorPlugin = new MuteVideoProjector(videoId, width, height, startingPlayMode, startingEOFMode);
        CustomPanelAPI projectorPanel = Global.getSettings().createCustom(width, height, projectorPlugin);

        int controlsHeight = 70;
        PlayerPanelPlugin panelPlugin = new PlayerPanelPlugin();
        CustomPanelAPI masterPanel = Global.getSettings().createCustom(width, height + 5f + controlsHeight, panelPlugin);
        masterPanel.addComponent(projectorPanel).inTL(0f, 0f);

        PlayerControlPanel controlPanel = new PlayerControlPanel(projectorPlugin, width, controlsHeight, null);
        masterPanel.addComponent(controlPanel.getControlPanel()).inTL(0f, height + controlsHeight);

        return new MuteVideoPlayerWithControls(masterPanel, controlPanel, projectorPlugin, projectorPanel);
    }

    public static MuteVideoPlayerWithControls createMutePlayerWithControls(String videoId, int width, int height, PlayMode startingPlayMode, EOFMode startingEOFMode, Color textColor, Color bgButtonColor) {
        VideoProjector projectorPlugin = new MuteVideoProjector(videoId, width, height, startingPlayMode, startingEOFMode);
        CustomPanelAPI projectorPanel = Global.getSettings().createCustom(width, height, projectorPlugin);

        int controlsHeight = 70;
        PlayerPanelPlugin panelPlugin = new PlayerPanelPlugin();
        CustomPanelAPI masterPanel = Global.getSettings().createCustom(width, height + 5f + controlsHeight, panelPlugin);
        masterPanel.addComponent(projectorPanel).inTL(0f, 0f);

        PlayerControlPanel controlPanel = new PlayerControlPanel(projectorPlugin, width, controlsHeight, null, textColor, bgButtonColor);
        masterPanel.addComponent(controlPanel.getControlPanel()).inTL(0f, height + controlsHeight);

        return new MuteVideoPlayerWithControls(masterPanel, controlPanel, projectorPlugin, projectorPanel);
    }

    public static MuteVideoPlayer createMutePlayer(String videoId, int width, int height, PlayMode startingPlayMode, EOFMode startingEOFMode) {
        VideoProjector projectorPlugin = new MuteVideoProjector(videoId, width, height, startingPlayMode, startingEOFMode);
        CustomPanelAPI projectorPanel = Global.getSettings().createCustom(width, height, projectorPlugin);
        
        return new MuteVideoPlayer(projectorPanel, projectorPlugin);
    }

    public static AudioVideoPlayer createAudioVideoPlayer(String videoId, int width, int height, float volume, PlayMode startingPlayMode, EOFMode startingEOFMode) {
        VideoProjector projectorPlugin = new AudioVideoProjector(videoId, width, height, volume, startingPlayMode, startingEOFMode);
        CustomPanelAPI projectorPanel = Global.getSettings().createCustom(width, height, projectorPlugin);

        return new AudioVideoPlayer(projectorPanel, projectorPlugin);
    }

    public static AudioVideoPlayerWithControls createAudioVideoPlayerWithControls(String videoId, int width, int height, float volume, PlayMode startingPlayMode, EOFMode startingEOFMode) {
        VideoProjector projectorPlugin = new AudioVideoProjector(videoId, width, height, volume, startingPlayMode, startingEOFMode);
        CustomPanelAPI projectorPanel = Global.getSettings().createCustom(width, height, projectorPlugin);
        Speakers speakers = projectorPlugin.getSpeakers();

        int controlsHeight = 70;
        PlayerPanelPlugin panelPlugin = new PlayerPanelPlugin();
        CustomPanelAPI masterPanel = Global.getSettings().createCustom(width, height + 5f + controlsHeight, panelPlugin);
        masterPanel.addComponent(projectorPanel).inTL(0f, 0f);

        PlayerControlPanel controlPanel = new PlayerControlPanel(projectorPlugin, width, controlsHeight, speakers);
        masterPanel.addComponent(controlPanel.getControlPanel()).inTL(0f, height + controlsHeight);

        return new AudioVideoPlayerWithControls(masterPanel, controlPanel, speakers, projectorPlugin, projectorPanel);
    }

    public static AudioVideoPlayerWithControls createAudioVideoPlayerWithControls(String videoId, int width, int height, float volume, PlayMode startingPlayMode, EOFMode startingEOFMode, Color textColor, Color bgButtonColor) {
        VideoProjector projectorPlugin = new AudioVideoProjector(videoId, width, height, volume, startingPlayMode, startingEOFMode);
        CustomPanelAPI projectorPanel = Global.getSettings().createCustom(width, height, projectorPlugin);
        Speakers speakers = projectorPlugin.getSpeakers();

        int controlsHeight = 70;
        PlayerPanelPlugin panelPlugin = new PlayerPanelPlugin();
        CustomPanelAPI masterPanel = Global.getSettings().createCustom(width, height + 5f + controlsHeight, panelPlugin);
        masterPanel.addComponent(projectorPanel).inTL(0f, 0f);

        PlayerControlPanel controlPanel = new PlayerControlPanel(projectorPlugin, width, controlsHeight, speakers, textColor, bgButtonColor);
        masterPanel.addComponent(controlPanel.getControlPanel()).inTL(0f, height + controlsHeight);

        return new AudioVideoPlayerWithControls(masterPanel, controlPanel, speakers, projectorPlugin, projectorPanel);
    }
}
