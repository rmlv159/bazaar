package com.r.bazaar.util;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.r.bazaar.MatchingRepo;
import com.r.bazaar.output.persist.DAO;
import com.r.bazaar.output.persist.PostgreSQLDAO;
import com.typesafe.config.Config;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutowireHelper {

    @Bean("conf")
    public Config appConfig() {
        //TODO remove?
        Config config = AppConfig.loadConfig("C:/Users/R/Desktop/bazaar/src/main/resources/app-conf.yml");
        return config;
    }

    @Bean("dao")
    public DAO createDAO() {
        return new PostgreSQLDAO();
    }

    @Bean("clientSystemMapping")
    public Table<String, String, String> createClientSystemMapping() {
        return HashBasedTable.create();
    }

    @Bean("choiceTypes")
    public Map<String, Set<String>> createChoiceTypes() {
        return Map.of("AmountStructure", Set.of("debit", "credit"));
    }
}
