package com.r.bazaar.output.persist;

import com.r.bazaar.entity.internal.Client;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SystemSpecifics {

    @Qualifier("dao")
    private DAO dao;

    public SystemSpecifics(DAO dao) {
        this.dao = dao;
    }

    public String lookUpGenericTableName(Client client, String tableName) {
        String systemName = client.getSystemName();
        String companyCode = client.getCompanyCode();
        if ("NAV18".equals(systemName)) {
            String defaultSystemTableName = tableName.replace("[dbo].[", "")
                .replace(companyCode, "")
                .replace("$", "")
                .replace("]", "");
            String genericTableName = dao.getGenericTableName(defaultSystemTableName);
            return genericTableName;
//        }
        }
        return null;
    }

    public String nav18(String rawTableName, String companyCode) {
        return new StringBuilder()
            .append("[dbo].[")
            .append(companyCode)
            .append("$")
            .append(rawTableName)
            .append("]").toString();
    }

}
