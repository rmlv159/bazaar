package com.r.bazaar;

import com.google.common.collect.Table;
import com.r.bazaar.output.persist.DAO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class MatchingRepo {

    private final DAO dao;

    private final Table<String, String, String> clientSystemMapping;
//    Map<String, Row> systemDefaultFieldsToGenericFields = dao.getSystemDefaultToGeneric(
//        clientId, systemName, genericTableNames);

    public MatchingRepo(
        //injection with qualifier on the fields directly didn't work, strangely, despite it
        // working everywhere else
        @Qualifier("dao")
        DAO dao,
        @Qualifier("clientSystemMapping")
        Table<String, String, String> clientSystemMapping) {
        this.dao = dao;
        this.clientSystemMapping = clientSystemMapping;
    }

    public boolean containsSystemDefaultField(String qualifiedSystemDefaultField,
        int clientId, String systemName) {
        return true;
//        return clientSystemMapping.containsValue();
//        return systemDefaultFieldsToGenericFields.containsKey(qualifiedSystemDefaultField);
    }

    public void refresh() {
        dao.fillAllSystemSchemaMapping(clientSystemMapping);
        System.out.println("halt");
    }
}
