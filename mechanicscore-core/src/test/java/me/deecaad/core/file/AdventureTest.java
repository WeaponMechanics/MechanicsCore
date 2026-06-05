package me.deecaad.core.file;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AdventureTest {

    private File file;
    private SnakeYamlConfig config;

    @BeforeEach
    void setUp() throws Exception {
        file = new File("adventure_config.yml");
        config = SnakeYamlConfig.ofText(new String(getClass().getResourceAsStream("/adventure_config.yml").readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        file = null;
        config = null;
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    public void test_parse(int i) throws SerializerException {
        SerializeData data = new SerializeData(file, "Key", config);

        String actual = data.of("Input_" + i).assertExists().getAdventure().get();
        String expected = data.of("Output_" + i).assertExists().get(String.class).get();

        assertEquals(expected, actual);
    }
}
