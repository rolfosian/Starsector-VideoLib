package videolib.entities;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;

import java.util.*;

public class CampaignBillboardPlugin extends BaseCustomEntityPlugin {
    private Map<String, String> factionSpriteMap;

    protected CampaignBillboard billboard;
    protected Object[] params;
    
    public CampaignBillboardPlugin() {
        super();
    }

    @Override
    public final void init(SectorEntityToken entity, Object params) {
        this.entity = entity;
        if (params != null) {
            CampaignBillboardParams pluginParams = (CampaignBillboardParams) params;
            this.params = pluginParams.params;
            this.factionSpriteMap = (Map<String, String>) pluginParams.factionSpriteMap;
        }
    }

    public CampaignBillboard getBillboard() {
        return this.billboard;
    }

    public void setBillboard(CampaignBillboard billboard) {
        this.billboard = billboard;
    }

    public Object[] getParams() {
        return this.params;
    }

    public void setFactionSpriteMap(Map<String, String> factionSpriteMap) {
        this.factionSpriteMap = factionSpriteMap;
        this.billboard.setFactionSpriteMap(factionSpriteMap);
    }

    public Map<String, String> getFactionSpriteMap() {
        return this.factionSpriteMap;
    }
}