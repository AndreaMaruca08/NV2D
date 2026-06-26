package nv.core.io;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import nv.core.annotations.EngineCore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static nv.core.errors.NvLogger.logWarn;

/**
 * <p>Class used for saving game data such as character info or preferences</p>
 * @since 1.1
 * @author Andrea Maruca
 */

@EngineCore
@SuppressWarnings("unused")
public final class GameSaveManager {
    private static Kryo kryo;
    private static String fileName = "save.bin";

    public static void initialize(String saveFileName){
        kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        fileName = saveFileName;
    }

    public static <T>T get(Class<T> classToRead){
        File file = new File(fileName);

        if (!file.exists()) {
            logWarn("File not found when trying to read save file: " + fileName);
            return null;
        }
        try (Input input = new Input(new GZIPInputStream(new FileInputStream(file)))) {
            return kryo.readObject(input, classToRead);
        } catch (IOException e) {
            logWarn("File not found when trying to read save file: " + fileName);
            return null;
        }
    }

    public static void save(Object objectToSave){
        File file = new File(fileName);

        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();

        try (Output output = new Output(new GZIPOutputStream(new FileOutputStream(file)))) {
            kryo.writeObject(output, objectToSave);
        } catch (IOException e) {
            logWarn("File not found when trying to write save file: " + fileName);
        }
    }
}