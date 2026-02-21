package videolib.entities;

import java.awt.Color;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.*;

import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;

import com.fs.graphics.Sprite;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.InteractionDialogImageVisual;

import org.json.JSONException;
import org.json.JSONObject;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.campaign.BaseLocation;
import com.fs.starfarer.campaign.CustomCampaignEntity;

import videolib.VideoPaths;
import videolib.projector.AutoTexProjector.AutoTexProjectorAPI;
import videolib.util.TexReflection;

import static videolib.VideoLibEveryFrame.phaseDelta;

public final class CampaignBillboard extends CustomCampaignEntityWrapper implements Serializable {
    private static final Logger logger = Logger.getLogger(CampaignBillboard.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    private static final VarHandle customCampaignEntitySpriteVarHandle;
    private static final VarHandle customCampaignEntityPluginVarHandle;

    private static final VarHandle[] spriteVarHandles;
    private static final MethodHandle readResolveHandle;

    public static void initStatic() {}
    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(CustomCampaignEntity.class, lookup);

            readResolveHandle = privateLookup.findVirtual(CustomCampaignEntity.class, "readResolve", MethodType.methodType(Object.class));

            customCampaignEntitySpriteVarHandle = privateLookup.findVarHandle(
                CustomCampaignEntity.class,
                "sprite", 
                Sprite.class
            );
            customCampaignEntityPluginVarHandle = privateLookup.findVarHandle(
                CustomCampaignEntity.class,
                "plugin", 
                CustomCampaignEntityPlugin.class
            );

            privateLookup = MethodHandles.privateLookupIn(Sprite.class, lookup);
            
            List<VarHandle> handles = new ArrayList<>();
            for (Object field : Sprite.class.getDeclaredFields()) {
                String name = TexReflection.getFieldName(field);
                Class<?> type = TexReflection.getFieldType(field);
                VarHandle handle = privateLookup.findVarHandle(
                    Sprite.class,
                    name, 
                    type
                );
                handles.add(handle);
            };
            spriteVarHandles = handles.toArray(new VarHandle[0]);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> parseMap(String tag) {
        try {
            String formatted = tag == null ? "" : tag.trim();
            if (formatted.startsWith("\"") && formatted.endsWith("\"")) {
                formatted = formatted.substring(1, formatted.length() - 1);
            }
            formatted = formatted.replace("\\\"", "\"").replace("\\n", "").trim();

            JSONObject obj = new JSONObject(formatted);
            Map<String, String> map = new HashMap<>();
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, obj.getString(key));
            }
            return map;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static void transplantSpriteFields(Sprite original, Sprite destination) {
        for (VarHandle handle : spriteVarHandles) handle.set(destination, handle.get(original));
    }
    
    public static interface BillboardDialogDelegate {
        public void execute(InteractionDialogAPI dialog, CampaignBillboard billboard);
    }

    /**Pseudo functional interface for the {@link RotationalTargeter} delegate to acquire its target*/
    public static abstract class TargetDelegate implements Serializable {
        public abstract SectorEntityToken target(CampaignBillboard billboard);
        
        public Object readResolve() {
            return this;
        }
    }

    /**Delegate to get the angle at which the billboard will face*/
    public static abstract class BillboardFacingDelegate {
        protected SectorEntityToken currentTarget;
        protected TargetDelegate targeter;

        public abstract float getAngle(CampaignBillboard billboard);

        public final void setTargeter(TargetDelegate targeter) {
            this.targeter = targeter;
        }
    }

    private transient AutoTexProjectorAPI texProjector;
    private transient BillboardSprite ourSprite;
    private transient SpriteAPI vFrameSprite;

    private final CampaignBillboard self = this;
    private final BaseLocation location;

    private float specWidth;
    private float specHeight;

    private BillboardDialogDelegate interactionDialogDelegate;
    private BillboardFacingDelegate angleDelegate;

    private final float lensStructureScale;
    private float alphaMult;
    private long pts;
    private boolean isContested = false;

    private String currSpriteName;
    private Map<String, String> factionSpriteMap;

    protected CampaignBillboard(
        BaseLocation location,
        String id,
        String name,
        String type,
        String factionId
    ) {
        this(location, id, name, type, factionId, -1.0f, -1.0f, null);
    }

    protected CampaignBillboard(
        BaseLocation location,
        String id,
        String name,
        String type,
        String factionId,
        float alphaMult,
        float holoLensStructureScale
    ) {
        this(location, id, name, type, factionId, alphaMult, holoLensStructureScale, null);
    }

    protected CampaignBillboard(
        BaseLocation location,
        String id,
        String name,
        String type,
        String factionId,
        float alphaMult,
        float holoLensStructureScale,
        BillboardFacingDelegate angleDelegate
    ) {
        this(location, id, name, type, factionId, -1.0f, -1.0f, null, alphaMult, holoLensStructureScale, angleDelegate, null);
    }

    protected CampaignBillboard(
        BaseLocation location,
        String id,
        String name,
        String type,
        String factionId,
        float spriteWidth,
        float spriteHeight,
        float alphaMult,
        float holoLensStructureScale,
        BillboardFacingDelegate angleDelegate,
        BillboardDialogDelegate interactionDialogDelegate
    ) {
        this(location, id, name, type, factionId, spriteWidth, spriteHeight, null, alphaMult, holoLensStructureScale, angleDelegate, interactionDialogDelegate);
    }

    protected CampaignBillboard(BaseLocation location,
        String id,
        String name,
        String type,
        String factionId,
        float spriteWidth,
        float spriteHeight,
        CampaignBillboardParams params,
        float alphaMult,
        float holoLensStructureScale,
        BillboardFacingDelegate angleDelegate,
        BillboardDialogDelegate interactionDialogDelegate
    ) { 
        super(new CustomCampaignEntity(id, name, type, factionId, 12f * holoLensStructureScale, spriteWidth, spriteHeight, location.getLightSource(), params));
        this.location = location;

        Sprite sprite = this.entity.getSprite();
        this.specWidth = sprite.getWidth();
        this.specHeight = sprite.getHeight();

        this.angleDelegate = angleDelegate != null ? angleDelegate : new RotationalTargeter.Default();
        this.interactionDialogDelegate = interactionDialogDelegate;

        this.lensStructureScale = holoLensStructureScale < 0 ? 1f : holoLensStructureScale;
        this.alphaMult = alphaMult < 0 ? 0.75f : alphaMult;

        this.currSpriteName = entity.getCustomEntitySpec().getSpriteName();
        this.ctorInit();

        location.addObject(this);
        location.addObject(this.entity);
    }

    private void init() {
        this.ourSprite = new BillboardSprite();
        transplantSpriteFields(entity.getSprite(), ourSprite);
        customCampaignEntitySpriteVarHandle.set(entity, ourSprite);

        this.setVFrameSprite(Global.getSettings().getSprite(this.currSpriteName));
        this.texProjector = VideoPaths.getAutoTexProjectorOverride(this.currSpriteName);

        if (this.getCustomInteractionDialogImageVisual() != null) {
            InteractionDialogImageVisual orig = this.getCustomInteractionDialogImageVisual();
            this.entity.setCustomInteractionDialogImageVisual(new InteractionDialogImageVisual(this.currSpriteName, orig.getSubImageDisplayWidth(), orig.getSubImageDisplayHeight()));
        }
    }

    private void ctorInit() {
        this.ourSprite = new BillboardSprite();
        transplantSpriteFields(this.entity.getSprite(), this.ourSprite);
        customCampaignEntitySpriteVarHandle.set(this.entity, this.ourSprite);

        if (!(this.getPlugin() instanceof CampaignBillboardPlugin)) {
            throw new RuntimeException("CustomCampaignEntityPlugin implementation for CampaignBillboard must be, or inherit from the class videolib.entities.CampaignBillboardPlugin. " + "Found: " 
                + this.getPlugin() == null ? "null" : this.getPlugin().getClass().getCanonicalName());
        }

        CampaignBillboardPlugin plugin = (CampaignBillboardPlugin) this.getPlugin();
        plugin.setBillboard(this);
        this.factionSpriteMap = plugin.getFactionSpriteMap();

        this.setVFrameSprite(Global.getSettings().getSprite(this.currSpriteName));
        this.texProjector = VideoPaths.getAutoTexProjectorOverride(this.currSpriteName);

        if (this.getCustomInteractionDialogImageVisual() != null) {
            InteractionDialogImageVisual orig = this.getCustomInteractionDialogImageVisual();
            this.entity.setCustomInteractionDialogImageVisual(new InteractionDialogImageVisual(currSpriteName, orig.getSubImageDisplayWidth(), orig.getSubImageDisplayHeight()));
        }
    }

    protected Object readResolve() {
        try {
            readResolveHandle.invoke(this.entity);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        Sprite sprite = this.entity.getSprite();
        this.specWidth = sprite.getWidth();
        this.specHeight = sprite.getHeight();

        this.init();

        if (this.texProjector != null && !this.texProjector.runWhilePaused()) {
            if (this.pts != 0 && this.pts != this.texProjector.getDecoder().getCurrentVideoPts()) {
                this.texProjector.getDecoder().seek(this.pts);
                this.texProjector.getDecoder().getCurrentVideoTextureIdDoNotUpdatePts();
            }
        }
        return this;
    }

    protected Object writeReplace() {
        if (this.texProjector != null) {
            this.pts = this.texProjector.getDecoder().getCurrentVideoPts();
        }
        return this;
    }

    public CustomCampaignEntityAPI getBillboardEntity() {
        return this.entity;
    }

    protected CampaignBillboardPlugin getPlugin() {
        return (CampaignBillboardPlugin) entity.getCustomPlugin();
    }

    public BillboardDialogDelegate getInteractionDialogDelegate() {
        return this.interactionDialogDelegate;
    }

    public void setInteractionDialogDelegate(BillboardDialogDelegate interactionDialogDelegate) {
        this.interactionDialogDelegate = interactionDialogDelegate;
    }

    public Sprite getSprite() {
        return entity.getSprite();
    }

    public String getCurrSpriteName() {
        return this.currSpriteName;
    }

    public void setCurrSpriteName(String name) {
        this.currSpriteName = name;
    }

    public void setVFrameSprite(String spriteName) {
        this.currSpriteName = spriteName;
        this.setVFrameSprite(Global.getSettings().getSprite(spriteName));
    }

    public void setVFrameSprite(SpriteAPI sprite) {
        this.ourSprite.setTexX(sprite.getTexX());
        this.ourSprite.setTexY(sprite.getTexY());
        this.ourSprite.setTexWidth(sprite.getTexWidth());
        this.ourSprite.setTexHeight(sprite.getTexHeight());

        this.ourSprite.setWidth(this.specWidth);
        this.ourSprite.setHeight(this.specHeight);

        this.vFrameSprite = sprite; 
    }
    
    public SpriteAPI getVFrameSprite() {
        return this.vFrameSprite;
    }

    public AutoTexProjectorAPI getTexProjector() {
        return this.texProjector;
    }

    public BillboardFacingDelegate getAngleDelegate() {
        return this.angleDelegate;
    }

    public void setAngleDelegate(BillboardFacingDelegate angleDelegate) {
        this.angleDelegate = angleDelegate;
    }

    public void remove() {
        this.location.removeEntity(this.entity);
        this.location.removeObject(this);
    }

    public float getSpritePhase() {
        return this.ourSprite.getPhase();
    }

    public boolean isContested() {
        return this.isContested;
    }

    public void setContested(boolean isContested) {
        this.isContested = isContested;

        if (isContested) {
            this.addTag(Tags.OBJECTIVE);
            this.factionSpriteMap = this.getPlugin().getFactionSpriteMap();
            this.currSpriteName = this.factionSpriteMap.get(this.getFaction().getId());

            if (this.currSpriteName == null) {
                this.currSpriteName = this.getFaction().getCrest();
            }

            this.setVFrameSprite(Global.getSettings().getSprite(this.currSpriteName));
            this.entity.getMemoryWithoutUpdate().set("$objectiveNonFunctional", false);

        } else {
            this.removeTag(Tags.OBJECTIVE);
            this.factionSpriteMap = this.getPlugin().getFactionSpriteMap();
            this.entity.getMemoryWithoutUpdate().set("$objectiveNonFunctional", true);
        } 
    }

    public Map<String, String> getFactionSpriteNameMap() {
        return this.factionSpriteMap;
    }

    /** Use CampaignBillboardPlugin.setFactionSpriteMap if you want to call this method */
    protected void setFactionSpriteMap(Map<String, String> factionSpriteMap) {
        this.factionSpriteMap = factionSpriteMap;
    }

    public void setAlphaMult(float alphaMult) {
        this.alphaMult = alphaMult;
    }

    public void setHoloNoiseAlphaMult(float alphaMult) {
        this.ourSprite.setNoiseAlphaMult(alphaMult);
    }

    @Override
    public void setFaction(String factionId) {
        this.entity.setFaction(factionId);

        Color color = entity.getFaction().getColor();
        this.ourSprite.setLensColor(color);
        this.ourSprite.setNoiseColor(color);

        if (this.isContested) {
            this.currSpriteName = this.factionSpriteMap.get(factionId);

            if (this.currSpriteName == null) {
                this.currSpriteName = Global.getSector().getFaction(factionId).getCrest();
                this.factionSpriteMap.put(factionId, this.currSpriteName);
                this.getPlugin().setFactionSpriteMap(this.factionSpriteMap);
            }

            this.setVFrameSprite(Global.getSettings().getSprite(this.currSpriteName));
            if (this.vFrameSprite == null) {
                throw new RuntimeException("Unable to define sprite on setFaction for CampaignBillboard in " + location.getName() + "using sprite name " + this.currSpriteName);
            }

            if (this.getCustomInteractionDialogImageVisual() != null) {
                InteractionDialogImageVisual orig = this.getCustomInteractionDialogImageVisual();
                this.entity.setCustomInteractionDialogImageVisual(new InteractionDialogImageVisual(currSpriteName, orig.getSubImageDisplayWidth(), orig.getSubImageDisplayHeight()));
            }
        }
    }

    private class BillboardSprite extends Sprite {
        private Sprite lensStructure = new Sprite("graphics/billboards/vl_lens_platform1.png");

        private float phase = 0.0f;

        private int noiseTexId = TexReflection.getTexObjId("graphics/fx/noise.png");
        private int lensTexId = TexReflection.getTexObjId("graphics/starscape/star.png");

        private byte lensRed;
        private byte lensGreen;
        private byte lensBlue;
        private byte noiseRed;
        private byte noiseGreen;
        private byte noiseBlue;
        {
            lensStructure.setSize(lensStructure.getWidth() * lensStructureScale, lensStructure.getHeight() * lensStructureScale);
            Color color = entity.getFaction().getColor();

            this.lensRed = (byte) color.getRed();
            this.lensGreen = (byte) color.getGreen();
            this.lensBlue = (byte) color.getBlue();

            this.noiseRed = (byte) color.getRed();
            this.noiseGreen = (byte) color.getGreen();
            this.noiseBlue = (byte) color.getBlue();
        }

        private float lensFaceLength = 45f * lensStructureScale;
        private float lensWidth = lensStructure.getWidth() / 2.75f;
        private float lensHeight = lensStructure.getHeight() / 2.75f;
        private float sideLensWidth = lensWidth / 1.5f;
        private float sideLensHeight = lensHeight / 1.5f;
        private float sideLensOffset = lensStructure.getWidth() / 2 - 13f;

        private float lensSpin = 0f;
        private float lensAngle = 1f;
        private float lensIntensity = 0.8f;
        private float noiseAlpha = 0.5f;

        private float realLensIntensity = 0.8f;
        private float realNoiseAlpha = 0.4f;
        private float realVframeAlpha = self.alphaMult;

        private void advancePhase(float facing) {
            if (Global.getSector().isPaused()) return;
        
            this.phase += phaseDelta;
            if (this.phase > 1000.0f) {
                this.phase -= 1000.0f;
            }
        
            this.lensSpin += phaseDelta * 5f;
            if (this.lensSpin >= 360f) {
                this.lensSpin -= 360f;
            }
        
            this.lensAngle = facing + this.lensSpin;
            if (this.lensAngle >= 360f) {
                this.lensAngle -= 360f;
            }
        }

        public float getPhase() {
            return this.phase;
        }

        @Override
        public void setAlphaMult(float realAlpha) {
            float brightness = entity.getSensorFader().getBrightness() * entity.getSensorContactFader().getBrightness();
            float real = realAlpha * brightness;
            this.lensStructure.setAlphaMult(real);

            float minFade = 0.915f; 
            float frequency = 2f; 
            float sineWave = (float) Math.sin(this.phase * frequency);
            float normalizedPulse = (sineWave + 1f) / 2f;
            float pulseMult = minFade + ((1f - minFade) * normalizedPulse);

            this.realNoiseAlpha = this.noiseAlpha * real * pulseMult;
            this.realVframeAlpha = self.alphaMult * real * pulseMult * brightness;

            minFade = 0.75f;
            this.realLensIntensity = this.lensIntensity * real * minFade + ((1f - minFade) * normalizedPulse);
        }
        
        @Override
        public void renderAtCenter(float x, float y) {
            float facing = angleDelegate.getAngle(self);
            this.advancePhase(facing);

            renderStructure(x, y, facing);
            renderVframePseudo3DSkewed(x, y, 25f * lensStructureScale, facing);
        }

        private void renderVframePseudo3DSkewed(float x, float y, float dist, float facingAngle) {
            float baseShear = 0.15f;
            float offset = baseShear * this.height;
            float renderHeight = this.height *  0.5f;

            renderHoloNoise(x, y, renderHeight, dist, facingAngle, offset);

            vFrameSprite.bindTexture();
            GL11.glPushMatrix();
            GL11.glColor4ub((byte)color.getRed(), (byte)color.getGreen(), (byte)color.getBlue(),
                            (byte)((int)(color.getAlpha() * realVframeAlpha)));
            
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(getBlendSrc(), getBlendDest());

            GL11.glTranslatef(x + getOffsetX(), y + getOffsetY(), 0.0F);
            GL11.glRotatef(facingAngle, 0.0F, 0.0F, 1.0F);

            GL11.glTranslatef(0.0F, -(dist + renderHeight), 0.0F);
            GL11.glTranslatef(-this.width * 0.5f, 0.0F, 0.0F);
            
            float topWidth = this.width - (offset * 2);
            float bottomWidth = this.width;
            float q = topWidth / bottomWidth;
            
            float u0 = this.texX;
            float u1 = this.texX + this.texWidth;
            float v0 = this.texY;
            float v1 = this.texY + this.texHeight;

            GL11.glBegin(GL11.GL_QUADS);

            // Top-Left
            GL11.glTexCoord4f(u0 * q, v0 * q, 0, q);
            GL11.glVertex2f(offset, 0.0F);

            // Bottom-Left
            GL11.glTexCoord4f(u0, v1, 0, 1.0f);
            GL11.glVertex2f(0.0F, renderHeight);

            // Bottom-Right
            GL11.glTexCoord4f(u1, v1, 0, 1.0f);
            GL11.glVertex2f(this.width, renderHeight);

            // Top-Right
            GL11.glTexCoord4f(u1 * q, v0 * q, 0, q);
            GL11.glVertex2f(this.width - offset, 0.0F);

            GL11.glEnd();

            GL11.glPopMatrix();
        }

        private void renderHoloNoise(
            float x,
            float y,
            float renderHeight,
            float dist,
            float facingAngle,
            float offset
        ) {
            GL11.glPushMatrix();
            
            GL11.glTranslatef(x + getOffsetX(), y + getOffsetY(), 0.0F);
            GL11.glRotatef(facingAngle, 0.0F, 0.0F, 1.0F);

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, noiseTexId);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
            GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        
            GL11.glColor4ub(
                this.noiseRed,
                this.noiseGreen,
                this.noiseBlue,
                (byte) (this.realNoiseAlpha * 255)
            );
        
            float sHalf = this.lensFaceLength * 0.5f;
            float sL = -sHalf;
            float sR =  sHalf;
            float sY = 0;
        
            float centerX = -this.width * 0.5f;
            
            float dY_top = -(dist + renderHeight);
            float dY_bot = -dist;
        
            float dTLx = centerX + offset;
            float dTRx = centerX + this.width - offset;
            float dBLx = centerX;
            float dBRx = centerX + this.width;
            
            float flickerIntensity = 0.1f;
            float uOffset = (float) Math.sin(this.phase * 20.0f) * flickerIntensity;
            float vOffset = (float) Math.cos(this.phase * 30.0f) * flickerIntensity;

            byte innerA = (byte)(this.realNoiseAlpha * 255);  // interior alpha
            byte outerA = 0;                              // feather edge alpha
        
            GL11.glBegin(GL11.GL_QUADS);
            
            // Top quad
            GL11.glTexCoord2f(0.0f + uOffset, 0.0f + vOffset);
            GL11.glColor4ub(this.noiseRed, this.noiseGreen, this.noiseBlue, innerA);
            GL11.glVertex2f(sL, sY);

            GL11.glTexCoord2f(0.0f + uOffset, 1.0f + vOffset);
            GL11.glColor4ub(this.noiseRed, this.noiseGreen, this.noiseBlue, outerA);
            GL11.glVertex2f(dTLx, dY_top);

            GL11.glTexCoord2f(1.0f + uOffset, 1.0f + vOffset);
            GL11.glColor4ub(this.noiseRed, this.noiseGreen, this.noiseBlue, outerA);
            GL11.glVertex2f(dTRx, dY_top);

            GL11.glTexCoord2f(1.0f + uOffset, 0.0f + vOffset);
            GL11.glColor4ub(this.noiseRed, this.noiseGreen, this.noiseBlue, innerA);
            GL11.glVertex2f(sR, sY);

            // Left triangle
            GL11.glTexCoord2f(0.0f + uOffset, 0.0f + vOffset);
            GL11.glColor4ub(this.noiseRed, this.noiseGreen, this.noiseBlue, innerA);
            GL11.glVertex2f(sL, sY);

            GL11.glTexCoord2f(1.0f + uOffset, 1.0f + vOffset);
            GL11.glColor4ub(this.noiseRed, this.noiseGreen, this.noiseBlue, outerA);
            GL11.glVertex2f(dBLx, dY_bot);

            GL11.glTexCoord2f(0.0f + uOffset, 1.0f + vOffset);
            GL11.glColor4ub(this.noiseRed, this.noiseGreen, this.noiseBlue, outerA);
            GL11.glVertex2f(dTLx, dY_top);

            GL11.glTexCoord2f(0.0f + uOffset, 0.0f + vOffset);
            GL11.glColor4ub(this.noiseRed, this.noiseGreen, this.noiseBlue, innerA);
            GL11.glVertex2f(sL, sY);

            // Right triangle
            GL11.glTexCoord2f(0.0f + uOffset, 0.0f + vOffset);
            GL11.glColor4ub(this.noiseRed, this.noiseGreen, this.noiseBlue, innerA);
            GL11.glVertex2f(sR, sY);

            GL11.glTexCoord2f(0.0f + uOffset, 1.0f + vOffset);
            GL11.glColor4ub(this.noiseRed, this.noiseGreen, this.noiseBlue, outerA);
            GL11.glVertex2f(dTRx, dY_top);

            GL11.glTexCoord2f(1.0f + uOffset, 1.0f + vOffset);
            GL11.glColor4ub(this.noiseRed, this.noiseGreen, this.noiseBlue, outerA);
            GL11.glVertex2f(dBRx, dY_bot);

            GL11.glTexCoord2f(0.0f + uOffset, 0.0f + vOffset);
            GL11.glColor4ub(this.noiseRed, this.noiseGreen, this.noiseBlue, innerA);
            GL11.glVertex2f(sR, sY);

            // Bottom quad
            GL11.glTexCoord2f(0.0f + uOffset, 0.0f + vOffset);
            GL11.glColor4ub(this.noiseRed, this.noiseGreen, this.noiseBlue, innerA);
            GL11.glVertex2f(sL, sY);

            GL11.glTexCoord2f(0.0f + uOffset, 1.0f + vOffset);
            GL11.glColor4ub(this.noiseRed, this.noiseGreen, this.noiseBlue, outerA);
            GL11.glVertex2f(dBLx, dY_bot);

            GL11.glTexCoord2f(1.0f + uOffset, 1.0f + vOffset);
            GL11.glColor4ub(this.noiseRed, this.noiseGreen, this.noiseBlue, outerA);
            GL11.glVertex2f(dBRx, dY_bot);

            GL11.glTexCoord2f(1.0f + uOffset, 0.0f + vOffset);
            GL11.glColor4ub(this.noiseRed, this.noiseGreen, this.noiseBlue, innerA);
            GL11.glVertex2f(sR, sY);
        
            GL11.glEnd();
        
            GL11.glPopMatrix();
        }

        private void renderStructure(float x, float y, float angle) {
            lensStructure.setColor(this.getColor());
            lensStructure.setAngle(angle);
            lensStructure.setNormalBlend();
            lensStructure.renderAtCenter(x, y);
        
            float angle2 = lensAngle - 45f;
        
            float rad = (float) Math.toRadians(angle);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
        
            renderLens(x, y, lensWidth, lensHeight, lensAngle);
            renderLens(x, y, lensWidth, lensHeight, angle2);
        
            float leftOffsetX = -sideLensOffset * cos;
            float leftOffsetY = -sideLensOffset * sin;
        
            float rightOffsetX = sideLensOffset * cos;
            float rightOffsetY = sideLensOffset * sin;
        
            renderLens(x + leftOffsetX, y + leftOffsetY, sideLensWidth, sideLensHeight, lensAngle);
            renderLens(x + leftOffsetX, y + leftOffsetY, sideLensWidth, sideLensHeight, angle2);
        
            renderLens(x + rightOffsetX, y + rightOffsetY, sideLensWidth, sideLensHeight, lensAngle);
            renderLens(x + rightOffsetX, y + rightOffsetY, sideLensWidth, sideLensHeight, angle2);
        }

        private void renderLens(float x, float y, float width, float height, float angle) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, lensTexId);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            
            GL11.glColor4ub(lensRed, lensGreen, lensBlue, (byte) (realLensIntensity * 255));

            GL11.glPushMatrix();

            GL11.glTranslatef(x, y, 0.0f);

            GL11.glRotatef(angle, 0.0f, 0.0f, 1.0f);
        
            GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2f(0, 0);
                GL11.glVertex2f(-width / 2, -height / 2);
        
                GL11.glTexCoord2f(1, 0);
                GL11.glVertex2f(width / 2, -height / 2);
        
                GL11.glTexCoord2f(1, 1);
                GL11.glVertex2f(width / 2, height / 2);
        
                GL11.glTexCoord2f(0, 1);
                GL11.glVertex2f(-width / 2, height / 2);
            GL11.glEnd();
        
            GL11.glPopMatrix();
        }

        public void setNoiseColor(Color noiseColor) {
            this.noiseRed = (byte) noiseColor.getRed();
            this.noiseGreen = (byte) noiseColor.getGreen();
            this.noiseBlue = (byte) noiseColor.getBlue();
        }

        public void setNoiseAlphaMult(float alphaMult) {
            this.noiseAlpha = alphaMult;
        }

        public void setLensColor(Color color) {
            this.lensRed = (byte) color.getRed();
            this.lensGreen = (byte) color.getGreen();
            this.lensBlue = (byte) color.getBlue();
        }
    }
}
