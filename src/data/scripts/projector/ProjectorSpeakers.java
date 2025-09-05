package data.scripts.projector;

import data.scripts.decoder.Decoder;
import data.scripts.ffmpeg.AudioFrame;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCcontext;
import org.lwjgl.openal.ALCdevice;

import org.apache.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class ProjectorSpeakers {
    private static final Logger logger = Logger.getLogger(ProjectorSpeakers.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    private Projector videoProjector;
    private Decoder decoder;

    private float volume;
    private boolean paused = false;

    private int sampleRate;
    private int channels;
    private int format;

    private AudioFrame currentFrame;

    private ALCdevice device;
    private ALCcontext context;
    private int sourceId;
    private IntBuffer bufferIds;

    public ProjectorSpeakers(Projector videoProjector, Decoder decoder, float volume) {
        if (true) throw new UnsupportedOperationException("UNIMPLEMENTED");
        this.videoProjector = videoProjector;
        this.decoder = decoder;
        this.volume = volume;

        this.format = (channels == 1) ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;

        initOpenAL();
    }

    private void initOpenAL() {
        context = ALC10.alcGetCurrentContext();
        device = ALC10.alcGetContextsDevice(context);
        if (device == null) throw new IllegalStateException("Failed to open default OpenAL device.");

        sourceId = AL10.alGenSources();

        bufferIds = BufferUtils.createIntBuffer(1);
        AL10.alGenBuffers(bufferIds);

        AL10.alSourcef(sourceId, AL10.AL_GAIN, volume);
    }

    public long advance(AudioFrame frame) {
        if (frame == null) return 0;

        int processed = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_PROCESSED);
        while (processed-- > 0) {
            int bufferId = AL10.alSourceUnqueueBuffers(sourceId);
            if (queueNextChunk(bufferId, frame)) {
                AL10.alSourceQueueBuffers(sourceId, bufferId);
            }
        }

        int state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
        if (state != AL10.AL_PLAYING) {
            AL10.alSourcePlay(sourceId);
        }

        return currentFrame.pts; // return currently playing frame pts for decoder audio clock
    }

    private boolean queueNextChunk(int bufferId, AudioFrame frame) {
        if (frame == null) return false;

        currentFrame = frame;

        AL10.alBufferData(bufferId, format, frame.buffer, sampleRate);
        return true;
    }

    public void start() {
        paused = false;

        this.sampleRate = decoder.getSampleRate();
        this.channels = decoder.getAudioChannels();

        // prime buffer
        AL10.alBufferData(bufferIds.get(0), format, generateSilentBuffer(0.001f), sampleRate);
        AL10.alSourceQueueBuffers(sourceId, bufferIds);

        if (AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
            AL10.alSourcePlay(sourceId);
        }
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

        // Unqueue all buffers
        int queued;
        while ((queued = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_QUEUED)) > 0) {
            AL10.alSourceUnqueueBuffers(sourceId);
        }
    }

    public void restart() {
        cleanup();
        initOpenAL();
        start();
    }

    public void setVolume(float volume) {
        this.volume = volume;
        AL10.alSourcef(sourceId, AL10.AL_GAIN, volume);
    }

    public float getVolume() { return volume; }
    public void mute() { setVolume(0f); }

    public void cleanup() {
        stop();
        AL10.alDeleteSources(sourceId);
        AL10.alDeleteBuffers(bufferIds);
    }

    private ByteBuffer generateSilentBuffer(float durationSeconds) {
        int totalSamples = (int) (sampleRate * durationSeconds);
        int bytesPerSample = 2; // 16-bit PCM
        ByteBuffer buffer = ByteBuffer.allocateDirect(totalSamples * channels * bytesPerSample);
        buffer.order(ByteOrder.nativeOrder());

        for (int i = 0; i < totalSamples * channels; i++) {
            buffer.putShort((short) 0);
        }

        buffer.flip();
        return buffer;
    }

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
}
