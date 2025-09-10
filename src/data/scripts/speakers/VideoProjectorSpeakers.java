package data.scripts.speakers;

import data.scripts.buffers.AudioFrameBuffer;
import data.scripts.decoder.Decoder;
import data.scripts.ffmpeg.AudioFrame;
import data.scripts.projector.Projector;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCcontext;
import org.lwjgl.openal.ALCdevice;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.input.InputEventAPI;

import org.apache.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
    
    private boolean isDone;

    private Projector videoProjector;
    private Decoder decoder;

    private float volume;
    private boolean paused = false;

    private long currentAudioPts;

    private int sampleRate;
    private int channels;
    private int format;

    private ALCdevice device;
    private ALCcontext context;
    private int sourceId;
    private IntBuffer bufferIds;

    private final AudioFrameBuffer audioFrameBuffer;
    private final Map<Integer, AudioFrame> bufferToFrame = new HashMap<>();
    private final Queue<AudioFrame> playingFrames = new ArrayDeque<>();
    private final Queue<Integer> availableBuffers = new ArrayDeque<>();

    public VideoProjectorSpeakers(Projector videoProjector, Decoder decoder, AudioFrameBuffer audioFrameBuffer, float volume) {
        this.decoder = decoder;
        this.volume = volume;

        this.audioFrameBuffer = audioFrameBuffer;

        this.channels = decoder.getAudioChannels();
        this.sampleRate = decoder.getSampleRate();
        this.format = (channels == 1) ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;

        initOpenAL();
    }

    private void initOpenAL() {
        context = ALC10.alcGetCurrentContext();
        device = ALC10.alcGetContextsDevice(context);
        if (device == null) throw new IllegalStateException("Failed to open default OpenAL device.");

        sourceId = AL10.alGenSources();

        bufferIds = BufferUtils.createIntBuffer(4);
        AL10.alGenBuffers(bufferIds);
        
        // Initialize available buffers pool
        availableBuffers.clear();
        for (int i = 0; i < bufferIds.capacity(); i++) {
            availableBuffers.offer(bufferIds.get(i));
        }

        AL10.alSourcef(sourceId, AL10.AL_GAIN, volume);
    }

    private AudioFrame getNextFrame() {
        AudioFrame frame;
        synchronized(audioFrameBuffer) {
            frame = audioFrameBuffer.pop();
        }
        return frame;
    }

    @Override
    public void advance(float deltaTime, List<InputEventAPI> events) {

    }

    @Override
    public void advance(float deltaTime) {

        currentAudioPts = 0;
    }

    public void start() {
        paused = false;

        this.sampleRate = decoder.getSampleRate();
        this.channels = decoder.getAudioChannels();

        Global.getSector().addTransientScript(this);
        if (AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
            AL10.alSourcePlay(sourceId);
        }
        
    }

    public void play() {
        paused = false;
        AL10.alSourcePlay(sourceId);
    }

    public void pause() {
        paused = true;
        AL10.alSourcePause(sourceId);
    }

    public void unpause() {
        paused = false;
        AL10.alSourcePlay(sourceId);
    }

    public void stop() {
        paused = true;
        AL10.alSourceStop(sourceId);

        int queued;
        while ((queued = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_QUEUED)) > 0) {
            int bufferId = AL10.alSourceUnqueueBuffers(sourceId);
            bufferToFrame.remove(bufferId);
            // Return buffer to available pool
            availableBuffers.offer(bufferId);
        }
        playingFrames.clear();
    }

    public void restart() {
        finish();
        initOpenAL();
        start();
    }

    public void setVolume(float volume) {
        this.volume = volume;
        AL10.alSourcef(sourceId, AL10.AL_GAIN, volume);
    }

    public float getVolume() { return volume; }
    public void mute() { setVolume(0f); }

    public void finish() {
        stop();

        AL10.alDeleteSources(sourceId);
        AL10.alDeleteBuffers(bufferIds);
        
        audioFrameBuffer.clear();
        bufferToFrame.clear();
        playingFrames.clear();
        availableBuffers.clear();
    }

    // unused - merely for debugging
    private ByteBuffer generateSilentBuffer(float durationSeconds) {
        int totalSamples = (int) (sampleRate * durationSeconds);
        int bytesPerSample = 2;
        ByteBuffer buffer = ByteBuffer.allocateDirect(totalSamples * channels * bytesPerSample);
        buffer.order(ByteOrder.nativeOrder());

        for (int i = 0; i < totalSamples * channels; i++) {
            buffer.putShort((short) 0);
        }

        buffer.flip();
        return buffer;
    }

    // unused - merely for debugging
    private ByteBuffer generateSineWave(float durationSeconds) {
        int totalSamples = (int) (sampleRate * durationSeconds);
        ByteBuffer buffer = ByteBuffer.allocateDirect(totalSamples * channels * 2).order(ByteOrder.nativeOrder());
    
        for (int i = 0; i < totalSamples; i++) {
            short sample = (short) (Math.sin(2.0 * Math.PI * 440f * i / sampleRate) * volume * Short.MAX_VALUE);
            for (int ch = 0; ch < channels; ch++) {
                buffer.putShort(sample);
            }
        }
    
        buffer.flip();
        return buffer;
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
}
