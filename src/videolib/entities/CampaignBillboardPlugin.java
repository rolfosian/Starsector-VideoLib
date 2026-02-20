package videolib.entities;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;

import java.util.*;

@SuppressWarnings("unchecked")
public class CampaignBillboardPlugin extends BaseCustomEntityPlugin {
    private Map<String, String> factionSpriteMap;

    protected CampaignBillboard billboard;
    protected Map<String, Object> params;
    
    public CampaignBillboardPlugin() {
        super();
    }

    @Override
   public void init(SectorEntityToken entity, Object pluginParams) {
      this.entity = entity;
      this.params = (Map<String, Object>) pluginParams;
      if (pluginParams != null) {
            this.factionSpriteMap = (Map<String, String>) this.params.get("factionSpriteMap");
      }
   }

    public CampaignBillboard getBillboard() {
        return this.billboard;
    }

    public void setBillboard(CampaignBillboard billboard) {
        this.billboard = billboard;
    }

    public Map<String, Object> getParams() {
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