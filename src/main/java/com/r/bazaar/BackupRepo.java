package com.r.bazaar;

import com.r.bazaar.entity.internal.Client;
import com.r.bazaar.input.Decoder;
import com.r.bazaar.lib.model.IntelliSyncConfig;
import com.r.bazaar.entity.model.Field;
import com.r.bazaar.output.persist.DAO;
import com.r.bazaar.output.persist.SystemSpecifics;
import com.r.bazaar.output.report.Packager;
import com.r.bazaar.lib.model.Backup;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class BackupRepo {

    //TODO implement per customer and eviction
    private final Map<String, ArrayList<String>> seqTypesCache = new HashMap<>();
    private final Map<String, ArrayList<Field>> schemaCache = new HashMap<>();
    private final Map<Integer, Client> clientCache;
    @Autowired
    private Decoder decoder;
    @Autowired
    private Packager packager;
    @Autowired
    private SystemSpecifics systemSpecifics;

    @Qualifier("dao")
    private DAO dao;
    private static final List<String> ID_FIELDS = List.of("customer_id", "backup_id", "company_code");

    public BackupRepo(DAO dao, SystemSpecifics systemSpecifics) {
        //TODO move this away from constructor and call externally
        this.dao = dao;
        this.systemSpecifics = systemSpecifics;
        clientCache = new HashMap<>();
        initConfigCache();
    }

    //TODO dummy for Spring, still needed?
//    public BackupRepo() {
//        this(null);
//    }

    private void initConfigCache() {
        for (var entry : dao.getTablesPerClient().entrySet()) {
            Integer clientId = entry.getKey();
            Client client = new Client();
            IntelliSyncConfig intelliSyncConfig = new IntelliSyncConfig();
            List<String> preparedTablesNames = entry.getValue().stream()
                .map(rawTableName -> {
                    String companyCode = dao.getCompanyCode(clientId);
                    client.setCompanyCode(companyCode);
                    String systemName = dao.getSystemName(clientId);
                    client.setSystemName(systemName);
                    if ("NAV18".equals(systemName)) {
                        return systemSpecifics.nav18(rawTableName, companyCode);
                    }
                    return "";
                })
                .collect(Collectors.toList());
            intelliSyncConfig.getRelevantTables().addAll(preparedTablesNames);
            client.setIntelliSyncConfig(intelliSyncConfig);
            clientCache.put(clientId, client);
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

    public void cacheSeqTypes(String tableName, ArrayList<String> seqTypes)
        throws IllegalArgumentException {
        if (seqTypesCache.containsKey(tableName)) {
            //TODO enable again once table cache is evicted after each backup
//            throw new IllegalArgumentException("table exists already");
        }
        seqTypesCache.put(tableName, seqTypes);
    }

    public void cacheSchema(String tableName, ArrayList<Field> schema) {
        if (schemaCache.containsKey(tableName)) {
            //TODO enable again once table cache is evicted after each backup
//            throw new IllegalArgumentException("table exists already");
        }
        schemaCache.put(tableName, schema);
    }

    public void saveRow(int clientId, String tableName, ArrayList<String> data) {
        Client client = clientCache.get(clientId);
        ArrayList<String> seqTypes = seqTypesCache.get(tableName);
        if (null == seqTypes) {
            throw new IllegalArgumentException("Table not present in schema");
        }
        //input validation has already been done
        String genericTableName = systemSpecifics.lookUpGenericTableName(client, tableName);
//        if ("gl_entries".equals(genericTableName)) {
        ArrayList<Field> schema = schemaCache.get(tableName);
        ArrayList<String> filteredFields = filter(clientId, client.getSystemName(),
            genericTableName, schema, data);
        dao.put(genericTableName, filteredFields, data);
//            packager.run(data);
    }

    private ArrayList<String> filter(int clientId, String systemName,
        String tableName, ArrayList<Field> schema, ArrayList<String> data) {

        Map<String, String> genericFieldsBySystemDefaultFields = dao.getGenericFieldsBySystemDefaultFields(
            clientId, systemName, tableName);
        Map<String, String> systemDefaultFieldsByGenericFields = dao.getSystemDefaultFieldsByGenericFields(
            clientId, systemName, tableName);

        ArrayList<String> defaultSystemFieldNames = schema.stream()
            .map(Field::getName)
            .collect(Collectors.toCollection(ArrayList::new));
        ArrayList<String> filteredDefaultSystemFieldNames = genericFieldsBySystemDefaultFields.values()
            .stream()
            .map(systemDefaultFieldsByGenericFields::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(ArrayList::new));
        ArrayList<String> filteredGenericFieldNames = defaultSystemFieldNames.stream()
            .map(genericFieldsBySystemDefaultFields::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(ArrayList::new));

        addIdFields(filteredGenericFieldNames);

        List<Integer> fieldIndexes = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            if (filteredDefaultSystemFieldNames.contains(defaultSystemFieldNames.get(i))) {
                fieldIndexes.add(i);
            }
        }
        ArrayList<String> temp = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            if (fieldIndexes.contains(i)) {
                temp.add(data.get(i));
            }
        }
        data.clear();
        data.addAll(temp);
        addGLValuesToBeDerivedLater(data);
        return filteredGenericFieldNames;
    }


    private void addIdFields(ArrayList<String> fields) {
        fields.addAll(ID_FIELDS);
    }

    private void addGLValuesToBeDerivedLater(ArrayList<String> data) {
        data.add("1");
        data.add("a4cd4efa98b553251cdef9345");
        data.add("1000");
    }

    public IntelliSyncConfig getIntelliSyncConfig(int clientId) {
        return clientCache.get(clientId).getIntelliSyncConfig(); //TODO make dynamic
    }

    public static List<String> getIdFields() {
        return ID_FIELDS;
    }
}
