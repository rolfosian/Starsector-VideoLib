package videolib;

import org.lwjgl.input.Keyboard;

import com.fs.graphics.Sprite;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ObjectiveEventListener;

import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;

import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.Objectives;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;
import videolib.decoder.grouped.DeltaTimeDelegator;
import videolib.entities.CampaignBillboard;
import videolib.entities.CampaignBillboardPlugin;
import videolib.ffmpeg.FFmpeg;
import videolib.playerui.VideoPlayer;
import videolib.projector.AutoTexProjector.AutoTexProjectorAPI;
import videolib.util.TexReflection;

import static videolib.util.UiUtil.utils;
import static videolib.VideoLibModPlugin.print;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.*;

class Objs extends Objectives {
    public Objs(InteractionDialogAPI dialog, OptionPanelAPI optionPanel, TextPanelAPI textPanel, SectorEntityToken entity) {
        super(entity);
        this.dialog = dialog;
        this.options = optionPanel;
        this.text = textPanel;
    }
}

public class VideoLibCampaignListener extends BaseCampaignEventListener implements ObjectiveEventListener {

    private static final VarHandle[] ruleBasedInteractionDialogPluginImplVarHandles;

    static {
        try {
            List<VarHandle> handles = new ArrayList<>();
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(RuleBasedInteractionDialogPluginImpl.class, lookup);

            for (Object field : RuleBasedInteractionDialogPluginImpl.class.getDeclaredFields()) {
                int mods = TexReflection.getFieldModifiers(field);
                if (TexReflection.isStatic(mods) || TexReflection.isFinal(mods)) continue;

                handles.add(
                    privateLookup.findVarHandle(
                        RuleBasedInteractionDialogPluginImpl.class,
                        TexReflection.getFieldName(field),
                        TexReflection.getFieldType(field)
                    )
                );
            }

            ruleBasedInteractionDialogPluginImplVarHandles = handles.toArray(new VarHandle[0]);

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    public static void transplant(RuleBasedInteractionDialogPluginImpl original, RuleBasedInteractionDialogPluginImpl destination) {
        for (VarHandle handle : ruleBasedInteractionDialogPluginImplVarHandles) handle.set(destination, handle.get(original));
    }

    public VideoLibCampaignListener() {
        super(false);
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

            if (billboard.isContested()) {
                handleHome(dialog, billboard, true);

                RuleBasedInteractionDialogPluginImpl ours = new RuleBasedInteractionDialogPluginImpl() {
                    @Override
                    public void optionSelected(String text, Object optionData) {
                        OptionPanelAPI optionPanel = dialog.getOptionPanel();
                        TextPanelAPI textPanel = dialog.getTextPanel();

                        switch(String.valueOf(optionData)) {
                            case "TAKE_CONTROL":
                                textPanel.addPara(text, Global.getSector().getPlayerFaction().getBrightUIColor());

                                textPanel.addPara("Taking control of the " + billboard.getName() + " billboard will grant your in-system fleets and colonies the \"benefits\" it provides.");
                                textPanel.addPara(billboard.getFaction().getDisplayNameWithArticle() + " will certainly regard such a takeover of \"vital\" infrastructure as an act of war.");

                                optionPanel.clearOptions();
                                optionPanel.addOption("Proceed", "TAKE_CONTROL_CONFIRM");
                                optionPanel.addOption("Never mind", "BACK_HOME");
                                break;

                            case "TAKE_CONTROL_CONFIRM":
                                textPanel.addPara(text, Global.getSector().getPlayerFaction().getBrightUIColor());

                                new Objs(dialog, optionPanel, textPanel, dialog.getInteractionTarget()).control(Factions.PLAYER);
                                textPanel.addPara("Your crews quickly complete a physical takeover of the structure, removing automated safeguards and installing black-box transmitters tuning its output frequency to your transponder codes.");
                                handleHome(dialog, billboard, false);
                                break;

                            case "BACK_HOME":
                                textPanel.addPara(text, Global.getSector().getPlayerFaction().getBrightUIColor());
                                handleHome(dialog, billboard, false);
                                break;

                            case "leave":
                                dialog.dismiss();
                                break;
                            default:
                                break;
                        }
                    }
                };
                transplant((RuleBasedInteractionDialogPluginImpl)dialog.getPlugin(), ours);
                dialog.setPlugin(ours);
                return;
            }

            AutoTexProjectorAPI projector = billboard.getTexProjector();
            if (projector != null) {
                // showVideoCenter(dialog, billboard);
            }
        }
    }

    private static void handleHome(InteractionDialogAPI dialog, CampaignBillboard billboard, boolean fresh) {
        String factionId = billboard.getFaction().getId();
        String factionDisplayName = billboard.getFaction().getDisplayName();
        String billboardName = billboard.getName();

        OptionPanelAPI optionPanel = dialog.getOptionPanel();
        optionPanel.clearOptions();

        if (!factionId.equals(Factions.PLAYER)) {
            optionPanel.addOption("Take control of the " + billboardName , "TAKE_CONTROL");
            optionPanel.addOption("Leave", "leave");
            optionPanel.setShortcut("leave", Keyboard.KEY_ESCAPE, false, false, false, false);
            
            if (fresh) {
                dialog.getTextPanel().clear();
                dialog.getTextPanel().addPara("The " + billboardName + " is under %s control.", billboard.getFaction().getBaseUIColor(), factionDisplayName);

                if (isFactionFleetNearbyAndAware(factionId)) {
                    dialog.getTextPanel().addPara("A nearby " + factionDisplayName + " fleet is tracking your movements, making interfering with the " + billboardName + " impossible.");
                    for (UIComponentAPI child : utils.getChildrenNonCopy(optionPanel)) {
                        if (child instanceof ButtonAPI button) {
                            if (!button.getText().contains("Leave [Esc]")) button.setEnabled(false);
                        }
                    }
                } else if (isHostileFleetNearbyAndAware()) {
                    dialog.getTextPanel().addPara("A nearby hostile fleet is tracking your movements, making interfering with the " + billboardName + " impossible.");
                    for (UIComponentAPI child : utils.getChildrenNonCopy(optionPanel)) {
                        if (child instanceof ButtonAPI button) {
                            if (!button.getText().contains("Leave [Esc]")) button.setEnabled(false);
                        }
                    }
                }
            }
            return;
        }

        if (fresh) {
            dialog.getTextPanel().clear();
            dialog.getTextPanel().addPara("The " + billboardName + " billboard is under %s control.", billboard.getFaction().getBaseUIColor(), factionDisplayName);
        }

        optionPanel.addOption("Leave", "leave");
        optionPanel.setShortcut("leave", Keyboard.KEY_ESCAPE, false, false, false, false);

        return;
    }

    @Override
    public void reportObjectiveChangedHands(SectorEntityToken entity, FactionAPI from, FactionAPI to) {
        if (entity.getCustomPlugin() instanceof CampaignBillboardPlugin plugin) {
            CampaignBillboard billboard = plugin.getBillboard();
            billboard.setFaction(to.getId());
        }
    }

    @Override
    public void reportObjectiveDestroyed(SectorEntityToken entity, SectorEntityToken stableLocation, FactionAPI to) {
        // if (entity.getCustomPlugin() instanceof CampaignBillboardPlugin plugin) {
        //     stableLocation.getContainingLocation().removeEntity(stableLocation);
        // }
    }

    private static boolean isHostileFleetNearbyAndAware() {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        for (CampaignFleetAPI fleet : playerFleet.getContainingLocation().getFleets()) {
            if (fleet.getAI() == null) continue; // dormant Remnant fleets
            if (fleet.getFaction().isPlayerFaction()) continue;
            if (fleet.isStationMode()) continue;
            
            if (!fleet.isHostileTo(playerFleet)) continue;
            if (fleet.getBattle() != null) continue;
            
            if (Misc.isInsignificant(fleet)) {
                continue;
            }
            
            VisibilityLevel level = playerFleet.getVisibilityLevelTo(fleet);

            if (level == VisibilityLevel.NONE) continue;
            
            if (fleet.getFleetData().getMembersListCopy().isEmpty()) continue;
            
            float dist = Misc.getDistance(playerFleet.getLocation(), fleet.getLocation());
            if (dist > 1500f) continue;

            if (fleet.getAI() instanceof ModularFleetAIAPI) {
                ModularFleetAIAPI ai = (ModularFleetAIAPI) fleet.getAI();
                if (ai.getTacticalModule() != null && 
                        (ai.getTacticalModule().isFleeing() || ai.getTacticalModule().isMaintainingContact() ||
                                ai.getTacticalModule().isStandingDown())) {
                    continue;
                }
            }
            
            return true;
        }
        return false;
    }

    private static boolean isFactionFleetNearbyAndAware(String factionId) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        for (CampaignFleetAPI fleet : playerFleet.getContainingLocation().getFleets()) {
            if (fleet.getFaction().isPlayerFaction()) continue;
            
            if (!fleet.getFaction().getId().equals(factionId)) continue;
            if (fleet.getBattle() != null) continue;
            if (fleet.isStationMode()) continue;
            
            
            VisibilityLevel level = playerFleet.getVisibilityLevelTo(fleet);
            if (level == VisibilityLevel.NONE) continue;
            
            if (fleet.getFleetData().getMembersListCopy().isEmpty()) continue;
            
            float dist = Misc.getDistance(playerFleet.getLocation(), fleet.getLocation());
            if (dist > 750f) continue;

            if (fleet.getAI() instanceof ModularFleetAIAPI) {
                ModularFleetAIAPI ai = (ModularFleetAIAPI) fleet.getAI();
                if (ai.getTacticalModule() != null && 
                        (ai.getTacticalModule().isFleeing() || ai.getTacticalModule().isMaintainingContact() ||
                                ai.getTacticalModule().isStandingDown())) {
                    continue;
                }
            }
            
            return true;
        }

        return false;
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
