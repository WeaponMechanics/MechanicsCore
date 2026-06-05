package me.deecaad.core.file;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SerializerTest {

    private File file;
    private SnakeYamlConfig config;

    @BeforeEach
    void setUp() throws Exception {
        file = new File("test-config.yml");
        config = SnakeYamlConfig.ofText(new String(
            getClass().getResourceAsStream("/test-config.yml").readAllBytes(), StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        file = null;
        config = null;
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    public void test_parseInvalid(int i) {
        SerializeData data = new SerializeData(file, "Squares.Invalid." + i, config);
        assertThrows(SerializerException.class, () -> data.of().serialize(Square.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    public void test_parseValid(int i) {
        SerializeData data = new SerializeData(file, "Squares.Invalid." + i, config);
        assertThrows(SerializerException.class, () -> data.of().serialize(Square.class));
    }
}
