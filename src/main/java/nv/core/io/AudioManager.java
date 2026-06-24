package nv.core.io;

import nv.core.annotations.EngineCore;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static nv.core.errors.NvLogger.logEngine;
import static nv.core.errors.NvLogger.logErr;

/**
 * Static utility class that handles audio playback for WAV, MP3, and OGG files using OpenAL.
 * Includes a built-in cache to prevent reloading files from disk.
 * <p>
 * Supports:
 * - Audio buffer caching
 * - Source caching
 * - Volume control from 0 to 100
 *
 * @author Andrea Maruca
 * @since 1.1
 */
@EngineCore
@SuppressWarnings("unused")
public final class AudioManager {

    private static final Map<String, Integer> bufferCache = new ConcurrentHashMap<>(10);
    private static final Map<String, Integer> activeSources = new ConcurrentHashMap<>(10);

    /**
     * Stores volumes using OpenAL gain values.
     * 0.0 = mute
     * 1.0 = max
     */
    private static final Map<String, Float> volumeCache = new ConcurrentHashMap<>(10);

    private static final float DEFAULT_VOLUME = 1.0f;
    private static final String PREFIX = "audio/";
    private static long audioDevice;
    private static long audioContext;

    private AudioManager() {
    }

    /**
     * Initializes OpenAL device and context.
     */
    public static void init() {
        audioDevice = ALC10.alcOpenDevice((ByteBuffer) null);

        if (audioDevice == 0) {
            logErr("Failed to open the default OpenAL audio device.");
            return;
        }

        org.lwjgl.openal.ALCCapabilities alcCapabilities = ALC.createCapabilities(audioDevice);
        audioContext = ALC10.alcCreateContext(audioDevice, (int[]) null);

        if (audioContext == 0) {
            logErr("Failed to create OpenAL context.");
            ALC10.alcCloseDevice(audioDevice);
            return;
        }

        ALC10.alcMakeContextCurrent(audioContext);
        AL.createCapabilities(alcCapabilities);

    }

    /**
     * Clears all OpenAL buffers, sources, and destroys the context/device.
     */
    public static void cleanup() {
        for (int sourceId : activeSources.values()) {
            AL10.alSourceStop(sourceId);
            AL10.alDeleteSources(sourceId);
        }

        for (int bufferId : bufferCache.values()) {
            AL10.alDeleteBuffers(bufferId);
        }

        activeSources.clear();
        bufferCache.clear();
        volumeCache.clear();

        if (audioContext != 0) {
            ALC10.alcMakeContextCurrent(0);
            ALC10.alcDestroyContext(audioContext);
        }

        if (audioDevice != 0) {
            ALC10.alcCloseDevice(audioDevice);
        }
    }

    /**
     * Loads an audio file into the cache.
     *
     * @param filePath file to load
     */
    public static void load(String filePath) {
        String fullPath = PREFIX + filePath;

        if (bufferCache.containsKey(fullPath)) {
            return;
        }

        int bufferId = loadAudioFile(fullPath);

        if (bufferId != -1) {
            bufferCache.put(fullPath, bufferId);
        } else {
            logErr("Failed to preload audio: " + fullPath);
        }
    }

    /**
     * Plays an audio file continuously on a loop.
     *
     * @param filePath audio file
     */
    public static void playLoop(String filePath) {
        int sourceId = getOrCreateSource(PREFIX + filePath);

        if (sourceId == -1) {
            return;
        }

        AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_TRUE);
        AL10.alSourcePlay(sourceId);
    }

    /**
     * Stops playback for a specific audio file.
     */
    public static void stop(String filePath) {
        Integer sourceId = activeSources.get(PREFIX + filePath);

        if (sourceId != null) {
            AL10.alSourceStop(sourceId);
        }
    }

    /**
     * Changes volume of an audio source.
     *
     * @param filePath audio file
     * @param volume   value between 0 and 100
     */
    public static void setVolume(String filePath, int volume) {
        if (volume < 0 || volume > 100) {
            throw new IllegalArgumentException("Volume must be between 0 and 100");
        }

        String path = PREFIX + filePath;
        float gain = volume / 100f;

        volumeCache.put(path, gain);
        Integer source = activeSources.get(path);

        if (source != null) {
            AL10.alSourcef(source, AL10.AL_GAIN, gain);
        }
    }

    /**
     * Returns current volume (0-100).
     */
    public static int getVolume(String filePath) {
        float gain = volumeCache.getOrDefault(PREFIX + filePath, DEFAULT_VOLUME);
        return Math.round(gain * 100);
    }

    /**
     * Helper to retrieve or initialize a source and its cached buffer.
     */
    private static int getOrCreateSource(String filePath) {
        int bufferId;

        if (bufferCache.containsKey(filePath)) {
            bufferId = bufferCache.get(filePath);
        } else {
            bufferId = loadAudioFile(filePath);

            if (bufferId == -1) {
                logErr("Failed to load audio file: " + filePath);
                return -1;
            }
            bufferCache.put(filePath, bufferId);
        }

        return activeSources.computeIfAbsent(filePath, path -> {
            int sourceId = AL10.alGenSources();
            AL10.alSourcei(sourceId, AL10.AL_BUFFER, bufferId);

            float volume = volumeCache.getOrDefault(path, DEFAULT_VOLUME);
            AL10.alSourcef(sourceId, AL10.AL_GAIN, volume);

            return sourceId;
        });
    }

    /**
     * Detects file extension and routes it to the correct decoder.
     */
    private static int loadAudioFile(String filePath) {
        String lowerCasePath = filePath.toLowerCase();

        if (lowerCasePath.endsWith(".ogg")) {
            return loadOGG(filePath);
        } else if (lowerCasePath.endsWith(".wav") || lowerCasePath.endsWith(".mp3")) {
            return loadViaJavaSound(filePath);
        }

        logErr("Unsupported audio format: " + filePath);
        return -1;
    }

    /**
     * Decodes OGG files using LWJGL STBVorbis.
     */
    private static int loadOGG(String filePath) {
        ByteBuffer fileBuffer = null;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            fileBuffer = ioResourceToByteBuffer(filePath);
            IntBuffer error = stack.mallocInt(1);

            long decoder = STBVorbis.stb_vorbis_open_memory(fileBuffer, error, null);

            if (decoder == 0) {
                logErr("STBVorbis failed: " + filePath + " error " + error.get(0));
                MemoryUtil.memFree(fileBuffer);
                return -1;
            }

            org.lwjgl.stb.STBVorbisInfo info = org.lwjgl.stb.STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(decoder, info);

            int channels = info.channels();
            int sampleRate = info.sample_rate();
            int samplesLength = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);

            ShortBuffer rawAudio = MemoryUtil.memAllocShort(samplesLength * channels);
            STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, rawAudio);
            STBVorbis.stb_vorbis_close(decoder);

            int format = channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
            int bufferId = AL10.alGenBuffers();

            AL10.alBufferData(bufferId, format, rawAudio, sampleRate);

            MemoryUtil.memFree(rawAudio);
            MemoryUtil.memFree(fileBuffer);

            return bufferId;

        } catch (Exception e) {
            logErr("OGG loading failed: " + e.getMessage());
            if (fileBuffer != null) {
                MemoryUtil.memFree(fileBuffer);
            }
            return -1;
        }
    }

    /**
     * Decodes WAV and MP3 using JavaSound SPI.
     */
    private static int loadViaJavaSound(String filePath) {
        try (InputStream is = AudioManager.class.getClassLoader().getResourceAsStream(filePath)) {
            if (is == null) {
                logErr("Resource not found: " + filePath);
                return -1;
            }

            try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(is))) {
                AudioFormat baseFormat = in.getFormat();
                AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false
                );

                try (AudioInputStream decodedIn = AudioSystem.getAudioInputStream(decodedFormat, in)) {
                    byte[] audioBytes = decodedIn.readAllBytes();
                    ByteBuffer buffer = MemoryUtil.memAlloc(audioBytes.length);

                    buffer.put(audioBytes).flip();

                    int format = decodedFormat.getChannels() == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
                    int bufferId = AL10.alGenBuffers();

                    AL10.alBufferData(bufferId, format, buffer, (int) decodedFormat.getSampleRate());
                    MemoryUtil.memFree(buffer);

                    return bufferId;
                }
            }
        } catch (Exception e) {
            logErr("JavaSound loading failed: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Loads a native ByteBuffer from resources.
     */
    private static ByteBuffer ioResourceToByteBuffer(String resource) throws Exception {
        try (InputStream is = AudioManager.class.getClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resource);
            }

            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);

            buffer.put(bytes).flip();
            return buffer;
        }
    }
}