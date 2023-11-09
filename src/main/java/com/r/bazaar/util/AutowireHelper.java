package com.r.bazaar.util;

import com.typesafe.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutowireHelper {

    @Bean("conf")
    public Config appConfig() {
        Config config = AppConfig.loadConfig("C:/Users/R/Desktop/bazaar/src/main/resources/app-conf.yml");
        return config;
    }
}
