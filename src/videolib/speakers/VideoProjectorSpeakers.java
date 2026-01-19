package videolib.speakers;

import videolib.ffmpeg.AudioFrame;
import videolib.ffmpeg.FFmpeg;
import videolib.buffers.AudioFrameBuffer;

import videolib.decoder.Decoder;
import videolib.projector.Projector;

import videolib.util.VideoUtils;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCcontext;
import org.lwjgl.openal.ALCdevice;
import org.lwjgl.opengl.Display;
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

    private static final boolean IS_LINUX = FFmpeg.IS_LINUX;
    private static final double SYNC_THRESHOLD = 0.02;
    private static final double NOSYNC_THRESHOLD = 1.0;
    private static final float MAX_PITCH_ADJUSTMENT = 0.03f;
    private static final float PITCH_SMOOTHING = 0.1f;

    private static final int NUM_BUFFERS = IS_LINUX ? 6 : 3;

    private boolean isDone = false;

    @FunctionalInterface
    private interface StreamUpdateDelegate {
        void update(float deltaTime);
    }

    private final StreamUpdateDelegate updateDelegate;
    
    private final VideoProjectorSpeakers self = this;
    private final Projector videoProjector;
    private final Decoder decoder;
    private final AudioFrameBuffer audioFrameBuffer;

    private float volume;
    private float volumeActual;
    private boolean paused = true;
    private boolean finished = false;

    private long currentAudioPts;
    
    private double wallClockSeconds;
    private float currentPitch = 1.0f;

    private final int sampleRate;
    private final int channels;
    private final int format;

    private ALCdevice device;
    private ALCcontext context;
    private int sourceId;
    private IntBuffer bufferIds;

    private final Map<Integer, AudioFrame> bufferToFrame = new HashMap<>();
    private final Queue<AudioFrame> playingFrames = new ArrayDeque<>();
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
        this.wallClockSeconds = 0;

        initOpenAL();

        // if (IS_LINUX) {
            this.updateDelegate = (deltaTime) -> {
                synchronized(self) {
                    wallClockSeconds += deltaTime;
                    updateStream();
                    syncAudioState(); // I DO NOT LIKE THIS
                }
            };
        // } else {
        //     this.updateDelegate = (deltaTime) -> {
        //         synchronized(this) {
        //             updateStream();
        //         }
        //     };
        // }
    }

    private void initOpenAL() {
        context = ALC10.alcGetCurrentContext();
        device = ALC10.alcGetContextsDevice(context);
        if (device == null) throw new IllegalStateException("Failed to get OpenAL device.");

        sourceId = AL10.alGenSources();
        AL10.alSourcef(sourceId, AL10.AL_GAIN, volumeActual);
        AL10.alSourcef(sourceId, AL10.AL_PITCH, 1.0f);

        bufferIds = BufferUtils.createIntBuffer(NUM_BUFFERS);
        AL10.alGenBuffers(bufferIds);

        availableBuffers.clear();
        for (int i = 0; i < bufferIds.capacity(); i++) {
            availableBuffers.offer(bufferIds.get(i));
        }
    }

    private synchronized void updateStream() {
        int processed = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_PROCESSED);
        for (int i = 0; i < processed; i++) {
            int bufferId = AL10.alSourceUnqueueBuffers(sourceId);
            if (AL10.alGetError() != AL10.AL_NO_ERROR) {
                logger.error("OpenAL error unqueueing buffer " + bufferId);
                continue;
            }
            availableBuffers.offer(bufferId);
    
            AudioFrame stale = bufferToFrame.remove(bufferId);
            if (stale != null) currentAudioPts = stale.pts;
    
            playingFrames.poll();
        }
    
        while (!availableBuffers.isEmpty()) {
            AudioFrame frame = getNextFrame();
            if (frame == null) break;
    
            int bufferId = availableBuffers.poll();
            AL10.alBufferData(bufferId, format, frame.buffer, sampleRate);
            AL10.alSourceQueueBuffers(sourceId, bufferId);

            frame.freeBuffer();
    
            bufferToFrame.put(bufferId, frame);
            playingFrames.offer(frame);
        }
    
        int sourceState = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
        int queued = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_QUEUED);
    
        if (sourceState != AL10.AL_PLAYING && !paused && queued > 0) {
            AL10.alSourcePlay(sourceId);
        }

        AudioFrame currentFrame = playingFrames.peek();

        // int sampleOffset = AL10.alGetSourcei(sourceId, AL11.AL_SAMPLE_OFFSET);
        // if (currentFrame != null) {
        //     long offsetUs = (long) ((sampleOffset / (double) sampleRate) * 1000000.0);
            // currentAudioPts = currentFrame.pts;
        // }

        if (currentFrame != null) {
            float offsetSeconds = AL10.alGetSourcef(sourceId, AL11.AL_SEC_OFFSET);
            long offsetMillis = (long) (offsetSeconds * 1000f);
            this.currentAudioPts = currentFrame.pts + offsetMillis;
        }
    
        if (finished && queued == 0) {
            isDone = true;
            stop();
        }
    }

    private synchronized void syncAudioState() {
        if (playingFrames.isEmpty()) return;

        double audioTime = currentAudioPts / 1_000_000.0;
        double masterTime = wallClockSeconds;
        double diff = audioTime - masterTime; // Positive: Audio is ahead. Negative: Audio is behind

        float maxPitchAdjustment = diff > 0 ? 0.005f : MAX_PITCH_ADJUSTMENT;

        if (Math.abs(diff) > NOSYNC_THRESHOLD) {
            wallClockSeconds = audioTime;
            diff = 0;
        }

        float targetPitch = 1.0f;

        if (Math.abs(diff) > SYNC_THRESHOLD) {
            float correction = (float) (diff * 1.5); 
            
            if (correction > maxPitchAdjustment) correction = maxPitchAdjustment;
            if (correction < -maxPitchAdjustment) correction = -maxPitchAdjustment;
            
            targetPitch = 1.0f - correction; 
        }

        // Smooth pitch changes to avoid audio warble
        currentPitch = currentPitch * (1f - PITCH_SMOOTHING) + targetPitch * PITCH_SMOOTHING;

        if (currentPitch < 0.5f) currentPitch = 0.5f;
        if (currentPitch > 2.0f) currentPitch = 2.0f;

        AL10.alSourcef(sourceId, AL10.AL_PITCH, currentPitch);
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

            frame.freeBuffer();

            bufferToFrame.put(bufferId, frame);
            playingFrames.offer(frame);
        }
    }

    @Override
    public void advance(float deltaTime, List<InputEventAPI> events) {
        if (Global.getCurrentState() != GameState.COMBAT || paused || isDone) return;
        this.updateDelegate.update(deltaTime);
    }

    @Override
    public void advance(float deltaTime) {
        if (paused || isDone) return;
        this.updateDelegate.update(deltaTime);
    }

    @Override
    public void start() {
        paused = false;
        isDone = false;
        currentAudioPts = 0;
        wallClockSeconds = 0;
        currentPitch = 1.0f;

        prefillBuffers();

        AL10.alSourceRewind(sourceId);
        AL10.alSourcePlay(sourceId);
        Global.getSector().addTransientScript(this);
        Global.getCombatEngine().addPlugin(this);
    }

    @Override
    public synchronized void play() {
        unpause();
    }

    @Override
    public synchronized void pause() {
        if (paused) return;
        paused = true;
        AL10.alSourcePause(sourceId);
    }

    @Override
    public synchronized void unpause() {
        if (!paused || isDone) return;
        paused = false;
        AL10.alSourcePlay(sourceId);
    }

    @Override
    public synchronized void stop() {
        paused = true;

        int queued = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_QUEUED);
        while (queued > 0) {
            int bufferId = AL10.alSourceUnqueueBuffers(sourceId);
            if (AL10.alGetError() != AL10.AL_NO_ERROR) break;
            availableBuffers.offer(bufferId);

            queued--;
        }

        AL10.alSourceStop(sourceId);

        bufferToFrame.clear();
        playingFrames.clear();

        currentAudioPts = 0;
        currentPitch = 1.0f;
        wallClockSeconds = 0;
        AL10.alSourcef(sourceId, AL10.AL_PITCH, 1.0f);
    }

    @Override
    public synchronized void restart() {
        finish();
        finished = false;
        initOpenAL();
        start();
    }

    @Override
    public synchronized void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
        this.volumeActual = Math.max(0f, Math.min(1f, volume) * VideoUtils.getSoundVolumeMult());
        AL10.alSourcef(sourceId, AL10.AL_GAIN, this.volumeActual);
    }

    @Override public float getVolume() { return volume; }
    @Override public void mute() { setVolume(0f); }

    @Override
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
    public synchronized long getCurrentAudioPts() {
        return this.currentAudioPts;
    }

    @Override
    public void setSoundDirection(Vector2f viewportLoc) {
        if (viewportLoc == null) return;
        AL10.alSource3f(sourceId, AL10.AL_POSITION, viewportLoc.x, 0f, viewportLoc.y);
    }

    @Override
    public void resetSoundDirection() {
        AL10.alSource3f(sourceId, AL10.AL_POSITION, 0f, 0f, 0f);
    }

    @Override
    public synchronized void notifySeek(long targetUs) {
        this.currentAudioPts = targetUs;
        this.wallClockSeconds = targetUs / 1_000_000.0;
        this.currentPitch = 1.0f;
    }
}