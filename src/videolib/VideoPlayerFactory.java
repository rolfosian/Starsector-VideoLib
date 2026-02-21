package videolib;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.campaign.BaseLocation;
import com.fs.starfarer.campaign.CustomCampaignEntity;

import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;

import videolib.entities.CampaignBillboard;
import videolib.entities.CampaignBillboard.BillboardFacingDelegate;
import videolib.entities.CampaignBillboard.BillboardDialogDelegate;
import videolib.entities.RotationalTargeter;

import videolib.playerui.AudioVideoPlayer;
import videolib.playerui.MuteVideoPlayer;
import videolib.playerui.PlayerControlPanel;
import videolib.playerui.VideoPlayer;
import videolib.playerui.MuteVideoPlayerWithControls;
import videolib.playerui.AudioVideoPlayerWithControls;
import videolib.playerui.ImagePanel;

import videolib.projector.AudioVideoProjector;
import videolib.projector.ImagePlugin;
import videolib.projector.MuteVideoProjector;
import videolib.projector.VideoProjector;
import videolib.projector.AutoTexProjector.AutoTexProjectorAPI;

import videolib.speakers.Speakers;

import java.awt.Color;
import java.util.Map;

import org.apache.log4j.Logger;
//

/**
 * Factory for creating video and image player UI components backed by projector plugins.
 * Provides mute and audio-enabled variants, with or without on-screen controls.
 */
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

    /**
     * Creates a mute-only video player with a built-in control panel.
     *
     * @param videoId           id of the video asset defined in settings.json
     * @param width             panel width in pixels
     * @param height            panel height in pixels
     * @param startingPlayMode  initial playback mode
     * @param startingEOFMode   behavior when the video reaches end-of-file
     * @param keepAlive         whether to keep resources alive when not visible
     * @return a {@link MuteVideoPlayerWithControls} wrapping the projector and controls
     */
    public static VideoPlayer createMutePlayerWithControls(String videoId, int width, int height, PlayMode startingPlayMode, EOFMode startingEOFMode, boolean keepAlive) {
        return createMutePlayerWithControls(videoId, width, height, startingPlayMode, startingEOFMode, keepAlive, 0);
    }

    /**
     * Creates a mute-only video player with a built-in control panel.
     *
     * @param videoId           id of the video asset defined in settings.json
     * @param width             panel width in pixels
     * @param height            panel height in pixels
     * @param startingPlayMode  initial playback mode
     * @param startingEOFMode   behavior when the video reaches end-of-file
     * @param keepAlive         whether to keep resources alive when not visible
     * @param startUs           starting timestamp in microseconds
     * @return a {@link MuteVideoPlayerWithControls} wrapping the projector and controls
     */
    public static VideoPlayer createMutePlayerWithControls(String videoId, int width, int height, PlayMode startingPlayMode, EOFMode startingEOFMode, boolean keepAlive, long startUs) {
        VideoProjector projectorPlugin = new MuteVideoProjector(videoId, width, height, startingPlayMode, startingEOFMode, keepAlive, startUs);
        CustomPanelAPI projectorPanel = Global.getSettings().createCustom(width, height, projectorPlugin);

        int controlsHeight = 70;
        CustomPanelAPI masterPanel = Global.getSettings().createCustom(width, height + 5f + controlsHeight, null);
        masterPanel.addComponent(projectorPanel).inTL(0f, 0f);

        PlayerControlPanel controlPanel = new PlayerControlPanel(projectorPlugin, width, controlsHeight, null);
        masterPanel.addComponent(controlPanel.getControlPanel()).inTL(0f, height + 30f); // 30f height of seek bar panel

        return new MuteVideoPlayerWithControls(masterPanel, controlPanel, projectorPlugin, projectorPanel);
    }

    /**
     * Creates a mute-only video player with a built-in control panel and custom UI colors.
     *
     * @param videoId           id of the video asset defined in settings.json
     * @param width             panel width in pixels
     * @param height            panel height in pixels
     * @param startingPlayMode  initial playback mode
     * @param startingEOFMode   behavior when the video reaches end-of-file
     * @param keepAlive         whether to keep resources alive when not visible
     * @param textColor         color of control text/icons
     * @param bgButtonColor     background color for control buttons
     * @return a {@link MuteVideoPlayerWithControls} with custom-styled controls
     */
    public static VideoPlayer createMutePlayerWithControls(String videoId, int width, int height, PlayMode startingPlayMode, EOFMode startingEOFMode, boolean keepAlive, Color textColor, Color bgButtonColor) {
        return createMutePlayerWithControls(videoId, width, height, startingPlayMode, startingEOFMode, keepAlive, textColor, bgButtonColor, 0);
    }

    /**
     * Creates a mute-only video player with a built-in control panel and custom UI colors.
     *
     * @param videoId           id of the video asset defined in settings.json
     * @param width             panel width in pixels
     * @param height            panel height in pixels
     * @param startingPlayMode  initial playback mode
     * @param startingEOFMode   behavior when the video reaches end-of-file
     * @param keepAlive         whether to keep resources alive when not visible
     * @param textColor         color of control text/icons
     * @param bgButtonColor     background color for control buttons
     * @param startUs           starting timestamp in microseconds
     * @return a {@link MuteVideoPlayerWithControls} with custom-styled controls
     */
    public static VideoPlayer createMutePlayerWithControls(String videoId, int width, int height, PlayMode startingPlayMode, EOFMode startingEOFMode, boolean keepAlive, Color textColor, Color bgButtonColor, long startUs) {
        VideoProjector projectorPlugin = new MuteVideoProjector(videoId, width, height, startingPlayMode, startingEOFMode, keepAlive, startUs);
        CustomPanelAPI projectorPanel = Global.getSettings().createCustom(width, height, projectorPlugin);

        int controlsHeight = 70;
        CustomPanelAPI masterPanel = Global.getSettings().createCustom(width, height + 5f + controlsHeight, null);
        masterPanel.addComponent(projectorPanel).inTL(0f, 0f);

        PlayerControlPanel controlPanel = new PlayerControlPanel(projectorPlugin, width, controlsHeight, null, textColor, bgButtonColor);
        masterPanel.addComponent(controlPanel.getControlPanel()).inTL(0f, height + 30f); // 30f height of seek bar panel

        return new MuteVideoPlayerWithControls(masterPanel, controlPanel, projectorPlugin, projectorPanel);
    }

    /**
     * Creates a mute-only video player without on-screen controls.
     *
     * @param videoId           id of the video asset defined in settings.json
     * @param width             panel width in pixels
     * @param height            panel height in pixels
     * @param startingPlayMode  initial playback mode
     * @param startingEOFMode   behavior when the video reaches end-of-file
     * @param keepAlive         whether to keep resources alive when not visible

     * @return a {@link MuteVideoPlayer} embedding the projector
     */
    public static VideoPlayer createMutePlayer(String videoId, int width, int height, PlayMode startingPlayMode, EOFMode startingEOFMode, boolean keepAlive) {
        return createMutePlayer(videoId, width, height, startingPlayMode, startingEOFMode, keepAlive, 0);
    }

    /**
     * Creates a mute-only video player without on-screen controls.
     *
     * @param videoId           id of the video asset defined in settings.json
     * @param width             panel width in pixels
     * @param height            panel height in pixels
     * @param startingPlayMode  initial playback mode
     * @param startingEOFMode   behavior when the video reaches end-of-file
     * @param keepAlive         whether to keep resources alive when not visible
     * @param startUs           starting timestamp in microseconds
     * @return a {@link MuteVideoPlayer} embedding the projector
     */
    public static VideoPlayer createMutePlayer(String videoId, int width, int height, PlayMode startingPlayMode, EOFMode startingEOFMode, boolean keepAlive, long startUs) {
        VideoProjector projectorPlugin = new MuteVideoProjector(videoId, width, height, startingPlayMode, startingEOFMode, keepAlive, startUs);
        CustomPanelAPI projectorPanel = Global.getSettings().createCustom(width, height, projectorPlugin);
        
        return new MuteVideoPlayer(projectorPanel, projectorPlugin);
    }

    /**
     * Creates an audio-enabled video player without on-screen controls. Falls back to mute variant if sound is disabled.
     *
     * @param videoId           id of the video asset defined in settings.json
     * @param width             panel width in pixels
     * @param height            panel height in pixels
     * @param volume            initial playback volume in range [0, 1] - normalized to game sound volume
     * @param startingPlayMode  initial playback mode
     * @param startingEOFMode   behavior when the video reaches end-of-file
     * @param keepAlive         whether to keep resources alive when not visible
     * @return an {@link AudioVideoPlayer} or a {@link MuteVideoPlayer} if sound is disabled
     */
        public static VideoPlayer createAudioVideoPlayer(String videoId, int width, int height, float volume, PlayMode startingPlayMode, EOFMode startingEOFMode, boolean keepAlive) {
            return createAudioVideoPlayer(videoId, width, height, volume, startingPlayMode, startingEOFMode, keepAlive, 0);
        }

    /**
     * Creates an audio-enabled video player without on-screen controls. Falls back to mute variant if sound is disabled.
     *
     * @param videoId           id of the video asset defined in settings.json
     * @param width             panel width in pixels
     * @param height            panel height in pixels
     * @param volume            initial playback volume in range [0, 1] - normalized to game sound volume
     * @param startingPlayMode  initial playback mode
     * @param startingEOFMode   behavior when the video reaches end-of-file
     * @param keepAlive         whether to keep resources alive when not visible
     * @param startUs           starting timestamp in microseconds
     * @return an {@link AudioVideoPlayer} or a {@link MuteVideoPlayer} if sound is disabled
     */
    public static VideoPlayer createAudioVideoPlayer(String videoId, int width, int height, float volume, PlayMode startingPlayMode, EOFMode startingEOFMode, boolean keepAlive, long startUs) {
        if (!Global.getSettings().isSoundEnabled()) return createMutePlayer(videoId, width, height, startingPlayMode, startingEOFMode, keepAlive, startUs);
        
        VideoProjector projectorPlugin = new AudioVideoProjector(videoId, width, height, volume, startingPlayMode, startingEOFMode, keepAlive, startUs);
        CustomPanelAPI projectorPanel = Global.getSettings().createCustom(width, height, projectorPlugin);

        return new AudioVideoPlayer(projectorPanel, projectorPlugin);
    }

    /**
     * Creates an audio-enabled video player with a built-in control panel. Falls back to mute controls if sound is disabled.
     *
     * @param videoId           id of the video asset defined in settings.json
     * @param width             panel width in pixels
     * @param height            panel height in pixels
     * @param volume            initial playback volume in range [0, 1] - normalized to game sound volume
     * @param startingPlayMode  initial playback mode
     * @param startingEOFMode   behavior when the video reaches end-of-file
     * @param keepAlive         whether to keep resources alive when not visible
     * @return an {@link AudioVideoPlayerWithControls} or mute variant if sound is disabled
     */
    public static VideoPlayer createAudioVideoPlayerWithControls(String videoId, int width, int height, float volume, PlayMode startingPlayMode, EOFMode startingEOFMode, boolean keepAlive) {
        return createAudioVideoPlayerWithControls(videoId, width, height, volume, startingPlayMode, startingEOFMode, keepAlive, 0);
    }

    /**
     * Creates an audio-enabled video player with a built-in control panel. Falls back to mute controls if sound is disabled.
     *
     * @param videoId           id of the video asset defined in settings.json
     * @param width             panel width in pixels
     * @param height            panel height in pixels
     * @param volume            initial playback volume in range [0, 1] - normalized to game sound volume
     * @param startingPlayMode  initial playback mode
     * @param startingEOFMode   behavior when the video reaches end-of-file
     * @param keepAlive         whether to keep resources alive when not visible
     * @param startUs           starting timestamp in microseconds
     * @return an {@link AudioVideoPlayerWithControls} or mute variant if sound is disabled
     */
    public static VideoPlayer createAudioVideoPlayerWithControls(String videoId, int width, int height, float volume, PlayMode startingPlayMode, EOFMode startingEOFMode, boolean keepAlive, long startUs) {
        if (!Global.getSettings().isSoundEnabled()) return createMutePlayerWithControls(videoId, width, height, startingPlayMode, startingEOFMode, keepAlive, startUs);

        VideoProjector projectorPlugin = new AudioVideoProjector(videoId, width, height, volume, startingPlayMode, startingEOFMode, keepAlive, startUs);
        CustomPanelAPI projectorPanel = Global.getSettings().createCustom(width, height, projectorPlugin);
        Speakers speakers = projectorPlugin.getSpeakers();

        int controlsHeight = 70;
        CustomPanelAPI masterPanel = Global.getSettings().createCustom(width, height + 5f + controlsHeight, null);
        masterPanel.addComponent(projectorPanel).inTL(0f, 0f);

        PlayerControlPanel controlPanel = new PlayerControlPanel(projectorPlugin, width, controlsHeight, speakers);
        masterPanel.addComponent(controlPanel.getControlPanel()).inTL(0f, height + 30f); // 30f height of seek bar panel

        return new AudioVideoPlayerWithControls(masterPanel, controlPanel, speakers, projectorPlugin, projectorPanel);
    }

    /**
     * Creates an audio-enabled video player with a built-in control panel and custom UI colors.
     * Falls back to a mute variant with custom controls if sound is disabled.
     *
     * @param videoId           id of the video asset defined in settings.json
     * @param width             panel width in pixels
     * @param height            panel height in pixels
     * @param volume            initial playback volume in range [0, 1] - normalized to game sound volume
     * @param startingPlayMode  initial playback mode
     * @param startingEOFMode   behavior when the video reaches end-of-file
     * @param keepAlive         whether to keep resources alive when not visible
     * @param textColor         color of control text/icons
     * @param bgButtonColor     background color for control buttons
     * @return an {@link AudioVideoPlayerWithControls} or mute variant if sound is disabled
     */
    public static VideoPlayer createAudioVideoPlayerWithControls(String videoId, int width, int height, float volume, PlayMode startingPlayMode, EOFMode startingEOFMode, boolean keepAlive, Color textColor, Color bgButtonColor) {
        return createAudioVideoPlayerWithControls(videoId, width, height, volume, startingPlayMode, startingEOFMode, keepAlive, textColor, bgButtonColor, 0);
    }

    /**
     * Creates an audio-enabled video player with a built-in control panel and custom UI colors.
     * Falls back to a mute variant with custom controls if sound is disabled.
     *
     * @param videoId           id of the video asset defined in settings.json
     * @param width             panel width in pixels
     * @param height            panel height in pixels
     * @param volume            initial playback volume in range [0, 1] - normalized to game sound volume
     * @param startingPlayMode  initial playback mode
     * @param startingEOFMode   behavior when the video reaches end-of-file
     * @param keepAlive         whether to keep resources alive when not visible
     * @param textColor         color of control text/icons
     * @param bgButtonColor     background color for control buttons
     * @param startUs           starting timestamp in microseconds
     * @return an {@link AudioVideoPlayerWithControls} or mute variant if sound is disabled
     */
    public static VideoPlayer createAudioVideoPlayerWithControls(String videoId, int width, int height, float volume, PlayMode startingPlayMode, EOFMode startingEOFMode, boolean keepAlive, Color textColor, Color bgButtonColor, long startUs) {
        if (!Global.getSettings().isSoundEnabled()) return createMutePlayerWithControls(videoId, width, height, startingPlayMode, startingEOFMode, keepAlive, textColor, bgButtonColor, startUs);

        VideoProjector projectorPlugin = new AudioVideoProjector(videoId, width, height, volume, startingPlayMode, startingEOFMode, keepAlive, startUs);
        CustomPanelAPI projectorPanel = Global.getSettings().createCustom(width, height, projectorPlugin);
        Speakers speakers = projectorPlugin.getSpeakers();

        int controlsHeight = 70;
        CustomPanelAPI masterPanel = Global.getSettings().createCustom(width, height + 5f + controlsHeight, null);
        masterPanel.addComponent(projectorPanel).inTL(0f, 0f);

        PlayerControlPanel controlPanel = new PlayerControlPanel(projectorPlugin, width, controlsHeight, speakers, textColor, bgButtonColor);
        masterPanel.addComponent(controlPanel.getControlPanel()).inTL(0f, height + 30f); // 30f height of seek bar panel

        return new AudioVideoPlayerWithControls(masterPanel, controlPanel, speakers, projectorPlugin, projectorPanel);
    }

    /**
     * Creates an image panel backed by an {@link ImagePlugin}.
     *
     * @param imageId   id or path of the image asset
     * @param width     panel width in pixels
     * @param height    panel height in pixels
     * @param keepAlive whether to keep resources alive when not visible
     * @return an {@link ImagePanel} embedding the image plugin
     */
    public static ImagePanel createImagePanel(String imageId, int width, int height, boolean keepAlive) {
        ImagePlugin imagePlugin = new ImagePlugin(imageId, width, height, keepAlive);
        CustomPanelAPI panel = Global.getSettings().createCustom(width, height, imagePlugin);

        return new ImagePanel(panel, imagePlugin);
    }
}
