package com.r.bazaar.output.persist;

import com.r.bazaar.entity.model.Field;
import com.r.bazaar.output.persist.PostgreSQLDAO.Row;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.Table;

public interface DAO {

    Map<Integer, Set<String>> getTablesPerClient();

    String getCompanyCode(int clientId);

    void put(String tableName, List<String> fieldNames, List<String> data);

    long putAndGetId(String tableName, List<String> fieldNames, List<String> data);

    String getSystemName(Integer clientId);

    Map<String, Set<String>> getGenericTablesToGenericFields(int clientId, String systemName, List<String> tableNames);

    Table<String, String, String> getSystemSchemaMappingTable(int clientId, String systemName, List<String> tableNames);

    Map<String, Row> getSystemDefaultToGeneric(int clientId, String systemName, List<String> tableNames);

    Map<String, String> getGenericFieldsToSystemDefaultFields(int clientId, String systemName, List<String> tableNames);

    List<String> getGenericTableNames(String defaultSystemTableName);

    void fillAllSystemSchemaMapping(final Table<String, String, String> container);

    List<Field> getSchema(String genericTableName);
}
