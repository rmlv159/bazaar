package com.r.bazaar.util;

import com.r.bazaar.output.persist.DAO;
import com.r.bazaar.output.persist.PostgreSQLDAO;
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

    @Bean("dao")
    public DAO createDAO() {
        return new PostgreSQLDAO();
    }
}
