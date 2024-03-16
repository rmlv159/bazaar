package com.r.bazaar.output.persist;

import com.r.bazaar.entity.internal.Client;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SystemSpecifics {

    @Qualifier("dao")
    private DAO dao;

    public SystemSpecifics(DAO dao) {
        this.dao = dao;
    }

    public List<String> lookUpGenericTableNames(Client client, String tableName) {
        //TODO RK improve performance, this needs to be cached per client and system, probably when a new backup is initiated
        String systemName = client.getSystemName();
        String companyCode = client.getCompanyCode();
        if ("NAV18".equals(systemName)) {
            String defaultSystemTableName = tableName.replace("[dbo].[", "")
                .replace(companyCode, "")
                .replace("$", "")
                .replace("]", "");
            List<String> genericTableNames = dao.getGenericTableNames(defaultSystemTableName,
                row.systemName());
            return genericTableNames;
//        }
        }
        return null;
    }

    public String BuildNAV18TableName(String rawTableName, String companyCode) {
        return new StringBuilder()
            .append("[dbo].[")
            .append(companyCode)
            .append("$")
            .append(rawTableName)
            .append("]").toString();
    }

}
