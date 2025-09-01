package data.scripts.console;

import data.scripts.VideoPlayerFactory;

import data.scripts.playerui.MuteVideoPlayer;
import data.scripts.playerui.MuteVideoPlayerWithControls;

import data.scripts.VideoModes.PlayMode;
import data.scripts.VideoModes.EOFMode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

public class VideoLibDemo implements BaseCommand {
    private static final Logger logger = Logger.getLogger(VideoLibDemo.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (context != CommandContext.CAMPAIGN_MAP) return CommandResult.WRONG_CONTEXT;
        List<String> splitArgs = Arrays.asList(args.split(" "));

        InteractionDialogPlugin interactionPlugin = new InteractionDialogPlugin() {
            @Override
            public void init(InteractionDialogAPI dialog) {                
                
                // Video width and height, should be the same as the encoded video file's resolution.
                // Width and height CAN be variable, but will incur non-negligible rescaling overhead in ffmpeg if not the same as the video's actual resolution, price especially noticeable while seeking
                // If you want to rescale the video while it is playing, you should have openGL do it by calling videoPlayer.getProjectorPanel().getPosition().setSize(width, height)
                int videoWidth = 960;
                int videoHeight = 540;

                // with controls
                if (splitArgs.contains("wc")) {                                                              // file ID defined in data/config/settings.json // starting PlayMode // starting EOFMode
                    MuteVideoPlayerWithControls videoPlayer = VideoPlayerFactory.createMutePlayerWithControls("video_lib_demo", videoWidth, videoHeight, PlayMode.PAUSED, EOFMode.LOOP);
                    videoPlayer.setClickToPause(true); // setClickToPause on the video so user can click it to pause/unpause it

                    PositionAPI masterPos = videoPlayer.getMasterPanel().getPosition();
                    int parentWidth = (int) masterPos.getWidth();
                    int parentHeight = (int) masterPos.getHeight();

                    CustomPanelAPI parentPanel = showCustomPanelAndCenter(dialog, parentWidth, parentHeight);
                    videoPlayer.addTo(parentPanel).inTL(0f, 0f);//.setXAlignOffset(-500f); // add to parent
                    videoPlayer.init(); // init projector so it knows where/height/width to render
        
                } else {                                                        // file ID defined in data/config/settings.json // starting PlayMode // starting EOFMode
                    MuteVideoPlayer videoPlayer = VideoPlayerFactory.createMutePlayer("video_lib_demo", videoWidth, videoHeight, PlayMode.PLAYING, EOFMode.LOOP);
                    videoPlayer.setClickToPause(true); // setClickToPause on the video so user can click it to pause/unpause it

                    CustomPanelAPI parentPanel = showCustomPanelAndCenter(dialog, videoWidth, videoHeight);
                    videoPlayer.addTo(parentPanel).inTL(0f,0f);//.setXAlignOffset(-500f); // add to parent
                    videoPlayer.init(); // init projector so it knows where/width/height to render
                    
                }
            }
            public void advance(float arg0) {}
            public void backFromEngagement(EngagementResultAPI arg0) {}
            public Object getContext() {return null;}
            public Map<String, MemoryAPI> getMemoryMap() {return null;}
            public void optionMousedOver(String arg0, Object arg1) {}
            public void optionSelected(String arg0, Object arg1) {}
        };

        Console.showDialogOnClose(interactionPlugin, null);

        return CommandResult.SUCCESS;
    }

    private CustomPanelAPI showCustomPanelAndCenter(InteractionDialogAPI dialog, int width, int height) {
        BaseCustomUIPanelPlugin escPlugin = new BaseCustomUIPanelPlugin() {
            @Override
            public void processInput(List<InputEventAPI> events) {
                for (int i = 0; i < events.size(); i++) {
                    InputEventAPI event = events.get(i);

                    if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                        if (dialog != null) dialog.dismiss();
                    }
                 }
            }
        };

        // parent
        CustomPanelAPI parentPanel = dialog.getVisualPanel().showCustomPanel(width, height, escPlugin);

        // center it
        PositionAPI pos = parentPanel.getPosition();
        // float displayCenterY = (int) Global.getSettings().getScreenHeightPixels() / 2;
        // float parentPanelX = pos.getX();
        
        float delta = 30; // the distance between the parentPanelX and the center of the display is always the same apparently
        pos.setXAlignOffset(-delta + -width / 2);

        return parentPanel;
    }
}
