package data.scripts.speakers;

import data.scripts.buffers.AudioFrameBuffer;
import data.scripts.decoder.Decoder;
import data.scripts.ffmpeg.AudioFrame;
import data.scripts.ffmpeg.FFmpeg;
import data.scripts.projector.Projector;
import data.scripts.util.VideoUtils;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCcontext;
import org.lwjgl.openal.ALCdevice;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.input.InputEventAPI;

import org.apache.log4j.Logger;

import java.nio.IntBuffer;
import java.util.*;

public class VideoProjectorSpeakers extends BaseEveryFrameCombatPlugin implements Speakers, EveryFrameScript {
    private static final Logger logger = Logger.getLogger(VideoProjectorSpeakers.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    private static final int NUM_BUFFERS = 4;

    private boolean isDone;

    private final Projector videoProjector;
    private final Decoder decoder;
    private final AudioFrameBuffer audioFrameBuffer;

    private float volume;
    private float volumeActual;
    private boolean paused = true;
    private boolean finished = false;

    private long currentAudioPts;

    private final int sampleRate;
    private final int channels;
    private final int format;

    private ALCdevice device;
    private ALCcontext context;
    private int sourceId;
    private IntBuffer bufferIds;

    private class BufferToFrame extends HashMap<Integer, AudioFrame> {
        public BufferToFrame() {
            super();
        }

        @Override
        public void clear() { // idk why it crashes if we straight up call frame.freeBuffer() instead of doing this, nothing else should be holding onto it?
            for (AudioFrame frame : this.values()) FFmpeg.cleaner.register(frame, () -> frame.freeBuffer());
            super.clear();
        }
    }

    private class PlayingFrames extends ArrayDeque<AudioFrame> {
        public PlayingFrames() {
            super();
        }

        @Override
        public void clear() { // idk why it crashes if we straight up call frame.freeBuffer() instead of doing this, nothing else should be holding onto it?
            for (AudioFrame frame : this) FFmpeg.cleaner.register(frame, () -> frame.freeBuffer());
            super.clear();
        }
    }

    private final Map<Integer, AudioFrame> bufferToFrame = new BufferToFrame();
    private final Queue<AudioFrame> playingFrames = new PlayingFrames();
    private final Queue<Integer> availableBuffers;

    public VideoProjectorSpeakers(Projector videoProjector, Decoder decoder, AudioFrameBuffer audioFrameBuffer, float volume) {
        this.videoProjector = videoProjector;
        this.decoder = decoder;
        this.audioFrameBuffer = audioFrameBuffer;
        this.volumeActual = volume * VideoUtils.getSoundVolumeMult();
        this.volume = volume;

        this.availableBuffers = new ArrayDeque<>();

        this.channels = decoder.getAudioChannels();
        this.sampleRate = decoder.getSampleRate();
        this.format = (channels == 1) ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;

        this.isDone = false;
        this.currentAudioPts = 0;

        initOpenAL();
    }

    private void initOpenAL() {
        context = ALC10.alcGetCurrentContext();
        device = ALC10.alcGetContextsDevice(context);
        if (device == null) throw new IllegalStateException("Failed to get OpenAL device.");

        sourceId = AL10.alGenSources();
        AL10.alSourcef(sourceId, AL10.AL_GAIN, volumeActual);

        bufferIds = BufferUtils.createIntBuffer(NUM_BUFFERS);
        AL10.alGenBuffers(bufferIds);

        availableBuffers.clear();
        for (int i = 0; i < bufferIds.capacity(); i++) {
            availableBuffers.offer(bufferIds.get(i));
        }
    }

    private void updateStream() {
        if (isDone || paused) {
            return;
        }

        int processed = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_PROCESSED);
        for (int i = 0; i < processed; i++) {
            int bufferId = AL10.alSourceUnqueueBuffers(sourceId);
            if (AL10.alGetError() != AL10.AL_NO_ERROR) {
                logger.error("OpenAL error unqueueing buffer " + bufferId);
                continue;
            }
            availableBuffers.offer(bufferId);

            AudioFrame stale = bufferToFrame.remove(bufferId);
            if (stale != null) stale.freeBuffer();

            playingFrames.poll();
        }

        while (!availableBuffers.isEmpty()) {
            AudioFrame frame = getNextFrame();
            if (frame == null) {
                break;
            }

            int bufferId = availableBuffers.poll();
            AL10.alBufferData(bufferId, format, frame.buffer, sampleRate);
            AL10.alSourceQueueBuffers(sourceId, bufferId);
            bufferToFrame.put(bufferId, frame);
            playingFrames.offer(frame);
        }

        AudioFrame currentFrame = playingFrames.peek();
        if (currentFrame != null) {
            float offsetSeconds = AL10.alGetSourcef(sourceId, AL11.AL_SEC_OFFSET);
            long offsetMillis = (long) (offsetSeconds * 1000f);
            this.currentAudioPts = currentFrame.pts + offsetMillis;
        }

        int sourceState = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
        int queued = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_QUEUED);

        if (sourceState != AL10.AL_PLAYING && !paused && queued > 0) {
            AL10.alSourcePlay(sourceId);
        }

        if (finished && queued == 0) {
            isDone = true;
            stop();
        }
    }

    private AudioFrame getNextFrame() {
        synchronized(audioFrameBuffer) {
            return audioFrameBuffer.pop();
        }
    }
    
    private void prefillBuffers() {
        int buffersToFill = availableBuffers.size();
        for (int i = 0; i < buffersToFill; i++) {
            AudioFrame frame = getNextFrame();
            if (frame == null) break;

            int bufferId = availableBuffers.poll();

            AL10.alBufferData(bufferId, format, frame.buffer, sampleRate);
            AL10.alSourceQueueBuffers(sourceId, bufferId);

            bufferToFrame.put(bufferId, frame);
            playingFrames.offer(frame);
        }
    }

    @Override
    public void advance(float deltaTime, List<InputEventAPI> events) {
        if (Global.getCurrentState() != GameState.COMBAT) return;
        updateStream();
    }

    @Override
    public void advance(float deltaTime) {
        updateStream();
    }

    public void start() {
        paused = false;
        isDone = false;
        currentAudioPts = 0;

        prefillBuffers();

        AL10.alSourceRewind(sourceId);
        AL10.alSourcePlay(sourceId);
        Global.getSector().addTransientScript(this);
        Global.getCombatEngine().addPlugin(this);
    }

    public synchronized void play() {
        unpause();
    }

    public synchronized void pause() {
        if (paused) return;
        paused = true;
        AL10.alSourcePause(sourceId);
    }

    public synchronized void unpause() {
        if (!paused || isDone) return;
        paused = false;
        AL10.alSourcePlay(sourceId);
    }

    public synchronized void stop() {
        paused = true;

        int queued = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_QUEUED);
        while (queued > 0) {
            int bufferId = AL10.alSourceUnqueueBuffers(sourceId);
            if (AL10.alGetError() != AL10.AL_NO_ERROR) break;
            availableBuffers.offer(bufferId);

            AudioFrame stale = bufferToFrame.remove(bufferId);
            if (stale != null) stale.freeBuffer();

            queued--;
        }

        AL10.alSourceStop(sourceId);

        bufferToFrame.clear();
        playingFrames.clear();
        currentAudioPts = 0;
    }

    public synchronized void restart() {
        finish();
        finished = false;
        initOpenAL();
        start();
    }

    public synchronized void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
        this.volumeActual = Math.max(0f, Math.min(1f, volume) * VideoUtils.getSoundVolumeMult());
        AL10.alSourcef(sourceId, AL10.AL_GAIN, this.volumeActual);
    }

    public float getVolume() { return volume; }
    public void mute() { setVolume(0f); }

    public void finish() {
        isDone = true;
        finished = true;
        stop();

        AL10.alDeleteSources(sourceId);
        AL10.alDeleteBuffers(bufferIds);

        audioFrameBuffer.clear();
        bufferToFrame.clear();
        playingFrames.clear();
        availableBuffers.clear();

        Global.getSector().removeTransientScript(this);
        Global.getCombatEngine().removePlugin(this);
    }
    
    @Override
    public int getSourceId() {
        return this.sourceId;
    }

    @Override
    public Decoder getDecoder() {
        return this.decoder;
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public long getCurrentAudioPts() {
        return this.currentAudioPts;
    }

    public void setSoundDirection(Vector2f viewportLoc) {
        if (viewportLoc == null) return;
        AL10.alSource3f(sourceId, AL10.AL_POSITION, viewportLoc.x, 0f, viewportLoc.y);
    }

    public void resetSoundDirection() {
        AL10.alSource3f(sourceId, AL10.AL_POSITION, 0f, 0f, 0f);
    }
}