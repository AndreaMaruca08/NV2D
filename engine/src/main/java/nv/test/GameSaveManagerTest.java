package nv.test;

import nv.core.io.AppPathUtils;
import nv.core.io.GameSaveManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameSaveManager Unit Tests")
public class GameSaveManagerTest {

    private static final String TEST_SAVE_FILE = "save/unit_test_save.bin";

    public static class TestSaveData implements Serializable {
        public String playerName;
        public int score;
        public float health;

        // No-arg constructor required by Kryo
        public TestSaveData() {}

        public TestSaveData(String playerName, int score, float health) {
            this.playerName = playerName;
            this.score = score;
            this.health = health;
        }
    }

    @BeforeEach
    void setUp() {
        GameSaveManager.initialize(TEST_SAVE_FILE);
    }

    @AfterEach
    void tearDown() {
        File file = AppPathUtils.resolvePath(TEST_SAVE_FILE).toFile();
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("Test saving and reading game save data with Kryo serialization")
    void testSaveAndGet() {
        TestSaveData data = new TestSaveData("Hero", 9999, 87.5f);
        GameSaveManager.save(data);

        TestSaveData loaded = GameSaveManager.get(TestSaveData.class);

        assertNotNull(loaded, "Loaded save data should not be null");
        assertEquals("Hero", loaded.playerName);
        assertEquals(9999, loaded.score);
        assertEquals(87.5f, loaded.health, 0.0001f);
    }

    @Test
    @DisplayName("Test reading non-existent save file returns null safely")
    void testReadNonExistentFile() {
        GameSaveManager.initialize("save/non_existent_file_12345.bin");
        TestSaveData loaded = GameSaveManager.get(TestSaveData.class);
        assertNull(loaded, "Reading non-existent save file should return null");
    }
}
