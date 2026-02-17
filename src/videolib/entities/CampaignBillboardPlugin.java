package videolib.entities;

import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;

public final class CampaignBillboardPlugin extends BaseCustomEntityPlugin {
    private final CampaignBillboard billboard;
    
    public CampaignBillboardPlugin(CampaignBillboard billboard) {
        super();
        this.billboard = billboard;
    }

    public CampaignBillboard getBillboard() {
        return this.billboard;
    }
}