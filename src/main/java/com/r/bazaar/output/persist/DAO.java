package com.r.bazaar.output.persist;

import java.util.List;
import java.util.Map;

public interface DAO {

    Map<Integer, List<String>> getTablesPerCustomer();

    String getCompanyCode(int clientId);

    <T> void put(T t);

    String getSystemName(Integer clientId);
}
