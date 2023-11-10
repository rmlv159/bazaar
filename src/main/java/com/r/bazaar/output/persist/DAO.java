package com.r.bazaar.output.persist;

import com.r.bazaar.entity.model.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface DAO {

    Map<Integer, List<String>> getTablesPerClient();

    String getCompanyCode(int clientId);

    void put(String tableName, ArrayList<String> fieldNames, ArrayList<String> data);

    String getSystemName(Integer clientId);

    Map<String, String> getGenericFieldsBySystemDefaultFields(int clientId, String systemName, String tableName);

    Map<String, String> getSystemDefaultFieldsByGenericFields(int clientId, String systemName, String tableName);

    String getGenericTableName(String defaultSystemTableName);
}
