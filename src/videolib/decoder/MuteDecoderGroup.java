package videolib.decoder;

import java.util.*;

import org.apache.log4j.Logger;

import videolib.buffers.TexBuffer;
import videolib.ffmpeg.FFmpeg;
import videolib.ffmpeg.VideoFrame;

/**
 * Assumes decoders are EOFMode.LOOP
 */
public class MuteDecoderGroup extends ArrayList<Decoder> {
    private static final Logger logger = Logger.getLogger(MuteDecoderGroup.class);
    private final Thread decodeThread;
    private volatile boolean running = false;

    public MuteDecoderGroup() {
        super();
        decodeThread = new Thread(this::decodeLoop, "DecoderGroup");
        running = true;
        decodeThread.start();
    }

    private void decodeLoop() {
        while (running) {
            synchronized(this) {
                for (Decoder decoder : new ArrayList<>(this)) {
                    TexBuffer textureBuffer = decoder.getTextureBuffer();
    
                    if (!textureBuffer.isFull()) {
                        long ctxPtr = decoder.getFFmpegCtxPtr();
                        VideoFrame f = FFmpeg.readFrameNoSound(ctxPtr);
    
                        if (f != null) {
                            synchronized(textureBuffer) {
                                textureBuffer.add(f);
                            }
                            continue;
                        }
    
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
            }
            sleep(1);
        }
    }

    public synchronized void finish() {
        for (Decoder decoder : new ArrayList<>(this)) {
            synchronized(decoder) {
                this.remove(decoder);
            }
        }
        running = false;
    }

    @Override
    public synchronized boolean add(Decoder e) {
        e.start(0);

        VideoFrame f = FFmpeg.readFrameNoSound(e.getFFmpegCtxPtr());
        if (f != null) {
            TexBuffer textureBuffer = e.getTextureBuffer();
            synchronized(textureBuffer) {
                textureBuffer.add(f);
            }
        }

        return super.add(e);
    }

    public synchronized boolean add(Decoder e, long startUs) {
        e.start(startUs);

        VideoFrame f = FFmpeg.readFrameNoSound(e.getFFmpegCtxPtr());
        if (f != null) {
            TexBuffer textureBuffer = e.getTextureBuffer();
            synchronized(textureBuffer) {
                textureBuffer.add(f);
            }
        }

        return super.add(e);
    }

    @Override
    public synchronized boolean remove(Object o) {
        ((Decoder) o).finish();
        return super.remove(o);
    }

    @Override
    public synchronized int size() {
        return super.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public synchronized boolean contains(Object o) {
        return super.contains(o);
    }

    @Override
    public synchronized Iterator<Decoder> iterator() {
        return super.iterator();
    }

    @Override
    public synchronized Object[] toArray() {
        return super.toArray();
    }

    @Override
    public synchronized <T> T[] toArray(T[] a) {
        return super.toArray(a);
    }

    @Override
    public synchronized boolean containsAll(Collection<?> c) {
        return super.containsAll(c);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends Decoder> c) {
        return super.addAll(c);
    }

    @Override
    public synchronized boolean addAll(int index, Collection<? extends Decoder> c) {
        return super.addAll(index, c);
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        return super.removeAll(c);
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        return super.retainAll(c);
    }

    @Override
    public synchronized void clear() {
        this.finish();
        super.clear();
    }

    @Override
    public synchronized Decoder get(int index) {
        return super.get(index);
    }

    @Override
    public synchronized Decoder set(int index, Decoder element) {
        return super.set(index, element);
    }

    @Override
    public synchronized void add(int index, Decoder element) {
        super.add(index, element);
    }

    @Override
    public synchronized Decoder remove(int index) {
        Decoder removed = super.get(index);
        this.remove(removed);
        return removed;
    }

    @Override
    public synchronized int indexOf(Object o) {
        return super.indexOf(o);
    }

    @Override
    public synchronized int lastIndexOf(Object o) {
        return super.lastIndexOf(o);
    }

    @Override
    public synchronized ListIterator<Decoder> listIterator() {
        return super.listIterator();
    }

    @Override
    public synchronized ListIterator<Decoder> listIterator(int index) {
        return super.listIterator(index);
    }

    @Override
    public synchronized List<Decoder> subList(int fromIndex, int toIndex) {
        return super.subList(fromIndex, toIndex);
    }

    protected static final void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch(InterruptedException ignored) {}
    }
}
