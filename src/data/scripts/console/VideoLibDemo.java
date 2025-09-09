package data.scripts.console;

import data.scripts.VideoPlayerFactory;

import data.scripts.playerui.MuteVideoPlayer;
import data.scripts.playerui.MuteVideoPlayerWithControls;
import data.scripts.playerui.VideoPlayer;
import data.scripts.VideoModes.PlayMode;
import data.scripts.ffmpeg.FFmpeg;
import data.scripts.VideoPaths;
import data.scripts.VideoModes.EOFMode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import java.awt.Color;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.ui.UITable;
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

    private float originalParentPanelX;
    private VideoPlayer videoPlayer;
    private String fileId = null;

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (context != CommandContext.CAMPAIGN_MAP) return CommandResult.WRONG_CONTEXT;
        List<String> splitArgs = Arrays.asList(args.split(" "));

        for (String id : VideoPaths.videoKeys()) {
            if (splitArgs.contains(id)) {
                fileId = id;
                break;
            }
        }

        final Color[] controlsColor = new Color[1];
        final Color[] controlsTextColor = new Color[1];

        if (splitArgs.contains("wc")) { // with controls
            for (String arg : splitArgs) {
                if (arg.startsWith("color:")) {
                    String[] colorArgs = arg.split(":")[1].split(",");
                    if (colorArgs.length > 3 || colorArgs.length < 3) throw new IllegalArgumentException("Color parameter must be \"color:R,G,B\" format format as integer values");

                    for (String colorArg : colorArgs) {
                        for (char c : colorArg.toCharArray())
                        if (!Character.isDigit(c)) throw new IllegalArgumentException("Color parameter must be \"color:R,G,B\" format as integer values");
                    }

                    int red = Integer.parseInt(colorArgs[0]);
                    int green = Integer.parseInt(colorArgs[1]);
                    int blue = Integer.parseInt(colorArgs[2]);
                    
                    controlsColor[0] = new Color(red, green, blue);

                } else if (arg.startsWith("textcolor:")) {
                    String[] colorArgs = arg.split(":")[1].split(",");
                    if (colorArgs.length > 3 || colorArgs.length < 3) throw new IllegalArgumentException("Text Color parameter must be \"textcolor:R,G,B\" format as integer values");

                    for (String colorArg : colorArgs) {
                        for (char c : colorArg.toCharArray())
                        if (!Character.isDigit(c)) throw new IllegalArgumentException("Text Color parameter must be \"textcolor:R,G,B\" format as integer values");
                    }

                    int red = Integer.parseInt(colorArgs[0]);
                    int green = Integer.parseInt(colorArgs[1]);
                    int blue = Integer.parseInt(colorArgs[2]);
                    
                    controlsTextColor[0] = new Color(red, green, blue);

                }
                
            }

            if (controlsTextColor[0] == null) controlsTextColor[0] = Misc.getTextColor();
            if (controlsColor[0] == null) controlsColor[0] = Misc.getDarkPlayerColor();
        }

        final int[] argWidth = new int[1];
        argWidth[0] = 0;
        final int[] argHeight = new int[1];
        argHeight[0] = 0;
        for (String arg : splitArgs) {
            if (arg.startsWith("width:")) {
                argWidth[0] = Integer.parseInt(arg.split(":")[1]);

            } else if (arg.startsWith("height:")) {
                argHeight[0] = Integer.parseInt(arg.split(":")[1]);
            }
        }

        InteractionDialogPlugin interactionPlugin = new InteractionDialogPlugin() {
            private String currentVideoId;

            @Override
            public void init(InteractionDialogAPI dialog) {
                CustomPanelAPI parentPanel;
                // ***THE MEAT AND POTATOES***

                // Video width and height, should be the same as the encoded video file's resolution.
                // Width and height CAN be variable, but will incur non-negligible rescaling overhead in ffmpeg if not the same as the video's actual resolution, price especially noticeable while seeking
                // If you want to rescale the video while it is playing, you should have openGL do it by calling videoPlayer.getProjectorPanel().getPosition().setSize(width, height)
                int videoWidth;
                int videoHeight;

                if (argWidth[0] != 0 && argHeight[0] != 0) {
                    videoWidth = argWidth[0];
                    videoHeight = argHeight[0];

                } else if (fileId == null) {
                    videoWidth = 960;
                    videoHeight = 540;
                    fileId = "vl_demo";

                } else {
                    int[] dimensions = FFmpeg.getWidthAndHeight(VideoPaths.getVideoPath(fileId));
                    videoWidth = dimensions[0];
                    videoHeight = dimensions[1];
                }
                
                
                if (splitArgs.contains("ws")) { // with sound
                    // with controls
                    if (splitArgs.contains("wc")) {
                                                                            // file ID defined in data/config/settings.json | vol |  starting PlayMode | starting EOFMode
                        videoPlayer = VideoPlayerFactory.createAudioVideoPlayerWithControls(fileId, videoWidth, videoHeight, 0.1f, PlayMode.PAUSED, EOFMode.LOOP, false, controlsTextColor[0], controlsColor[0]);
                        videoPlayer.setClickToPause(true); // setClickToPause on the video so user can click it to pause/unpause it

                        PositionAPI masterPos = videoPlayer.getMasterPanel().getPosition();
                        int parentWidth = (int) masterPos.getWidth();
                        int parentHeight = (int) masterPos.getHeight();

                        parentPanel = showCustomPanelAndCenter(dialog, parentWidth, parentHeight);
                        videoPlayer.addTo(parentPanel).inTL(0f, 0f); // add to parent
                        videoPlayer.init(); // init projector and controls so they know where/height/width to render

                    } else {                                 // file ID defined in data/config/settings.json | vol | starting PlayMode | starting EOFMode
                        videoPlayer = VideoPlayerFactory.createAudioVideoPlayer(fileId, videoWidth, videoHeight, 0.1f, PlayMode.PLAYING, EOFMode.LOOP, false);
                        videoPlayer.setClickToPause(true); // setClickToPause on the video so user can click it to pause/unpause it

                        parentPanel = showCustomPanelAndCenter(dialog, videoWidth, videoHeight);
                        videoPlayer.addTo(parentPanel).inTL(0f,0f); // add to parent
                        videoPlayer.init(); // init projector so it knows where/width/height to render
                    }

                } else { // no sound
                    // with controls
                    if (splitArgs.contains("wc")) {
                                                                    // file ID defined in data/config/settings.json | starting PlayMode | starting EOFMode | keepAlive?
                        videoPlayer = VideoPlayerFactory.createMutePlayerWithControls(fileId, videoWidth, videoHeight, PlayMode.PAUSED, EOFMode.LOOP, false, controlsTextColor[0], controlsColor[0]);
                        videoPlayer.setClickToPause(true); // setClickToPause on the video so user can click it to pause/unpause it

                        PositionAPI masterPos = videoPlayer.getMasterPanel().getPosition();
                        int parentWidth = (int) masterPos.getWidth();
                        int parentHeight = (int) masterPos.getHeight();

                        parentPanel = showCustomPanelAndCenter(dialog, parentWidth, parentHeight);
                        videoPlayer.addTo(parentPanel).inTL(0f, 0f); // add to parent
                        videoPlayer.init(); // init projector and controls so they know where/height/width to render

                    } else {                                 // file ID defined in data/config/settings.json | starting PlayMode | starting EOFMode | keepAlive?
                        videoPlayer = VideoPlayerFactory.createMutePlayer(fileId, videoWidth, videoHeight, PlayMode.PLAYING, EOFMode.LOOP, false);
                        videoPlayer.setClickToPause(true); // setClickToPause on the video so user can click it to pause/unpause it

                        parentPanel = showCustomPanelAndCenter(dialog, videoWidth, videoHeight);
                        videoPlayer.addTo(parentPanel).inTL(0f,0f); // add to parent
                        videoPlayer.init(); // init projector so it knows where/width/height to render
                    }
                }


                // ignore everything below this line if you value your sanity
                currentVideoId = fileId;
                // May revisit this at some stage, too much debug required, repositioning is obtuse
                if (videoPlayer instanceof MuteVideoPlayer) {
                    Color c1 = Global.getSettings().getBasePlayerColor();
                    Color c2 = Global.getSettings().getDarkPlayerColor();
    
                    TooltipMakerAPI tt = parentPanel.createUIElement(202f, videoHeight, true);
                    UITable table = (UITable) tt.beginTable(c1, c2, Misc.getHighlightedOptionColor(), 30f, false, true,
                    new Object[]{"Available Videos by Id", 200f});
    
                    String[] videoFileIds = VideoPaths.videoKeys();
                    float yOffset = 0f;
                    
                    for (String fileId : videoFileIds) {
                        UIPanelAPI row = (UIPanelAPI) tt.addRowWithGlow(c1, fileId);
    
                        PositionAPI rowPos = row.getPosition();
                        
                        CustomPanelAPI overlayPanel = Global.getSettings().createCustom(200f, 29f, new BaseCustomUIPanelPlugin() {
                            @Override
                            public void buttonPressed(Object buttonId) {
                                if (!currentVideoId.equals(fileId)) {
                                    currentVideoId = fileId;
                                    String path = VideoPaths.getVideoPath(fileId);
    
                                    int[] size = FFmpeg.getWidthAndHeight(path);
    
                                    // if (videoPlayer instanceof MuteVideoPlayer) {
                                        videoPlayer.openNewVideo(fileId, size[0], size[1]);
                                        recenter(parentPanel.getPosition(), size[0]);
    
                                    // } else if (videoPlayer instanceof MuteVideoPlayerWithControls) {
                                    //     PlayMode mode = videoPlayer.getProjector().getPlayMode();
                                    //     EOFMode eofMode = videoPlayer.getProjector().getEOFMode();
    
                                    //     tt.removeComponent((UIComponentAPI) table);
                                    //     parentPanel.removeComponent(tt);
                                    //     parentPanel.removeComponent(videoPlayer.getMasterPanel()); // will stop and clean up automatically
    
                                    //     videoPlayer = VideoPlayerFactory.createMutePlayerWithControls(fileId, size[0], size[1], mode, eofMode);
                                    //     videoPlayer.setClickToPause(true);
    
                                    //     videoPlayer.addTo(parentPanel).inTL(0f, 0f); // add to parent
                                    //     videoPlayer.init();
                                    //     parentPanel.addComponent(tt).rightOfTop((UIComponentAPI)videoPlayer.getMasterPanel(), 0f);
                                    // }
                                }
                            };
                        });
                        
                        TooltipMakerAPI buttonHolder = overlayPanel.createUIElement(200f, 29f, false);
                        ButtonAPI button = buttonHolder.addButton("", "", 200f, 29f, 0f);
                        button.setButtonPressedSound(null);
                        button.setMouseOverSound(null);
                        button.setOpacity(0.1f);
                        overlayPanel.addUIElement(buttonHolder);
    
                        yOffset = yOffset == 0 ? yOffset + 29f: yOffset + 31f;
                        tt.addComponent(overlayPanel).inTL(rowPos.getX(), yOffset);
                    }
    
                    tt.addTable("", 0, 1f);
                    parentPanel.addUIElement(tt).rightOfTop((UIComponentAPI)videoPlayer.getMasterPanel(), 0f);
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
        CustomPanelAPI parentPanel = dialog.getVisualPanel().showCustomPanel(width + 202f, height, escPlugin);

        // center it
        PositionAPI pos = parentPanel.getPosition();
        // float displayCenterY = (int) Global.getSettings().getScreenHeightPixels() / 2;
        originalParentPanelX = pos.getX();
        
        float delta = 30; // the distance between the parentPanelX and the center of the display is always the same apparently
        pos.setXAlignOffset(-delta + -width / 2);

        return parentPanel;
    }

    private void recenter(PositionAPI parentPanelPos, int width) {
        float delta = 30; // the distance between the parentPanelX and the center of the display is always the same apparently
        parentPanelPos.setLocation(originalParentPanelX, parentPanelPos.getY());
        parentPanelPos.setXAlignOffset(-delta + -width / 2);
    }
}
