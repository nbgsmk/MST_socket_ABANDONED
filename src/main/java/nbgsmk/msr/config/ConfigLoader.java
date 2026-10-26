package nbgsmk.msr.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigLoader {
    private ConfigLoader() {}

    public static AppConfig load(String externalPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream input = externalPath == null || externalPath.isBlank()
                ? ConfigLoader.class.getResourceAsStream("/application.yml")
                : Files.newInputStream(Path.of(externalPath))) {
            if (input == null) throw new IOException("application.yml was not found");
            return mapper.readValue(input, AppConfig.class);
        }
    }
}
