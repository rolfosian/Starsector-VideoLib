package videolib.entities;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import java.awt.Color;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.InteractionDialogImageVisual;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityPlugin;
import com.fs.starfarer.api.campaign.CustomEntitySpecAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.OrbitAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import com.fs.starfarer.campaign.CustomCampaignEntity;

public class CustomCampaignEntityWrapper implements CustomCampaignEntityAPI {
    protected final CustomCampaignEntity entity;

    public CustomCampaignEntityWrapper(CustomCampaignEntity entity) {
        this.entity = entity;
    }

   @Override
    public void addAbility(String arg0) {
        entity.addAbility(arg0);
    }

    @Override
    public void addDropRandom(DropData arg0) {
        entity.addDropRandom(arg0);
    }

    @Override
    public void addDropRandom(String arg0, int arg1) {
        entity.addDropRandom(arg0, arg1);
    }

    @Override
    public void addDropRandom(String arg0, int arg1, int arg2) {
        entity.addDropRandom(arg0, arg1, arg2);
    }

    @Override
    public void addDropValue(DropData arg0) {
        entity.addDropValue(arg0);
    }

    @Override
    public void addDropValue(String arg0, int arg1) {
        entity.addDropValue(arg0, arg1);
    }

    @Override
    public void addFloatingText(String arg0, Color arg1, float arg2) {
        entity.addFloatingText(arg0, arg1, arg2);
    }

    @Override
    public void addFloatingText(String arg0, Color arg1, float arg2, boolean arg3) {
        entity.addFloatingText(arg0, arg1, arg2, arg3);
    }

    @Override
    public void addScript(EveryFrameScript arg0) {
        entity.addScript(arg0);
    }

    @Override
    public void addTag(String arg0) {
        entity.addTag(arg0);
    }

    @Override
    public void advance(float arg0) {
        entity.advance(arg0);
    }

    @Override
    public void autoUpdateHyperLocationBasedOnInSystemEntityAtRadius(SectorEntityToken arg0, float arg1) {
        entity.autoUpdateHyperLocationBasedOnInSystemEntityAtRadius(arg0, arg1);
    }

    @Override
    public void clearAbilities() {
        entity.clearAbilities();
    }

    @Override
    public void clearFloatingText() {
        entity.clearFloatingText();
    }

    @Override
    public void clearTags() {
        entity.clearTags();
    }

    @Override
    public void fadeInIndicator() {
        entity.fadeInIndicator();
    }

    @Override
    public void fadeOutIndicator() {
        entity.fadeOutIndicator();
    }

    @Override
    public void forceOutIndicator() {
        entity.forceOutIndicator();
    }

    @Override
    public void forceSensorContactFaderBrightness(float arg0) {
        entity.forceSensorContactFaderBrightness(arg0);
    }

    @Override
    public void forceSensorFaderBrightness(float arg0) {
        entity.forceSensorFaderBrightness(arg0);
    }

    @Override
    public void forceSensorFaderOut() {
        entity.forceSensorFaderOut();
    }

    @Override
    public Map<String, AbilityPlugin> getAbilities() {
        return entity.getAbilities();
    }

    @Override
    public AbilityPlugin getAbility(String arg0) {
        return entity.getAbility(arg0);
    }

    @Override
    public PersonAPI getActivePerson() {
        return entity.getActivePerson();
    }

    @Override
    public Boolean getAlwaysUseSensorFaderBrightness() {
        return entity.getAlwaysUseSensorFaderBrightness();
    }

    @Override
    public String getAutogenJumpPointNameInHyper() {
        return entity.getAutogenJumpPointNameInHyper();
    }

    @Override
    public float getBaseSensorRangeToDetect(float arg0) {
        return entity.getBaseSensorRangeToDetect(arg0);
    }

    @Override
    public CargoAPI getCargo() {
        return entity.getCargo();
    }

    @Override
    public float getCircularOrbitAngle() {
        return entity.getCircularOrbitAngle();
    }

    @Override
    public float getCircularOrbitPeriod() {
        return entity.getCircularOrbitPeriod();
    }

    @Override
    public float getCircularOrbitRadius() {
        return entity.getCircularOrbitRadius();
    }

    @Override
    public Constellation getConstellation() {
        return entity.getConstellation();
    }

    @Override
    public LocationAPI getContainingLocation() {
        return entity.getContainingLocation();
    }

    @Override
    public Map<String, Object> getCustomData() {
        return entity.getCustomData();
    }

    @Override
    public String getCustomDescriptionId() {
        return entity.getCustomDescriptionId();
    }

    @Override
    public CustomEntitySpecAPI getCustomEntitySpec() {
        return entity.getCustomEntitySpec();
    }

    @Override
    public String getCustomEntityType() {
        return entity.getCustomEntityType();
    }

    @Override
    public InteractionDialogImageVisual getCustomInteractionDialogImageVisual() {
        return entity.getCustomInteractionDialogImageVisual();
    }

    @Override
    public CustomCampaignEntityPlugin getCustomPlugin() {
        return entity.getCustomPlugin();
    }

    @Override
    public StatBonus getDetectedRangeMod() {
        return entity.getDetectedRangeMod();
    }

    @Override
    public Float getDetectionRangeDetailsOverrideMult() {
        return entity.getDetectionRangeDetailsOverrideMult();
    }

    @Override
    public Float getDiscoveryXP() {
        return entity.getDiscoveryXP();
    }

    @Override
    public List<DropData> getDropRandom() {
        return entity.getDropRandom();
    }

    @Override
    public List<DropData> getDropValue() {
        return entity.getDropValue();
    }

    @Override
    public float getExtendedDetectedAtRange() {
        return entity.getExtendedDetectedAtRange();
    }

    @Override
    public float getFacing() {
        return entity.getFacing();
    }

    @Override
    public FactionAPI getFaction() {
        return entity.getFaction();
    }

    @Override
    public String getFullName() {
        return entity.getFullName();
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public Color getIndicatorColor() {
        return entity.getIndicatorColor();
    }

    @Override
    public Color getLightColor() {
        return entity.getLightColor();
    }

    @Override
    public SectorEntityToken getLightSource() {
        return entity.getLightSource();
    }

    @Override
    public Vector2f getLocation() {
        return entity.getLocation();
    }

    @Override
    public Vector2f getLocationInHyperspace() {
        return entity.getLocationInHyperspace();
    }

    @Override
    public MarketAPI getMarket() {
        return entity.getMarket();
    }

    @Override
    public float getMaxSensorRangeToDetect(SectorEntityToken arg0) {
        return entity.getMaxSensorRangeToDetect(arg0);
    }

    @Override
    public MemoryAPI getMemory() {
        return entity.getMemory();
    }

    @Override
    public MemoryAPI getMemoryWithoutUpdate() {
        return entity.getMemoryWithoutUpdate();
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public OrbitAPI getOrbit() {
        return entity.getOrbit();
    }

    @Override
    public SectorEntityToken getOrbitFocus() {
        return entity.getOrbitFocus();
    }

    @Override
    public float getRadius() {
        return entity.getRadius();
    }

    @Override
    public Float getSalvageXP() {
        return entity.getSalvageXP();
    }

    @Override
    public List<EveryFrameScript> getScripts() {
        return entity.getScripts();
    }

    @Override
    public float getSensorContactFaderBrightness() {
        return entity.getSensorContactFaderBrightness();
    }

    @Override
    public float getSensorFaderBrightness() {
        return entity.getSensorFaderBrightness();
    }

    @Override
    public float getSensorProfile() {
        return entity.getSensorProfile();
    }

    @Override
    public StatBonus getSensorRangeMod() {
        return entity.getSensorRangeMod();
    }

    @Override
    public float getSensorStrength() {
        return entity.getSensorStrength();
    }

    @Override
    public StarSystemAPI getStarSystem() {
        return entity.getStarSystem();
    }

    @Override
    public Collection<String> getTags() {
        return entity.getTags();
    }

    @Override
    public Vector2f getVelocity() {
        return entity.getVelocity();
    }

    @Override
    public VisibilityLevel getVisibilityLevelOfPlayerFleet() {
        return entity.getVisibilityLevelOfPlayerFleet();
    }

    @Override
    public VisibilityLevel getVisibilityLevelTo(SectorEntityToken arg0) {
        return entity.getVisibilityLevelTo(arg0);
    }

    @Override
    public VisibilityLevel getVisibilityLevelToPlayerFleet() {
        return entity.getVisibilityLevelToPlayerFleet();
    }

    @Override
    public boolean hasAbility(String arg0) {
        return entity.hasAbility(arg0);
    }

    @Override
    public boolean hasDiscoveryXP() {
        return entity.hasDiscoveryXP();
    }

    @Override
    public boolean hasSalvageXP() {
        return entity.hasSalvageXP();
    }

    @Override
    public boolean hasScriptOfClass(Class arg0) {
        return entity.hasScriptOfClass(arg0);
    }

    @Override
    public boolean hasSensorProfile() {
        return entity.hasSensorProfile();
    }

    @Override
    public boolean hasSensorStrength() {
        return entity.hasSensorStrength();
    }

    @Override
    public boolean hasTag(String arg0) {
        return entity.hasTag(arg0);
    }

    @Override
    public boolean isAlive() {
        return entity.isAlive();
    }

    @Override
    public boolean isDiscoverable() {
        return entity.isDiscoverable();
    }

    @Override
    public boolean isExpired() {
        return entity.isExpired();
    }

    @Override
    public boolean isFreeTransfer() {
        return entity.isFreeTransfer();
    }

    @Override
    public boolean isInCurrentLocation() {
        return entity.isInCurrentLocation();
    }

    @Override
    public boolean isInHyperspace() {
        return entity.isInHyperspace();
    }

    @Override
    public boolean isInOrNearSystem(StarSystemAPI arg0) {
        return entity.isInOrNearSystem(arg0);
    }

    @Override
    public boolean isPlayerFleet() {
        return entity.isPlayerFleet();
    }

    @Override
    public boolean isSkipForJumpPointAutoGen() {
        return entity.isSkipForJumpPointAutoGen();
    }

    @Override
    public boolean isStar() {
        return entity.isStar();
    }

    @Override
    public boolean isSystemCenter() {
        return entity.isSystemCenter();
    }

    @Override
    public boolean isTransponderOn() {
        return entity.isTransponderOn();
    }

    @Override
    public boolean isVisibleToPlayerFleet() {
        return entity.isVisibleToPlayerFleet();
    }

    @Override
    public boolean isVisibleToSensorsOf(SectorEntityToken arg0) {
        return entity.isVisibleToSensorsOf(arg0);
    }

    @Override
    public void removeAbility(String arg0) {
        entity.removeAbility(arg0);
    }

    @Override
    public void removeScript(EveryFrameScript arg0) {
        entity.removeScript(arg0);
    }

    @Override
    public void removeScriptsOfClass(Class arg0) {
        entity.removeScriptsOfClass(arg0);
    }

    @Override
    public void removeTag(String arg0) {
        entity.removeTag(arg0);
    }

    @Override
    public void setActivePerson(PersonAPI arg0) {
        entity.setActivePerson(arg0);
    }

    @Override
    public void setAlwaysUseSensorFaderBrightness(Boolean arg0) {
        entity.setAlwaysUseSensorFaderBrightness(arg0);
    }

    @Override
    public void setAutogenJumpPointNameInHyper(String arg0) {
        entity.setAutogenJumpPointNameInHyper(arg0);
    }

    @Override
    public void setCircularOrbit(SectorEntityToken arg0, float arg1, float arg2, float arg3) {
        entity.setCircularOrbit(arg0, arg1, arg2, arg3);
    }

    @Override
    public void setCircularOrbitAngle(float arg0) {
        entity.setCircularOrbitAngle(arg0);

    }

    @Override
    public void setCircularOrbitPointingDown(SectorEntityToken arg0, float arg1, float arg2, float arg3) {
        entity.setCircularOrbitPointingDown(arg0, arg1, arg2, arg3);
    }

    @Override
    public void setCircularOrbitWithSpin(SectorEntityToken arg0, float arg1, float arg2, float arg3, float arg4,
            float arg5) {
        entity.setCircularOrbitWithSpin(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public void setContainingLocation(LocationAPI arg0) {
        entity.setContainingLocation(arg0);
    }

    @Override
    public void setCustomDescriptionId(String arg0) {
        entity.setCustomDescriptionId(arg0);
    }

    @Override
    public void setCustomInteractionDialogImageVisual(InteractionDialogImageVisual arg0) {
        entity.setCustomInteractionDialogImageVisual(arg0);
    }

    @Override
    public void setDetectionRangeDetailsOverrideMult(Float arg0) {
        entity.setDetectionRangeDetailsOverrideMult(arg0);
    }

    @Override
    public void setDiscoverable(Boolean arg0) {
        entity.setDiscoverable(arg0);
    }

    @Override
    public void setDiscoveryXP(Float arg0) {
        entity.setDiscoveryXP(arg0);
    }

    @Override
    public void setExpired(boolean arg0) {
        entity.setExpired(arg0);
    }

    @Override
    public void setExtendedDetectedAtRange(Float arg0) {
        entity.setExtendedDetectedAtRange(arg0);
    }

    @Override
    public void setFacing(float arg0) {
        entity.setFacing(arg0);
    }

    @Override
    public void setFixedLocation(float arg0, float arg1) {
        entity.setFixedLocation(arg0, arg1);
    }

    @Override
    public void setFreeTransfer(boolean arg0) {
        entity.setFreeTransfer(arg0);
    }

    @Override
    public void setId(String arg0) {
        entity.setId(arg0);
    }

    @Override
    public void setInteractionImage(String arg0, String arg1) {
        entity.setInteractionImage(arg0, arg1);
    }

    @Override
    public void setLightSource(SectorEntityToken arg0, Color arg1) {
        entity.setLightSource(arg0, arg1);
    }

    @Override
    public void setLocation(float arg0, float arg1) {
        entity.setLocation(arg0, arg1);
    }

    @Override
    public void setMarket(MarketAPI arg0) {
        entity.setMarket(arg0);
    }

    @Override
    public void setMemory(MemoryAPI arg0) {
        entity.setMemory(arg0);
    }

    @Override
    public void setName(String arg0) {
        entity.setName(arg0);
    }

    @Override
    public void setOrbit(OrbitAPI arg0) {
        entity.setOrbit(arg0);
    }

    @Override
    public void setOrbitFocus(SectorEntityToken arg0) {
        entity.setOrbitFocus(arg0);
    }

    @Override
    public void setSalvageXP(Float arg0) {
        entity.setSalvageXP(arg0);
    }

    @Override
    public void setSensorProfile(Float arg0) {
        entity.setSensorProfile(arg0);
    }

    @Override
    public void setSensorStrength(Float arg0) {
        entity.setSensorStrength(arg0);
    }

    @Override
    public void setSkipForJumpPointAutoGen(boolean arg0) {
        entity.setSkipForJumpPointAutoGen(arg0);
    }

    @Override
    public void setTransponderOn(boolean arg0) {
        entity.setTransponderOn(arg0);
    }

    @Override
    public EnumSet<CampaignEngineLayers> getActiveLayers() {
        return entity.getActiveLayers();
    }

    @Override
    public CampaignFleetAPI getFleetForVisual() {
        return entity.getFleetForVisual();
    }

    @Override
    public void setActiveLayers(CampaignEngineLayers... arg0) {
        entity.setActiveLayers(arg0);
    }

    @Override
    public void setFleetForVisual(CampaignFleetAPI arg0) {
        entity.setFleetForVisual(arg0);
    }

    @Override
    public void setRadius(float arg0) {
        entity.setRadius(arg0);
    }

    @Override
    public void setFaction(String arg0) {
        entity.setFaction(arg0);
    }
}
