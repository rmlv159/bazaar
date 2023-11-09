package com.r.bazaar.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
public class AppConfig {
    public static Config loadConfig(String configFile) {
        Yaml yaml = new Yaml();
        Config parsedConfig = ConfigFactory.parseString(yaml.load(configFile));
        return parsedConfig;
    }

    public static String thisEnvironment() {
        return System.getenv("THIS_ENV");
    }
}
