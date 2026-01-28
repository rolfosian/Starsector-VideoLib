package videolib.decoder.grouped;

import org.apache.log4j.Logger;

import videolib.ffmpeg.FFmpeg;
import videolib.ffmpeg.Frame;
import videolib.ffmpeg.VideoFrame;
import videolib.ffmpeg.AudioFrame;

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
                }
            }
            
            sleep(1);
        }
    }
    

    @Override
    public boolean add(Decoder decoder) {
        decoder.start(0);
        read(decoder, decoder.getTextureBuffer(), decoder.getFFmpegCtxPtr());
        return super.add(decoder);
    }

    @Override
    public boolean add(Decoder decoder, long startUs) {
        decoder.start(startUs);
        read(decoder, decoder.getTextureBuffer(), decoder.getFFmpegCtxPtr());
        return super.add(decoder);
    }


    @Override
    protected boolean read(Decoder decoder, TexBuffer texBuffer, long ptr) {
        Frame f = FFmpeg.read(ptr);
        if (f != null) {
            if (f instanceof VideoFrame vf) {
                synchronized(texBuffer) {
                    texBuffer.add(vf);
                }
            } else {
                AudioFrameBuffer audioFrameBuffer = decoder.getAudioBuffer();
                synchronized(audioFrameBuffer) {
                    audioFrameBuffer.add((AudioFrame)f);
                }
            }
            return true;
        }
        return false;
    }
}
