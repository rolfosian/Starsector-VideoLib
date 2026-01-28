package videolib.decoder.grouped;

import java.util.*;

import videolib.buffers.TexBuffer;
import videolib.decoder.Decoder;

public abstract class DecoderGroup extends ArrayList<Decoder> {
    protected volatile boolean running = false;
    private final Thread decodeThread;

    public DecoderGroup() {
        super();
        decodeThread = new Thread(this::decodeLoop, "DecoderGroup");
        running = true;
        decodeThread.start();
    }

    protected abstract void decodeLoop();
    protected abstract boolean read(Decoder decoder, TexBuffer texBuffer, long ptr);

    @Override
    public synchronized boolean add(Decoder decoder) {
        return super.add(decoder);
    }

    public abstract boolean add(Decoder decoder, long startUs);

    public final synchronized void finish() {
        for (Decoder decoder : new ArrayList<>(this)) {
                this.remove(decoder);
        }
        running = false;
    }

    protected synchronized final List<Decoder> getTmpDecoderList() {
        return new ArrayList<>(this);
    }

    @Override
    public synchronized final boolean remove(Object o) {
        Decoder decoder = (Decoder) o;
        synchronized(decoder) {
            synchronized(decoder.getTextureBuffer()) {
                if (decoder.getAudioBuffer() != null) {
                    synchronized(decoder.getAudioBuffer()) {
                        decoder.finish();
                    }
                } else {
                    decoder.finish();
                }
            }
        }
        return super.remove(decoder);
    }

    @Override
    public synchronized Decoder remove(int index) {
        Decoder removed = super.get(index);
        this.remove(removed);
        return removed;
    }

    protected static final void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch(InterruptedException ignored) {}
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
}
