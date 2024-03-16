package com.r.bazaar.output.persist;

import com.r.bazaar.entity.model.Field;
import com.r.bazaar.output.persist.PostgreSQLDAO.GenericTableRow;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.Table;
import java.util.function.Consumer;

public interface DAO {

    record AuxRelationRow(String systemDefaultTableName,
                          String systemDefaultFieldName,
                          String value,
                          String auxRelationTargetSystemDefault,
                          String auxRelationSourceGeneric,
                          String cache1To1Generic,
                          String cacheFactGeneric,
                          String meta_company_code, int clientId, String systemName,
                          String backupId) {
    }

    Map<Integer, Set<String>> getTablesPerClient();

    String getCompanyCode(int clientId);

    void put(String tableName, List<String> fieldNames, List<String> data);

    long putAndGetId(String tableName, List<String> fieldNames, List<String> data);

    String getSystemName(Integer clientId);

    Map<String, Set<String>> getGenericTablesToGenericFields(int clientId, String systemName,
        List<String> tableNames);

    Table<String, String, String> getSystemSchemaMappingTable(int clientId, String systemName,
        List<String> tableNames);

    Map<String, GenericTableRow> getSystemDefaultToGeneric(int clientId, String systemName,
        List<String> tableNames);

    Map<String, String> getGenericFieldsToSystemDefaultFields(int clientId, String systemName,
        List<String> tableNames);

    List<String> getGenericTableNames(String defaultSystemTableName, String s);

    void fillAllSystemSchemaMapping(final Table<String, String, String> container);

    List<Field> getSchema(String genericTableName);

    void getAndProcessAuxRelationsData(int clientId, String backupId,
        List<String> auxRelationsFieldNames, Consumer<AuxRelationRow> consumer);

    AuxRelationRow getOneAuxRelationRow(int clientId, String backupId,
        List<String> auxRelationsFieldNames, String systemDefaultTableName,
        String systemDefaultFieldName, String value);

    void removeAuxRelationRow(AuxRelationRow row);

    void updateGenericTable(GenericTableRow originalGenericRow, String first, String last);
}
