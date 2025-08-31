package data.scripts.console;

import data.scripts.VideoPlayerFactory;
import data.scripts.playerui.VideoPlayerWithControls;
import data.scripts.projector.MuteVideoProjector;
import data.scripts.VideoModes.PlayMode;
import data.scripts.VideoModes.EOFMode;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.ui.CustomPanelAPI;
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
        if (!(context == CommandContext.CAMPAIGN_MAP)) return CommandResult.WRONG_CONTEXT;
        
        InteractionDialogPlugin interactionPlugin = new InteractionDialogPlugin() {
            @Override
            public void init(InteractionDialogAPI dialog) {
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

                // Video width and height (can be variable, but probably keep aspect ratio)
                float width = 960f; // video width 
                float height = 540f; // video height

                // parent
                CustomPanelAPI panel = dialog.getVisualPanel().showCustomPanel(width, height, escPlugin);

                if (args.contains("controls")) {
                    VideoPlayerWithControls videoPlayer = VideoPlayerFactory.createMutePlayerWithControls("video_lib_demo", (int)width, (int)height, PlayMode.PLAYING, EOFMode.LOOP);
                    videoPlayer.setClickToPause(true);
                    videoPlayer.addTo(panel).inTL(0f, 0f).setXAlignOffset(-500f); // add to parent
                    videoPlayer.init(); // init projector so it knows where/height/width to render, starts implicitly
        
                } else {
                    MuteVideoProjector projector = new MuteVideoProjector("video_lib_demo", (int)width, (int)height, PlayMode.PLAYING, EOFMode.LOOP); // Create our projector
                    CustomPanelAPI projectorPanel = Global.getSettings().createCustom(width, height, projector); // Its panel
                    panel.addComponent(projectorPanel).inTL(0f,0f).setXAlignOffset(-500f); // add the projector panel to its parent

                    projector.init(projectorPanel.getPosition(), projectorPanel); // init projector so it knows where/height/width to render
                    projector.setClickToPause(true); // setClickToPause on the video so user has some basic semblance of control
                    projector.setIsRendering(true); // DONT FORGET TO ENABLE RENDERING ON THE PROJECTOR LMAO!!!!
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
}
