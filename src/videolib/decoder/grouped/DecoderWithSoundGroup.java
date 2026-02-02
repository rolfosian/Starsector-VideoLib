package videolib.decoder.grouped;

import org.apache.log4j.Logger;

import videolib.ffmpeg.FFmpeg;

import videolib.buffers.AudioFrameBuffer;
import videolib.buffers.TexBuffer;
import videolib.decoder.Decoder;

/**
 * Assumes decoders are EOFMode.LOOP
 */
public class DecoderWithSoundGroup extends DecoderGroup {
    private static final Logger logger = Logger.getLogger(GroupedDecoderWithSound.class);

    @Override
    protected void decodeLoop() {
        while (running) {
            for (Decoder decoder : getTmpDecoderList()) {
                TexBuffer textureBuffer = decoder.getTextureBuffer();

                if (!textureBuffer.isFull()) {
                    AudioFrameBuffer audioBuffer = decoder.getAudioBuffer();
                    long ctxPtr = decoder.getFFmpegCtxPtr();

                    if (read(textureBuffer, audioBuffer, ctxPtr)) continue;

                    if (FFmpeg.getErrorStatus(ctxPtr) != FFmpeg.AVERROR_EOF) {
                        logger.error(
                            "FFmpeg error for file " + decoder.getVideoFilePath() + ": " + FFmpeg.getErrorMessage(ctxPtr) + ", removing decoder from group...",
                            new RuntimeException(FFmpeg.getErrorMessage(ctxPtr))
                        );

                        this.remove(decoder);
                        continue;
                    }
                    
                    decoder.seekWithoutClearingBuffer(0);
                    read(textureBuffer, audioBuffer, ctxPtr);
                }
            }
            
            sleep(1);
        }
    }
    

    @Override
    public boolean add(Decoder decoder) {
        decoder.start(0);
        read(decoder.getTextureBuffer(), decoder.getAudioBuffer(), decoder.getFFmpegCtxPtr());
        return super.add(decoder);
    }

    public boolean add(Decoder decoder, long startUs) {
        decoder.start(startUs);
        read(decoder.getTextureBuffer(), decoder.getAudioBuffer(), decoder.getFFmpegCtxPtr());
        return super.add(decoder);
    }
}
