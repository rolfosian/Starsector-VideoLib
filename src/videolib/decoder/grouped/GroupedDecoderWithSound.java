package videolib.decoder.grouped;

import videolib.ffmpeg.FFmpeg;

import videolib.VideoModes.EOFMode;
import videolib.VideoModes.PlayMode;

import videolib.decoder.DecoderWithSound;
import videolib.buffers.RGBATextureBuffer;

import videolib.projector.Projector;

public class GroupedDecoderWithSound extends DecoderWithSound {
    public GroupedDecoderWithSound(Projector videoProjector, String videoFilePath, int width, int height, float volume, PlayMode startingPlayMode, EOFMode startingEOFMode) {
        super(videoProjector, videoFilePath, width, height, volume, startingPlayMode, startingEOFMode);
    }
    
    public void start(long startUs) {
        if (running) return;
        // print("Starting GroupedDecoderWithSound for file", videoFilePath);
        running = true;

        ctxPtr = FFmpeg.openCtx(videoFilePath, width, height, startUs);
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
        // this.textureBuffer = isRGBA ? new RGBATextureBuffer(30) : new TextureBuffer(30);
        this.textureBuffer = new RGBATextureBuffer(30);
        this.textureBuffer.initTexStorage(width, height);
        this.currentVideoTextureId = this.textureBuffer.getTextureId();
        // this.textureBuffer = isRGBA ? new RGBATextureBufferList() : new TextureBufferList();

        audioChannels = FFmpeg.getAudioChannels(ctxPtr);
        audioSampleRate = FFmpeg.getAudioSampleRate(ctxPtr);
        // print("Audio Channels=", audioChannels);
        // print("Audio Sample Rate=", audioSampleRate);

        // print("GroupedDecoderWithSound decoderLoop thread started");
        return;
    }

    public void finish() {
        // print("Stopping GroupedDecoderWithSound thread");
        running = false;
        timeAccumulator = 0f;
        videoFps = 0f;

        // print("Joining GroupedDecoderWithSound thread");

        if (ctxPtr != 0) {
            // print("Closing FFmpeg ctx");
            FFmpeg.closeCtx(ctxPtr);
            ctxPtr = 0;
        }

        // print("Clearing Texture/Video Buffer");
        synchronized(textureBuffer) {
            textureBuffer.clear();
            textureBuffer.cleanupTexStorage();
        }
        synchronized(audioBuffer) {
            audioBuffer.clear();
        }

        speakers.finish();
    }
}
