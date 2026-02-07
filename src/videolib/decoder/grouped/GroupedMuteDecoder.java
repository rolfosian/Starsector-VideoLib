package videolib.decoder.grouped;

import videolib.ffmpeg.FFmpeg;

import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;

import videolib.buffers.RGBATextureBuffer;
import videolib.decoder.MuteDecoder;

import videolib.projector.Projector;

public class GroupedMuteDecoder extends MuteDecoder {
    private int texWidth;
    private int texHeight;
    private boolean doCleanup;

    public GroupedMuteDecoder(
        Projector videoProjector,
        String videoFilePath,
        int width,
        int height,
        int textureId,
        int texWidth,
        int texHeight,
        PlayMode startingPlayMode,
        EOFMode startingEOFmode
    ) {
        super(videoProjector, videoFilePath, width, height, startingPlayMode, startingEOFmode);
        this.currentVideoTextureId = textureId;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

    @Override
    public void start(long startUs) {
        if (running) return;
        // print("Starting GroupedMuteDecoder for file", videoFilePath);
        running = true;

        ctxPtr = FFmpeg.openCtxNoSound(videoFilePath, width, height, startUs);
        // print("Opened FFmpeg ctx, ptr =", ctxPtr);

        if (ctxPtr == 0) throw new RuntimeException("Failed to initiate FFmpeg ctx context for " + videoFilePath);

        videoDurationSeconds = FFmpeg.getDurationSeconds(ctxPtr);
        videoDurationUs = FFmpeg.getDurationUs(ctxPtr);

        videoFps = FFmpeg.getVideoFps(ctxPtr);
        spf = 1 / videoFps;
        // print("Video Framerate =", videoFps);
        // print("Video Duration=", videoDurationSeconds);
        // print("Video DurationUs=", videoDurationUs);
        if (this.currentVideoTextureId != 0) {
            this.textureBuffer = new RGBATextureBuffer(3, currentVideoTextureId, texWidth, texHeight);
            this.doCleanup = false;
            return;
        }
        this.doCleanup = true;
        this.textureBuffer = new RGBATextureBuffer(3);
        this.textureBuffer.initTexStorage(texWidth, texHeight);
        this.currentVideoTextureId = this.textureBuffer.getTextureId();
    }

    @Override
    public void finish() {
        if (!running) return;
        // print("Stopping GroupedMuteDecoder decoderLoop thread");
        running = false;
        timeAccumulator = 0f;
        videoFps = 0f;

        if (ctxPtr != 0) {
            print("Closing FFmpeg ctx");
            FFmpeg.closeCtx(ctxPtr);
            ctxPtr = 0;
        }

        // print("Clearing Texture/Video Buffer");
        synchronized(textureBuffer) {
            textureBuffer.clear();
            if (doCleanup) {
                textureBuffer.cleanupTexStorage();
            }
        }
    }
}
