package com.r.bazaar.output.persist;

import com.r.bazaar.output.persist.PostgreSQLDAO.Row;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DAO {

    Map<Integer, Set<String>> getTablesPerClient();

    String getCompanyCode(int clientId);

    void put(String tableName, List<String> fieldNames, List<String> data);

    String getSystemName(Integer clientId);

    Map<String, String> getGenericTablesToGenericFields(int clientId, String systemName, List<String> tableNames);

    Map<String, Row> getSystemDefaultToGeneric(int clientId, String systemName, List<String> tableNames);

    Map<String, String> getGenericFieldsToSystemDefaultFields(int clientId, String systemName, List<String> tableNames);

    List<String> getGenericTableNames(String defaultSystemTableName);
}
