package videolib.planetlistener;

import videolib.projector.PlanetProjector;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

public class PlanetProjectorListener extends BaseCampaignEventListener {
    private PlanetProjector projector;

    public PlanetProjectorListener(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {
        if (projector != null) {
            projector.setRunWhilePaused(false);
            projector = null;
        }
    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        SectorEntityToken target = dialog.getInteractionTarget();
        if (target == null) return;

        MemoryAPI mem = target.getMemoryWithoutUpdate();
        if (mem == null) return;

        projector = (PlanetProjector) mem.get(PlanetProjector.PLANET_PROJECTOR_MEM_KEY);
        if (projector == null) return;
        
        projector.setRunWhilePaused(true);
    }
    
}
