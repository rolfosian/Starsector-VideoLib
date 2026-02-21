package videolib.entities;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import videolib.entities.CampaignBillboard.TargetDelegate;
import videolib.entities.CampaignBillboard.BillboardFacingDelegate;

import static videolib.VideoLibEveryFrame.campaignDt;

public class RotationalTargeter extends BillboardFacingDelegate {
    public static final class Default extends BillboardFacingDelegate {
        @Override
        public float getAngle(CampaignBillboard billboard) {
            return billboard.getBillboardEntity().getFacing() - 90f;
        }
    }

    /** Assumes the orbit and orbit focus of the billboard's entity are non-null */
    public static final class PointAtOrbitFocus extends BillboardFacingDelegate {
        @Override
        public float getAngle(CampaignBillboard billboard) {
            return angleBetween(billboard.getLocation(), billboard.getBillboardEntity().getOrbit().getFocus().getLocation()) - 90f;
        }
    }

    /** Assumes the orbit and orbit focus of the billboard's entity are non-null */
    public static final class PointAwayFromOrbitFocus extends BillboardFacingDelegate {
        @Override
        public float getAngle(CampaignBillboard billboard) {
            return angleBetween(billboard.getLocation(), billboard.getBillboardEntity().getOrbit().getFocus().getLocation()) - 270f;
        }
    }

    protected static final float MAX_STEP = 0.05f;

    private float currentAngle = 0f;
    private float currentVelocity = 0f;

    protected float tension = 2f;
    protected float damping = 4f;

    public RotationalTargeter(TargetDelegate targeter) {
        this.targeter = targeter;
    }

    public RotationalTargeter(TargetDelegate targeter, float tension, float damping) {
        this.targeter = targeter;
        this.tension = tension;
        this.damping = damping;
    }

    @Override
    public float getAngle(CampaignBillboard billboard) {
        float targetAngle = angleBetween(
            billboard.getLocation(),
            targeter.target(billboard).getLocation()
        ) - 90f;

        if (Global.getSector().isPaused()) {
            return currentAngle;
        }

        float dt = campaignDt;

        int steps = (int) Math.ceil(dt / MAX_STEP);
        float stepDt = dt / steps;

        for (int i = 0; i < steps; i++) {

            float angleDifference = normalizeAngle(targetAngle - currentAngle);

            currentVelocity += angleDifference * tension * stepDt;

            float dampingFactor = 1f - damping * stepDt;
            if (dampingFactor < 0f) dampingFactor = 0f;
            currentVelocity *= dampingFactor;

            currentAngle += currentVelocity * stepDt;
        }

        currentAngle = normalizeAngle(currentAngle);

        return currentAngle;
    }

    
    private static float normalizeAngle(float angle) {
        angle %= 360f;
        if (angle < -180f) angle += 360f;
        if (angle > 180f) angle -= 360f;
        return angle;
    }

    public static CampaignFleetAPI getNearestFleet(SectorEntityToken entity) {
        Vector2f entityLoc = entity.getLocation();

        float closestDistance = Float.MAX_VALUE;
        CampaignFleetAPI closest = null;

        for (CampaignFleetAPI fleet : entity.getContainingLocation().getFleets()) {

            float dist = distanceBetween(entityLoc, fleet.getLocation());

            if (dist < closestDistance) {
                closest = fleet;
                closestDistance = dist;
            }
        }
        return closest;
    }

    public static float angleBetween(Vector2f loc1, Vector2f loc2) {
        float dx = loc1.x - loc2.x;
        float dy = loc1.y - loc2.y;
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }

    public static float distanceBetween(Vector2f pos1, Vector2f pos2) {
        return (float)Math.sqrt((double)((pos1.x - pos2.x) * (pos1.x - pos2.x) + (pos1.y - pos2.y) * (pos1.y - pos2.y)));
    }
}