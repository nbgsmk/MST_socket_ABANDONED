package nbgsmk.msr.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConfigLoader {
    private static final Pattern ENVIRONMENT_PLACEHOLDER =
            Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)(:-([^}]*))?}");
    private ConfigLoader() {}

    public static AppConfig load(String externalPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream input = externalPath == null || externalPath.isBlank()
                ? ConfigLoader.class.getResourceAsStream("/application.yml")
                : Files.newInputStream(Path.of(externalPath))) {
            if (input == null) throw new IOException("application.yml was not found");
            String yaml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return mapper.readValue(expandEnvironment(yaml), AppConfig.class);
        }
    }

    private static String expandEnvironment(String value) {
        Matcher matcher = ENVIRONMENT_PLACEHOLDER.matcher(value);
        StringBuffer expanded = new StringBuffer();
        while (matcher.find()) {
            String configured = System.getenv(matcher.group(1));
            String replacement = configured != null && !configured.isBlank()
                    ? configured
                    : matcher.group(3) == null ? matcher.group(0) : matcher.group(3);
            matcher.appendReplacement(expanded, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(expanded);
        return expanded.toString();
    }
}
