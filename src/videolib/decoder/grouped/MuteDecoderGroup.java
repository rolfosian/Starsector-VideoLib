package videolib.decoder.grouped;

import org.apache.log4j.Logger;

import videolib.buffers.TexBuffer;
import videolib.decoder.Decoder;
import videolib.ffmpeg.FFmpeg;
import videolib.ffmpeg.VideoFrame;

/**
 * Assumes decoders are EOFMode.LOOP
 */
public class MuteDecoderGroup extends DecoderGroup {
    private static final Logger logger = Logger.getLogger(MuteDecoderGroup.class);

    @Override
    protected void decodeLoop() {
        while (running) {
            for (Decoder decoder : getTmpDecoderList()) {
                TexBuffer textureBuffer = decoder.getTextureBuffer();

                if (!textureBuffer.isFull()) {
                    long ctxPtr = decoder.getFFmpegCtxPtr();

                    if (read(decoder, textureBuffer, ctxPtr)) continue;

                    if (FFmpeg.getErrorStatus(ctxPtr) != FFmpeg.AVERROR_EOF) {
                        logger.error(
                            "FFmpeg error for file " + decoder.getVideoFilePath() + ": " + FFmpeg.getErrorMessage(ctxPtr) + ", removing decoder from group...",
                            new RuntimeException(FFmpeg.getErrorMessage(ctxPtr))
                        );

                        this.remove(decoder);
                        continue;
                    }
                    
                    decoder.seekWithoutClearingBuffer(0);
                    read(decoder, textureBuffer, ctxPtr);
                }
            }
            
            sleep(1);
        }
    }

    @Override
    public synchronized boolean add(Decoder decoder) {
        decoder.start(0);
        read(decoder, decoder.getTextureBuffer(), decoder.getFFmpegCtxPtr());
        return super.add(decoder);
    }

    @Override
    public synchronized boolean add(Decoder decoder, long startUs) {
        decoder.start(startUs);
        read(decoder, decoder.getTextureBuffer(), decoder.getFFmpegCtxPtr());
        return super.add(decoder);
    }

    @Override
    protected boolean read(Decoder decoder, TexBuffer textureBuffer, long ptr) {
        VideoFrame f = FFmpeg.readFrameNoSound(ptr);
        if (f != null) {
            synchronized(textureBuffer) {
                textureBuffer.add(f);
            }
            return true;
        }
        return false;
    }
}
