package com.r.bazaar;

import com.r.bazaar.entity.internal.Client;
import com.r.bazaar.input.Decoder;
import com.r.bazaar.lib.model.IntelliSyncConfig;
import com.r.bazaar.entity.model.Field;
import com.r.bazaar.output.persist.DAO;
import com.r.bazaar.output.persist.PostgreSQLDAO.Row;
import com.r.bazaar.output.persist.SystemSpecifics;
import com.r.bazaar.output.report.Packager;
import com.r.bazaar.lib.model.Backup;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
            Set<String> preparedTablesNames = entry.getValue().stream()
                .map(rawTableName -> {
                    //TODO RK improve performance and cache single dao calls
                    String companyCode = dao.getCompanyCode(clientId);
                    client.setCompanyCode(companyCode);
                    String systemName = dao.getSystemName(clientId);
                    client.setSystemName(systemName);
                    if ("NAV18".equals(systemName)) {
                        return systemSpecifics.nav18(rawTableName, companyCode);
                    }
                    return "";
                })
                .collect(Collectors.toSet());
            intelliSyncConfig.getRelevantTables().addAll(preparedTablesNames);
            client.
                setIntelliSyncConfig(intelliSyncConfig);
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

    public void saveRow(int clientId, String tableNameReceived, ArrayList<String> dataReceived) {
        Client client = clientCache.get(clientId);
        ArrayList<String> seqTypes = seqTypesCache.get(tableNameReceived);
        if (null == seqTypes) {
            throw new IllegalArgumentException("Table not present in schema");
        }
        //input validation has already been done
        List<String> genericTableNames = systemSpecifics.lookUpGenericTableNames(client, tableNameReceived);
        ArrayList<Field> schemaReceived = schemaCache.get(tableNameReceived);
        Map<String, Submission> filteredFieldsAndTables = filter(clientId, client.getSystemName(),
            genericTableNames, schemaReceived, dataReceived);

        for (var pair : filteredFieldsAndTables.entrySet()) {
            dao.put(pair.getKey(), pair.getValue().filteredGenericFieldNames, pair.getValue().data);
//            packager.run(data);
        }
    }

    protected record Submission(List<String> filteredGenericFieldNames,
                                List<String> data) {

        //TODO instead we need a queue here
    }

    protected Map<String, Submission> filter(int clientId, String systemName,
        List<String> genericTableNames, ArrayList<Field> schema, ArrayList<String> data) {

        Map<String, Row> systemDefaultFieldsToGenericFields = dao.getSystemDefaultToGeneric(
            clientId, systemName, genericTableNames);
        Map<String, String> genericTablesToGenericFields = dao.getGenericTablesToGenericFields(
            clientId, systemName, genericTableNames);
        Map<String, String> genericFieldsToSystemDefaultFields = dao.getGenericFieldsToSystemDefaultFields(
            clientId, systemName, genericTableNames);

        Map<String, Submission> submissions = new HashMap<>();
        for (Map.Entry<String, String> genericTableEntry : genericTablesToGenericFields.entrySet()) {
            ArrayList<String> sortedData = new ArrayList<>();
            List<String> sortedGenericFieldNames = new ArrayList<>();
            for (String datum : data) {
                if (genericFieldsToSystemDefaultFields.containsKey(genericTableEntry.getValue())) {
                    sortedData.add(datum);
                    sortedGenericFieldNames.add(genericTableEntry.getValue());
                }
            }
            submissions.put(genericTableEntry.getKey(),
                new Submission(sortedGenericFieldNames, sortedData));
        }
        //TODO bad, new objects
        return submissions;
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
