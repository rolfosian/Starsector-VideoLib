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
import org.lwjgl.util.vector.Vector2f;


import com.fs.graphics.Sprite;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.InteractionDialogImageVisual;

import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityPlugin;
import com.fs.starfarer.api.campaign.CustomEntitySpecAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.OrbitAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.characters.PersonAPI;

import com.fs.starfarer.api.combat.StatBonus;

import com.fs.starfarer.api.graphics.SpriteAPI;

import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import com.fs.starfarer.campaign.BaseLocation;
import com.fs.starfarer.campaign.CustomCampaignEntity;

import videolib.VideoPaths;
import videolib.projector.AutoTexProjector.AutoTexProjectorAPI;
import videolib.util.TexReflection;

import static videolib.VideoLibEveryFrame.phaseDelta;

public final class CampaignBillboard implements CustomCampaignEntityAPI, Serializable {
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
    
    public static interface BillboardDialogDelegate {
        public void execute(InteractionDialogAPI dialog, CampaignBillboard billboard);
    }

    public static abstract class BillboardTargeter implements Serializable {
        public abstract SectorEntityToken target(CampaignBillboard billboard);
        
        public Object readResolve() {
            return this;
        }
    }

    public static abstract class BillboardFacingDelegate {
        protected SectorEntityToken currentTarget;
        protected BillboardTargeter targeter;

        public abstract float getAngle(CampaignBillboard billboard);

        public final void setTargeter(BillboardTargeter targeter) {
            this.targeter = targeter;
        }
    }

    private transient AutoTexProjectorAPI texProjector;
    private transient BillboardSprite ourSprite;

    private final CampaignBillboard self = this;
    private final BaseLocation location;
    private final CustomCampaignEntity entity;

    private BillboardDialogDelegate interactionDialogDelegate;
    private BillboardFacingDelegate angleDelegate;

    private float alphaMult;
    private long pts;

    public CampaignBillboard(
        BaseLocation location,
        String id,
        String name,
        String type,
        String factionId,
        float alphaMult
    ) {
        this(location, id, name, type, factionId, alphaMult, null);
    }

    public CampaignBillboard(
        BaseLocation location,
        String id,
        String name,
        String type,
        String factionId,
        float alphaMult,
        BillboardFacingDelegate angleDelegate
    ) {
        this(location, id, name, type, factionId, -1.0f, -1.0f, null, alphaMult, angleDelegate, null);
    }

    public CampaignBillboard(
        BaseLocation location,
        String id,
        String name,
        String type,
        String factionId,
        float spriteWidth,
        float spriteHeight,
        float alphaMult,
        BillboardFacingDelegate angleDelegate,
        BillboardDialogDelegate interactionDialogDelegate
    ) {
        this(location, id, name, type, factionId, spriteWidth, spriteHeight, null, alphaMult, angleDelegate, interactionDialogDelegate);
    }

    public CampaignBillboard(BaseLocation location,
        String id,
        String name,
        String type,
        String factionId,
        float spriteWidth,
        float spriteHeight,
        Object params,
        float alphaMult,
        BillboardFacingDelegate angleDelegate,
        BillboardDialogDelegate interactionDialogDelegate
    ) { 
        this.entity = new CustomCampaignEntity(id, name, type, factionId, 25f, spriteWidth, spriteHeight, location.getLightSource(), params);
        this.angleDelegate = angleDelegate != null ? angleDelegate : new BillboardFacingDelegate() {
            @Override
            public float getAngle(CampaignBillboard billboard) {
                return billboard.getBillboardEntity().getFacing() - 90f;
            }
        };
        this.interactionDialogDelegate = interactionDialogDelegate;
        this.alphaMult = alphaMult;
        this.location = location;
        this.init();
        location.addObject(this);
        location.addObject(this.entity);
    }

    public CustomCampaignEntityAPI getBillboardEntity() {
        return this.entity;
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
        this.location.removeEntity(entity);
        this.location.removeObject(this);
    }

    public float getSpritePhase() {
        return this.ourSprite.getPhase();
    }

    private class BillboardSprite extends Sprite {
        private final Sprite structureMid = new Sprite("graphics/billboards/vl_lens_platform1.png");
        private final SpriteAPI vFrameSprite = Global.getSettings().getSprite(entity.getCustomEntitySpec().getSpriteName());

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
            Color color = entity.getFaction().getColor();

            this.lensRed = (byte) color.getRed();
            this.lensGreen = (byte) color.getGreen();
            this.lensBlue = (byte) color.getBlue();

            this.noiseRed = (byte) color.getRed();
            this.noiseGreen = (byte) color.getGreen();
            this.noiseBlue = (byte) color.getBlue();
        }

        private float lensWidth = structureMid.getWidth() / 2.75f;
        private float lensHeight = structureMid.getHeight() / 2.75f;
        private float sideLensWidth = lensWidth / 1.5f;
        private float sideLensHeight = lensHeight / 1.5f;
        private float sideLensOffset = structureMid.getWidth() / 2 - 13f;

        private float lensSpin = 0f;
        private float lensAngle = 1f;
        private float lensIntensity = 0.8f;
        private float noiseAlpha = 0.5f;

        private float realLensIntensity = 0.8f;
        private float realNoiseAlpha = 0.4f;
        private float realVframeAlpha = self.alphaMult;

        public void advancePhase(float facing) {
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
            this.structureMid.setAlphaMult(real);

            float minFade = 0.925f; 
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
            renderVframePseudo3DSkewed(x, y, 25f, facing, 1.0f);
        }

        private void renderVframePseudo3DSkewed(float x, float y, float dist, float facingAngle, float perspectiveFactor) {
            float baseShear = 0.3f;
            float shear = baseShear * perspectiveFactor;
            float dynamicHeight = height * perspectiveFactor;
            float offset = shear * dynamicHeight;

            renderHoloNoise(x, y, dist, facingAngle, 45f, offset);

            vFrameSprite.bindTexture();
            GL11.glPushMatrix();
            GL11.glColor4ub((byte)color.getRed(), (byte)color.getGreen(), (byte)color.getBlue(),
                            (byte)((int)(color.getAlpha() * realVframeAlpha)));
            
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(getBlendSrc(), getBlendDest());

            GL11.glTranslatef(x + getOffsetX(), y + getOffsetY(), 0.0F);
            GL11.glRotatef(facingAngle, 0.0F, 0.0F, 1.0F);

            GL11.glTranslatef(0.0F, -(dist + height), 0.0F);
            GL11.glTranslatef(-width * 0.5f, 0.0F, 0.0F);
            
            float topWidth = width - (offset * 2);
            float bottomWidth = width;
            float q = topWidth / bottomWidth;
            
            float u0 = texX;
            float u1 = texX + texWidth;
            float v0 = texY;
            float v1 = texY + texHeight;

            GL11.glBegin(GL11.GL_QUADS);

            // Top-Left
            GL11.glTexCoord4f(u0 * q, v0 * q, 0, q);
            GL11.glVertex2f(offset, 0.0F);
        
            // Bottom-Left
            GL11.glTexCoord4f(u0, v1, 0, 1.0f);
            GL11.glVertex2f(0.0F, height);
        
            // Bottom-Right
            GL11.glTexCoord4f(u1, v1, 0, 1.0f);
            GL11.glVertex2f(width, height);
        
            // Top-Right
            GL11.glTexCoord4f(u1 * q, v0 * q, 0, q);
            GL11.glVertex2f(width - offset, 0.0F);
        
            GL11.glEnd();
        
            GL11.glPopMatrix();
        }

        private void renderHoloNoise(
            float x, 
            float y, 
            float dist,
            float facingAngle,
            float lensFaceLength, 
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
                noiseRed,
                noiseGreen,
                noiseBlue,
                (byte) (realNoiseAlpha * 255)
            );
        
            float sHalf = lensFaceLength * 0.5f;
            float sL = -sHalf;
            float sR =  sHalf;
            float sY = 0;
        
            float centerX = -width * 0.5f;
            
            float dY_top = -(dist + height);
            float dY_bot = -dist;
        
            float dTLx = centerX + offset;
            float dTRx = centerX + width - offset;
            float dBLx = centerX;
            float dBRx = centerX + width;
            
            float flickerIntensity = 0.1f;
            float uOffset = (float) Math.sin(this.phase * 20.0f) * flickerIntensity;
            float vOffset = (float) Math.cos(this.phase * 30.0f) * flickerIntensity;

            byte innerA = (byte)(realNoiseAlpha * 255);  // interior alpha
            byte outerA = 0;                              // feather edge alpha
        
            GL11.glBegin(GL11.GL_QUADS);
            
            // Top quad
            GL11.glTexCoord2f(0.0f + uOffset, 0.0f + vOffset);
            GL11.glColor4ub(noiseRed, noiseGreen, noiseBlue, innerA);
            GL11.glVertex2f(sL, sY);

            GL11.glTexCoord2f(0.0f + uOffset, 1.0f + vOffset);
            GL11.glColor4ub(noiseRed, noiseGreen, noiseBlue, outerA);
            GL11.glVertex2f(dTLx, dY_top);

            GL11.glTexCoord2f(1.0f + uOffset, 1.0f + vOffset);
            GL11.glColor4ub(noiseRed, noiseGreen, noiseBlue, outerA);
            GL11.glVertex2f(dTRx, dY_top);

            GL11.glTexCoord2f(1.0f + uOffset, 0.0f + vOffset);
            GL11.glColor4ub(noiseRed, noiseGreen, noiseBlue, innerA);
            GL11.glVertex2f(sR, sY);

            // Left triangle
            GL11.glTexCoord2f(0.0f + uOffset, 0.0f + vOffset);
            GL11.glColor4ub(noiseRed, noiseGreen, noiseBlue, innerA);
            GL11.glVertex2f(sL, sY);

            GL11.glTexCoord2f(1.0f + uOffset, 1.0f + vOffset);
            GL11.glColor4ub(noiseRed, noiseGreen, noiseBlue, outerA);
            GL11.glVertex2f(dBLx, dY_bot);

            GL11.glTexCoord2f(0.0f + uOffset, 1.0f + vOffset);
            GL11.glColor4ub(noiseRed, noiseGreen, noiseBlue, outerA);
            GL11.glVertex2f(dTLx, dY_top);

            GL11.glTexCoord2f(0.0f + uOffset, 0.0f + vOffset);
            GL11.glColor4ub(noiseRed, noiseGreen, noiseBlue, innerA);
            GL11.glVertex2f(sL, sY);

            // Right triangle
            GL11.glTexCoord2f(0.0f + uOffset, 0.0f + vOffset);
            GL11.glColor4ub(noiseRed, noiseGreen, noiseBlue, innerA);
            GL11.glVertex2f(sR, sY);

            GL11.glTexCoord2f(0.0f + uOffset, 1.0f + vOffset);
            GL11.glColor4ub(noiseRed, noiseGreen, noiseBlue, outerA);
            GL11.glVertex2f(dTRx, dY_top);

            GL11.glTexCoord2f(1.0f + uOffset, 1.0f + vOffset);
            GL11.glColor4ub(noiseRed, noiseGreen, noiseBlue, outerA);
            GL11.glVertex2f(dBRx, dY_bot);

            GL11.glTexCoord2f(0.0f + uOffset, 0.0f + vOffset);
            GL11.glColor4ub(noiseRed, noiseGreen, noiseBlue, innerA);
            GL11.glVertex2f(sR, sY);

            // Bottom quad
            GL11.glTexCoord2f(0.0f + uOffset, 0.0f + vOffset);
            GL11.glColor4ub(noiseRed, noiseGreen, noiseBlue, innerA);
            GL11.glVertex2f(sL, sY);

            GL11.glTexCoord2f(0.0f + uOffset, 1.0f + vOffset);
            GL11.glColor4ub(noiseRed, noiseGreen, noiseBlue, outerA);
            GL11.glVertex2f(dBLx, dY_bot);

            GL11.glTexCoord2f(1.0f + uOffset, 1.0f + vOffset);
            GL11.glColor4ub(noiseRed, noiseGreen, noiseBlue, outerA);
            GL11.glVertex2f(dBRx, dY_bot);

            GL11.glTexCoord2f(1.0f + uOffset, 0.0f + vOffset);
            GL11.glColor4ub(noiseRed, noiseGreen, noiseBlue, innerA);
            GL11.glVertex2f(sR, sY);
        
            GL11.glEnd();
        
            GL11.glPopMatrix();
        }

        private void renderStructure(float x, float y, float angle) {
            structureMid.setColor(this.getColor());
            structureMid.setAngle(angle);
            structureMid.setNormalBlend();
            structureMid.renderAtCenter(x, y);
        
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

    public void setAlphaMult(float alphaMult) {
        this.alphaMult = alphaMult;
    }

    private void init() {
        this.ourSprite = new BillboardSprite();
        transplantSpriteFields(entity.getSprite(), ourSprite);
        customCampaignEntitySpriteVarHandle.set(entity, ourSprite);

        CampaignBillboardPlugin plugin = new CampaignBillboardPlugin(this);
        plugin.init(entity, null);
        customCampaignEntityPluginVarHandle.set(this.entity, plugin);

        this.texProjector = VideoPaths.getAutoTexProjectorOverride(entity.getCustomEntitySpec().getSpriteName());
    }

    protected Object readResolve() {
        try {
            readResolveHandle.invoke(this.entity);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

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

    private static void transplantSpriteFields(Sprite original, Sprite destination) {
        for (VarHandle handle : spriteVarHandles) handle.set(destination, handle.get(original));
    }

    public static void test() {
        LocationAPI location = Global.getSector().getPlayerFleet().getContainingLocation();
        Vector2f playerLoc = Global.getSector().getPlayerFleet().getLocation();

        CampaignBillboard testBoard = new CampaignBillboard(
            (BaseLocation)location,
            null,
            null,
            "vl_billboard_example",
            "pirates",
            200f,
            92f,
            0.6f,
            new RotationalTargeter(new BillboardTargeter() {
                @Override
                public SectorEntityToken target(CampaignBillboard billboard) {
                    return RotationalTargeter.getNearestFleet(billboard);
                }
            }),
            null
        );
        // testBoard.setAlphaMult(0);

        if (location instanceof StarSystemAPI system) {
            PlanetAPI[] closestPlanet = new PlanetAPI[] {null};
            float closestDistance = Float.MAX_VALUE;
            for (PlanetAPI planet : system.getPlanets()) {
                float dist = distanceBetween(planet.getLocation(), playerLoc);
                if (dist < closestDistance) {
                    closestPlanet[0] = planet;
                    closestDistance = dist;
                }
            }
            PlanetAPI closest = closestPlanet[0];
            Vector2f planetLoc = closest.getLocation();

            // testBoard.getAngleDelegate().setTargeter(
            //     new BillboardTargeter() {
            //         @Override
            //         public SectorEntityToken target(CampaignBillboard billboard) {
            //             return closest;
            //         }
            //     }
            // );

            float angle = angleBetween(playerLoc, planetLoc);
            testBoard.setCircularOrbit(closest, angle, distanceBetween(playerLoc, closest.getLocation()), 20f);
            return;
        }
        testBoard.setLocation(playerLoc.x, playerLoc.y);
    }

    public static float angleBetween(Vector2f loc1, Vector2f loc2) {
        float dx = loc1.x - loc2.x;
        float dy = loc1.y - loc2.y;
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }

    public static float distanceBetween(Vector2f pos1, Vector2f pos2) {
        return (float)Math.sqrt((double)((pos1.x - pos2.x) * (pos1.x - pos2.x) + (pos1.y - pos2.y) * (pos1.y - pos2.y)));
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
    public void setFaction(String arg0) {
        entity.setFaction(arg0);
        Color color = entity.getFaction().getColor();
        ourSprite.setLensColor(color);
        ourSprite.setNoiseColor(color);
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
}
