package com.r.bazaar.entity.internal;

import com.r.bazaar.lib.model.IntelliSyncConfig;

public class Client {

    private int id;
    private String name;
    private String companyCode;
    private String systemName;
    private IntelliSyncConfig intelliSyncConfig;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public IntelliSyncConfig getIntelliSyncConfig() {
        return intelliSyncConfig;
    }

    public void setIntelliSyncConfig(IntelliSyncConfig intelliSyncConfig) {
        this.intelliSyncConfig = intelliSyncConfig;
    }
}
