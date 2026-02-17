package videolib;

import com.fs.graphics.Sprite;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;
import videolib.decoder.grouped.DeltaTimeDelegator;
import videolib.entities.CampaignBillboard;
import videolib.entities.CampaignBillboardPlugin;
import videolib.ffmpeg.FFmpeg;
import videolib.playerui.VideoPlayer;
import videolib.projector.AutoTexProjector.AutoTexProjectorAPI;

import static videolib.util.UiUtil.utils;
import static videolib.VideoLibModPlugin.print;

import java.util.List;

import org.lwjgl.input.Keyboard;

public class VideoLibCampaignListener extends BaseCampaignEventListener {
    private VideoLibCampaignListener() {
        super(false);
    }

    private static VideoLibCampaignListener instance;

    public static VideoLibCampaignListener getInstance() {
        if (instance != null) {
            Global.getSector().removeListener(instance);
        }
        return instance = new VideoLibCampaignListener();
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        for (AutoTexProjectorAPI projector : VideoPaths.getAutoTexProjectorsWithCampaignSpeedup())  {
            ((DeltaTimeDelegator)projector.getDecoder()).setCampaign();
        }
    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        if (dialog.getInteractionTarget() != null && dialog.getInteractionTarget().getCustomPlugin() instanceof CampaignBillboardPlugin plugin) {
            CampaignBillboard billboard = plugin.getBillboard();

            if (billboard.getInteractionDialogDelegate() != null) {
                billboard.getInteractionDialogDelegate().execute(dialog, billboard);
                return;
            }

            AutoTexProjectorAPI projector = billboard.getTexProjector();
            if (projector != null) {
                showVideoCenter(dialog, billboard);
            }
        }
    }

    public static void showVideoCenter(InteractionDialogAPI dialog, CampaignBillboard billboard) {
        OptionPanelAPI optionPanel = dialog.getOptionPanel();
        optionPanel.clearOptions();

        UIPanelAPI panel = (UIPanelAPI) dialog;
        List<UIComponentAPI> children = utils.getChildrenCopy(panel);

        for (UIComponentAPI child : children) {
            if (child instanceof LabelAPI label) {
                label.setOpacity(0);
                break;
            }
        }
        
        int[] dimensions = FFmpeg.getWidthAndHeight(billboard.getTexProjector().getDecoder().getVideoFilePath());
        int width = dimensions[0];
        int height = dimensions[1];
        float finalScaleFactor = 1f;

        float screenWidth = Global.getSettings().getScreenWidthPixels();
        float screenHeight = Global.getSettings().getScreenHeightPixels();
        float scaleFactor = 0.8f;

        if (width > screenWidth * scaleFactor || height > screenHeight * scaleFactor) {
            finalScaleFactor = 0.7f;
            float[] dimensionsf = getScaledDimensions(width, height, screenWidth, screenHeight, finalScaleFactor);
            width = (int)dimensionsf[0];
            height = (int)dimensionsf[1];
        }

        VideoPlayer videoPlayer = VideoPlayerFactory.createMutePlayer(
            VideoPaths.getVideoId(billboard.getTexProjector().getDecoder().getVideoFilePath()),
            width, height,
            PlayMode.PLAYING, EOFMode.LOOP,
            false,
            billboard.getTexProjector().getDecoder().getCurrentVideoPts()
        );
        videoPlayer.setClickToPause(false);

        CustomPanelAPI parentPanel = showCustomPanelAndCenter(
            dialog,
            videoPlayer,
            width,
            height,
            finalScaleFactor,
            billboard
        );
        videoPlayer.addTo(parentPanel).inTL(0f,0f);
        videoPlayer.init(); // init projector so it knows where/width/height to render
        return;
    }

    public static CustomPanelAPI showCustomPanelAndCenter(
        InteractionDialogAPI dialog,
        VideoPlayer player,
        int width,
        int height,
        float scaleFactor,
        CampaignBillboard billboard
    ) {
        BaseCustomUIPanelPlugin escPlugin = new BaseCustomUIPanelPlugin() {
            @Override
            public void processInput(List<InputEventAPI> events) {
                for (InputEventAPI event : events) {
                    if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                        if (dialog != null) {
                            if (billboard != null && billboard.getTexProjector().getDecoder().isRunning()) {
                                billboard.getTexProjector().getDecoder().seek(player.getDecoder().getCurrentVideoPts());
                            }
                            dialog.dismiss();
                            return;
                        }
                    }
                }
            }
        };

        CustomPanelAPI parentPanel = dialog.getVisualPanel().showCustomPanel(width, height, escPlugin);

        PositionAPI pos = parentPanel.getPosition();

        float baseXDelta = 30f;
        float baseYDelta = 280f;
        
        float[] ourOffsets = billboard != null ? getScaledOffsets(width, height, new Sprite(billboard.getCustomEntitySpec().getSpriteName())) : new float[] {0f, 0f};

        pos.setXAlignOffset(-baseXDelta + ourOffsets[0] - width / 2f);
        pos.setYAlignOffset(-baseYDelta + ourOffsets[1] + height / 2f);

        return parentPanel;
    }

    public static float[] getScaledOffsets(int ourWidth, int ourHeight, Sprite sprite) {
        float contentWidth = sprite.getWidth();
        float contentHeight = sprite.getHeight();
    
        float texWidthRatio = sprite.getTexWidth();
        float texHeightRatio = sprite.getTexHeight();

        float textureWidth = contentWidth / texWidthRatio;
        float textureHeight = contentHeight / texHeightRatio;

        float padX = textureWidth - contentWidth;
        float padY = textureHeight - contentHeight;
    
        float scaleX = ourWidth / textureWidth;
        float scaleY = ourHeight / textureHeight;
    
        return new float[] {
            (padX * scaleX) / 2,
            (padY * scaleY) / 2
        };
    }

    public static float[] getScaledDimensions(int width, int height, float screenWidth, float screenHeight, float scaleFactor) {
        float clampedScale = Math.max(0f, Math.min(1f, scaleFactor));

        float maxAllowedWidth = screenWidth * clampedScale;
        float maxAllowedHeight = screenHeight * clampedScale;

        float widthRatio = maxAllowedWidth / width;
        float heightRatio = maxAllowedHeight / height;

        float finalScale = Math.min(widthRatio, heightRatio);

        finalScale = Math.min(finalScale, 1f);
    
        int scaledWidth = Math.round(width * finalScale);
        int scaledHeight = Math.round(height * finalScale);
    
        return new float[] { scaledWidth, scaledHeight, finalScale};
    }
}
