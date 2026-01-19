package videolib.console;

import videolib.VideoPlayerFactory;
import videolib.playerui.ImagePanel;
import videolib.ffmpeg.FFmpeg;
import videolib.VideoPaths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

public class VideoLibImageTest implements BaseCommand {
    private static final Logger logger = Logger.getLogger(VideoLibDemo.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    private float originalParentPanelX;
    private float originalParentPanelY;

    private String fileId = null;

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (context != CommandContext.CAMPAIGN_MAP) return CommandResult.WRONG_CONTEXT;
        List<String> splitArgs = Arrays.asList(args.split(" "));

        for (String id : VideoPaths.imageKeys()) {
            if (splitArgs.contains(id)) {
                fileId = id;
                break;
            }
        }

        InteractionDialogPlugin interactionPlugin = new InteractionDialogPlugin() {

            @Override
            public void init(InteractionDialogAPI dialog) {

                int width = 0;
                int height = 0;
        
                if (fileId != null) {
                    int[] dimensions = FFmpeg.getWidthAndHeight(VideoPaths.getImagePath(fileId));
                    width = dimensions[0];
                    height = dimensions[1];
        
                } else {
                    fileId = "vl_jpeg";
                    width = 880;
                    height = 880;
                }

                CustomPanelAPI parentPanel = showCustomPanelAndCenter(dialog, width, height);
                ImagePanel imagePanel = VideoPlayerFactory.createImagePanel(fileId, width, height, false);
                imagePanel.addTo(parentPanel).inTL(0f, 0f);
                imagePanel.init();

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
        CustomPanelAPI parentPanel = dialog.getVisualPanel().showCustomPanel(width + 202f, height, escPlugin);

        // center it
        PositionAPI pos = parentPanel.getPosition();
        originalParentPanelX = pos.getCenterX() - ((width + 202f) / 2);
        originalParentPanelY = pos.getCenterY() - (height / 2);
        
        float delta = 30;
        pos.setXAlignOffset(-delta + -width / 2);

        delta = 280;
        pos.setYAlignOffset(-delta + height / 2 );

        return parentPanel;
    }

    // private void recenter(PositionAPI parentPanelPos, int width, int height) {
    //     float delta = 30;
    //     parentPanelPos.setLocation(originalParentPanelX, originalParentPanelY);
    //     PositionAPI pos = parentPanelPos.setXAlignOffset(-delta + -width / 2);

    //     originalParentPanelX = pos.getCenterX() - ((width + 202f) / 2);
    //     originalParentPanelY = pos.getCenterY() - (height / 2);
    // }
}
