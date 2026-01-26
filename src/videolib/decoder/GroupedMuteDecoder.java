package videolib.decoder;

import videolib.ffmpeg.FFmpeg;

import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;
import videolib.buffers.RGBATextureBuffer;
import videolib.projector.Projector;

public class GroupedMuteDecoder extends MuteDecoder {
    public GroupedMuteDecoder(Projector videoProjector, String videoFilePath, int width, int height, PlayMode startingPlayMode, EOFMode startingEOFMode) {
        super(videoProjector, videoFilePath, width, height, startingPlayMode, startingEOFMode);
    }

    @Override
    public void start(long startUs) {
        if (running) return;
        // print("StartingGroupedMuteDecoder for file", videoFilePath);
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

        // boolean isRGBA = FFmpeg.isRGBA(ctxPtr);
        // print("isRGBA=", isRGBA);
        // this.textureBuffer = isRGBA ? new RGBATextureBuffer(10) : new TextureBuffer(10);
        this.textureBuffer = new RGBATextureBuffer(3);
        this.textureBuffer.initTexStorage(width, height);
        this.currentVideoTextureId = this.textureBuffer.getTextureId();

        // print("MuteDecoder decoderLoop thread started");
    }

    @Override
    public void finish() {
        if (!running) return;
        // print("StoppingGroupedMuteDecoder decoderLoop thread");
        running = false;
        timeAccumulator = 0f;
        videoFps = 0f;

        // print("JoiningGroupedMuteDecoder decoderLoop thread");

        if (ctxPtr != 0) {
            print("Closing FFmpeg ctx");
            FFmpeg.closeCtx(ctxPtr);
            ctxPtr = 0;
        }

        // print("Clearing Texture/Video Buffer");
        synchronized(textureBuffer) {
            textureBuffer.clear();
            textureBuffer.cleanupTexStorage();
        }
        // textureBuffer.glDeleteBuffers();
    }
}
