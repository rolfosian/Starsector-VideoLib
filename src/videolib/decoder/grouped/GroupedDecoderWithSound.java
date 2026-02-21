package videolib.decoder.grouped;

import videolib.ffmpeg.FFmpeg;

import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;

import videolib.decoder.DecoderWithSound;
import videolib.buffers.RGBATextureBuffer;

import videolib.projector.Projector;

public class GroupedDecoderWithSound extends DecoderWithSound {
    private boolean doCleanup;

    public GroupedDecoderWithSound(Projector videoProjector, String videoFilePath, int width, int height, int textureId, float volume, PlayMode startingPlayMode, EOFMode startingEOFmode) {
        super(videoProjector, videoFilePath, width, height, volume, startingPlayMode, startingEOFmode);
        this.currentVideoTextureId = textureId;
    }
    
    public void start(long startUs) {
        if (this.running) return;
        // print("Starting GroupedDecoderWithSound for file", videoFilePath);
        this.running = true;

        this.ctxPtr = FFmpeg.openCtx(this.videoFilePath, this.width, this.height, startUs);
        // print("Opened FFmpeg ctx, ptr =", ctxPtr);

        if (ctxPtr == 0) throw new RuntimeException("Failed to initiate FFmpeg ctx context for " + this.videoFilePath);

        this.videoDurationSeconds = FFmpeg.getDurationSeconds(this.ctxPtr);
        this.videoDurationUs = FFmpeg.getDurationUs(this.ctxPtr);

        this.videoFps = FFmpeg.getVideoFps(this.ctxPtr);
        this.spf = 1 / this.videoFps;
        // print("Video Framerate =", videoFps);
        // print("Video Duration=", videoDurationSeconds);
        // print("Video DurationUs=", videoDurationUs);

        // boolean isRGBA = FFmpeg.isRGBA(ctxPtr);
        // print("isRGBA=", isRGBA);
        // this.textureBuffer = isRGBA ? new RGBATextureBuffer(30) : new TextureBuffer(30);
        if (this.currentVideoTextureId != 0) {
            this.textureBuffer = new RGBATextureBuffer(30, currentVideoTextureId, width, height);
            this.doCleanup = false;
            return;
        }
        this.doCleanup = true;
        this.textureBuffer = new RGBATextureBuffer(30);
        this.textureBuffer.initTexStorage(this.width, this.height);
        this.currentVideoTextureId = this.textureBuffer.getTextureId();
        // this.textureBuffer = isRGBA ? new RGBATextureBufferList() : new TextureBufferList();

        this.audioChannels = FFmpeg.getAudioChannels(this.ctxPtr);
        this.audioSampleRate = FFmpeg.getAudioSampleRate(this.ctxPtr);
        // print("Audio Channels=", audioChannels);
        // print("Audio Sample Rate=", audioSampleRate);

        // print("GroupedDecoderWithSound decoderLoop thread started");
        return;
    }

    public void finish() {
        // print("Stopping GroupedDecoderWithSound thread");
        this.running = false;
        this.timeAccumulator = 0f;
        this.videoFps = 0f;

        // print("Joining GroupedDecoderWithSound thread");

        if (this.ctxPtr != 0) {
            // print("Closing FFmpeg ctx");
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
        synchronized(this.audioBuffer) {
            this.audioBuffer.clear();
        }

        this.speakers.finish();
    }
}
