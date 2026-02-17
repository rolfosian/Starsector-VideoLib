package videolib.entities;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import videolib.entities.CampaignBillboard.BillboardTargeter;
import static videolib.VideoLibEveryFrame.campaignDt;

public class RotationalTargeter extends CampaignBillboard.BillboardFacingDelegate {
    private float currentAngle = 0f;
    private float currentVelocity = 0f;

    protected static final float MAX_STEP = 0.05f;
    protected float TENSION = 2f;
    protected float DAMPING = 4f;

    public RotationalTargeter(BillboardTargeter targeter) {
        this.targeter = targeter;
    }

    public RotationalTargeter(BillboardTargeter targeter, float tension, float damping) {
        this.targeter = targeter;
        this.TENSION = tension;
        this.DAMPING = damping;
    }

    @Override
    public float getAngle(CampaignBillboard billboard) {
        float targetAngle = CampaignBillboard.angleBetween(
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

            currentVelocity += angleDifference * TENSION * stepDt;

            float dampingFactor = 1f - DAMPING * stepDt;
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

            float dist = CampaignBillboard.distanceBetween(entityLoc, fleet.getLocation());

            if (dist < closestDistance) {
                closest = fleet;
                closestDistance = dist;
            }
        }
        return closest;
    }
}