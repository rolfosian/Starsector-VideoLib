package videolib.entities;

import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;

public class CampaignBillboardParams {
    public final Object[] params;
    public final Map<String, String> factionSpriteMap;

    /**CampaignBillboardParams
     * @param factionIdToSpriteNameMap faction id to sprite name map for if billboard is intended to be a contested objective between factions
     * @param params custom params
    */
    public CampaignBillboardParams(Map<String, String> factionIdToSpriteNameMap, Object... params) {
        this.params = params;
        this.factionSpriteMap = factionIdToSpriteNameMap;

        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (!factionIdToSpriteNameMap.containsKey(faction.getId())); {
                factionSpriteMap.put(faction.getId(), faction.getCrest());
            }
        }
    }
}
