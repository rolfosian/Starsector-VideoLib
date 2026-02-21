package videolib.decoder.grouped;

import videolib.ffmpeg.FFmpeg;

import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;

import videolib.buffers.RGBATextureBuffer;
import videolib.decoder.MuteDecoder;

import videolib.projector.Projector;

public class GroupedMuteDecoder extends MuteDecoder {
    private boolean doCleanup;

    public GroupedMuteDecoder(
        Projector videoProjector,
        String videoFilePath,
        int width,
        int height,
        int textureId,
        PlayMode startingPlayMode,
        EOFMode startingEOFmode
    ) {
        super(videoProjector, videoFilePath, width, height, startingPlayMode, startingEOFmode);
        this.currentVideoTextureId = textureId;
    }

    @Override
    public void start(long startUs) {
        if (this.running) return;
        // print("Starting GroupedMuteDecoder for file", videoFilePath);
        this.running = true;
        
        this.ctxPtr = FFmpeg.openCtxNoSound(this.videoFilePath, this.width, this.height, startUs);
        // print("Opened FFmpeg ctx, ptr =", ctxPtr);

        if (this.ctxPtr == 0) throw new RuntimeException("Failed to initiate FFmpeg ctx context for " + videoFilePath);

        this.videoDurationSeconds = FFmpeg.getDurationSeconds(this.ctxPtr);
        this.videoDurationUs = FFmpeg.getDurationUs(this.ctxPtr);

        this.videoFps = FFmpeg.getVideoFps(this.ctxPtr);
        this.spf = 1 / this.videoFps;
        // print("Video Framerate =", videoFps);
        // print("Video Duration=", videoDurationSeconds);
        // print("Video DurationUs=", videoDurationUs);
        if (this.currentVideoTextureId != 0) {
            this.textureBuffer = new RGBATextureBuffer(3, this.currentVideoTextureId, this.width, this.height);
            this.doCleanup = false;
            return;
        }
        this.doCleanup = true;
        this.textureBuffer = new RGBATextureBuffer(3);
        this.textureBuffer.initTexStorage(width, height);
        this.currentVideoTextureId = this.textureBuffer.getTextureId();
    }

    @Override
    public void finish() {
        if (!this.running) return;
        // print("Stopping GroupedMuteDecoder decoderLoop thread");
        this.running = false;
        this.timeAccumulator = 0f;
        this.videoFps = 0f;

        if (this.ctxPtr != 0) {
            print("Closing FFmpeg ctx");
            FFmpeg.closeCtx(this.ctxPtr);
            this.ctxPtr = 0;
        }

        // print("Clearing Texture/Video Buffer");
        synchronized(this.textureBuffer) {
            this.textureBuffer.clear();
            if (this.doCleanup) {
                this.textureBuffer.cleanupTexStorage();
                this.currentVideoTextureId = 0;
            }
        }
    }
}
