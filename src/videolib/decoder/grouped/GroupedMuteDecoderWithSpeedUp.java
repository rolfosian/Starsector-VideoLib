package videolib.decoder.grouped;

import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;
import videolib.projector.Projector;
import static videolib.VideoLibEveryFrame.campaignDt;

public class GroupedMuteDecoderWithSpeedUp extends GroupedMuteDecoder implements DeltaTimeDelegator {
    private static interface SpeedDelegate {
        int run(float dt);
    }
    private final SpeedDelegate combatDelegate = new SpeedDelegate() {
        @Override
        public int run(float dt) {
            return superGet(dt);
        }
    };
    private final SpeedDelegate campaignDelegate = new SpeedDelegate() {
        @Override
        public int run(float dt) {
            return superGet(campaignDt);
        }
    };

    private SpeedDelegate delegate;
    private int superGet(float dt) {
        return super.getCurrentVideoTextureId(dt);
    }

    public void setCombat() {
        for (int i = 0; i < 10; i++) print("SETTING COMBAT");
        this.delegate = combatDelegate;
    }

    public void setCampaign() {
        for (int i = 0; i < 10; i++) print("SETTING CAMPAIGN");
        this.delegate = campaignDelegate;
    }

    public GroupedMuteDecoderWithSpeedUp(
        Projector videoProjector,
        String videoFilePath,
        int width,
        int height,
        int textureId,
        PlayMode startingPlayMode,
        EOFMode startingEOFmode
    ) {
        super(videoProjector, videoFilePath, width, height, textureId, startingPlayMode, startingEOFmode);
    }

    @Override
    public int getCurrentVideoTextureId(float dt) {
        return delegate.run(dt);
    }
}
