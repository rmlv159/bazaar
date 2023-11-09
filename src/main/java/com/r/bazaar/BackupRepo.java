package com.r.bazaar;

import com.r.bazaar.input.Decoder;
import com.r.bazaar.lib.model.IntelliSyncConfig;
import com.r.bazaar.output.persist.DAO;
import com.r.bazaar.output.report.Packager;
import com.r.bazaar.lib.model.Backup;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class BackupRepo {

    //TODO implement per customer and eviction
    private final Map<String, ArrayList<String>> schemaCache = new HashMap<>();
    private final Map<Integer, IntelliSyncConfig> configCache;
    @Autowired
    private Decoder decoder;
    //TODO make separate
//            System.out.println(decoder.base64(type, data.get(i)));
    @Autowired
    private Packager packager;

    @Qualifier("dao")
    private DAO dao;

    public BackupRepo(DAO dao) {
        this.dao = dao;
        configCache = new HashMap<>();
        initConfigCache();
    }

    //TODO dummy for Spring, still needed?
//    public BackupRepo() {
//        this(null);
//    }

    private void initConfigCache() {
        for (var entry : dao.getTablesPerCustomer().entrySet()) {
            Integer clientId = entry.getKey();
            IntelliSyncConfig intelliSyncConfig = new IntelliSyncConfig();
            List<String> preparedTablesNames = entry.getValue().stream()
                .map(rawTableName -> {
                    String companyCode = dao.getCompanyCode(clientId);
                    String systemName = dao.getSystemName(clientId);
                    if ("NAV18".equals(systemName)) {
                        return new StringBuilder()
                            .append("[dbo].[")
                            .append(companyCode)
                            .append("$")
                            .append(rawTableName)
                            .append("]").toString();
                    }
                    return "";
                })
                .collect(Collectors.toList());
            intelliSyncConfig.getRelevantTables().addAll(preparedTablesNames);
            configCache.put(clientId, intelliSyncConfig);
        }
    }

    public List<Backup> getWeekly() {
        return List.of(
            new Backup(LocalDateTime.of(2023, 10, 20, 17, 0)),
            new Backup(LocalDateTime.of(2023, 10, 13, 17, 0)),
            new Backup(LocalDateTime.of(2023, 10, 6, 17, 0)),
            new Backup(LocalDateTime.of(2023, 10, 29, 17, 0)),
            new Backup(LocalDateTime.of(2023, 9, 22, 17, 0)),
            new Backup(LocalDateTime.of(2023, 9, 15, 17, 0))
        );
    }

    public void cacheSchema(String tableName, ArrayList<String> schema)
        throws IllegalArgumentException {
        if (schemaCache.containsKey(tableName)) {
            //TODO enable again once table cache is evicted after each backup
//            throw new IllegalArgumentException("table exists already");
        }
        schemaCache.put(tableName, schema);
    }

    public void saveRow(String tableName, ArrayList<String> data) {
        ArrayList<String> schema = schemaCache.get(tableName);
        if (null == schema) {
            throw new IllegalArgumentException("Table not present in schema");
        }
        //input validation has already been done
        for (int i = 0; i < data.size(); i++) {
            String type = schema.get(i);
            System.out.println(data.get(i));

//            packager.run(data);
        }
    }

    public IntelliSyncConfig getIntelliSyncConfig() {
        return configCache.get(1); //TODO make dynamic
    }
}
