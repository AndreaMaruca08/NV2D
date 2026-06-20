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

import static nv.core.errors.NvLogger.*;

/**
 * Static utility class that handles audio playback for WAV, MP3, and OGG files using OpenAL.
 * Includes a built-in cache to prevent reloading files from disk.
 *
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class AudioManager {

    private static final Map<String, Integer> bufferCache = new ConcurrentHashMap<>();
    private static final Map<String, Integer> activeSources = new ConcurrentHashMap<>();

    private static final String PREFIX = "audio/";

    private AudioManager() {}

    private static long audioDevice;
    private static long audioContext;

    public static void init() {
        // 1. Open the default audio device (returns a long pointer)
        audioDevice = ALC10.alcOpenDevice((java.nio.ByteBuffer) null);
        if (audioDevice == 0) {
            logErr("Failed to open the default OpenAL audio device.");
            return;
        }

        // 2. Create device capabilities
        org.lwjgl.openal.ALCCapabilities alcCapabilities = ALC.createCapabilities(audioDevice);

        // 3. Create the audio context (returns a long pointer)
        audioContext = ALC10.alcCreateContext(audioDevice, (int[]) null);
        if (audioContext == 0) {
            logErr("Failed to create OpenAL context.");
            ALC10.alcCloseDevice(audioDevice);
            return;
        }
        ALC10.alcMakeContextCurrent(audioContext);

        // 4. Create context capabilities (This fixes your IllegalStateException!)
        AL.createCapabilities(alcCapabilities);

        logEngine("OpenAL Audio Engine initialized successfully.");
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

        // Destroy context and device using long handles
        if (audioContext != 0) {
            ALC10.alcMakeContextCurrent(0);
            ALC10.alcDestroyContext(audioContext);
        }
        if (audioDevice != 0) {
            ALC10.alcCloseDevice(audioDevice);
        }
    }

    /**
     * loads an audio file in the cache
     * @param filePath file to load
     */
    public static void load(String filePath) {
        String fullPath = PREFIX + filePath;

        // If already cached, do nothing
        if (bufferCache.containsKey(fullPath)) {
            return;
        }

        int bufferId = loadAudioFile(fullPath);
        if (bufferId != -1) {
            bufferCache.put(fullPath, bufferId);
        } else {
            logErr("Failed to pre-load audio file: " + fullPath);
        }
    }
    /**
     * Plays an audio file continuously on a loop.
     * @param filePath Path to the audio file (WAV, MP3, OGG)
     */
    public static void playLoop(String filePath) {
        int sourceId = getOrCreateSource(PREFIX + filePath);
        if (sourceId == -1) return;

        AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_TRUE);
        AL10.alSourcePlay(sourceId);
    }

    /**
     * Stops playback for a specific audio file (works for both single play and loops).
     */
    public static void stop(String filePath) {
        Integer sourceId = activeSources.get(PREFIX + filePath);
        if (sourceId != null) {
            AL10.alSourceStop(sourceId);
        }
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
            if (bufferId != -1) {
                bufferCache.put(filePath, bufferId);
            } else {
                logErr("Failed to load audio file: " + filePath);
                return -1;
            }
        }

        return activeSources.computeIfAbsent(filePath, path -> {
            int sourceId = AL10.alGenSources();
            AL10.alSourcei(sourceId, AL10.AL_BUFFER, bufferId);
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
     * Decodes OGG files using LWJGL's native STBVorbis binding via raw resource memory buffers.
     */
    private static int loadOGG(String filePath) {
        ByteBuffer fileBuffer = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            fileBuffer = ioResourceToByteBuffer(filePath);

            IntBuffer error = stack.mallocInt(1);
            long decoder = STBVorbis.stb_vorbis_open_memory(fileBuffer, error, null);
            if (decoder == 0) {
                logErr("STBVorbis decoding failed for: " + filePath + " (Error code: " + error.get(0) + ")");
                MemoryUtil.memFree(fileBuffer);
                return -1;
            }

            // Allocazione della struct sullo stack nativo e passaggio come secondo parametro
            org.lwjgl.stb.STBVorbisInfo info = org.lwjgl.stb.STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(decoder, info);

            int channels = info.channels();
            int sampleRate = info.sample_rate();
            int samplesLength = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);

            ShortBuffer rawAudio = MemoryUtil.memAllocShort(samplesLength * channels);
            STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, rawAudio);
            STBVorbis.stb_vorbis_close(decoder);

            int format = (channels == 1) ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
            int bufferId = AL10.alGenBuffers();
            AL10.alBufferData(bufferId, format, rawAudio, sampleRate);

            MemoryUtil.memFree(rawAudio);
            MemoryUtil.memFree(fileBuffer);
            return bufferId;
        } catch (Exception e) {
            logErr("STBVorbis loading failed for: " + filePath + " specific: " + e.getMessage());
            if (fileBuffer != null) MemoryUtil.memFree(fileBuffer);
            return -1;
        }
    }

    /**
     * Decodes WAV and MP3 files from resource InputStreams using JavaSound SPI.
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

                    int format = (decodedFormat.getChannels() == 1) ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
                    int bufferId = AL10.alGenBuffers();
                    AL10.alBufferData(bufferId, format, buffer, (int) decodedFormat.getSampleRate());

                    MemoryUtil.memFree(buffer);
                    return bufferId;
                }
            }
        } catch (Exception e) {
            logErr("JavaSound loading failed for: " + filePath + " specific: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Helper to load an unmanaged native byte buffer from resources for STB consumption.
     */
    private static ByteBuffer ioResourceToByteBuffer(String resource) throws Exception {
        try (InputStream is = AudioManager.class.getClassLoader().getResourceAsStream(resource)) {
            if (is == null) throw new IllegalArgumentException("Resource not found: " + resource);
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        }
    }
}