package com.r.bazaar.entity.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;

//TODO warning, not the same as in IntelliSync, if you consider moving it, bare in mind not mingling json and pojo
public class Table {

    @JsonProperty("table-name")
    private String name;
    private ArrayList<Field> schema;
    @JsonProperty("sequenced-types")
    private ArrayList<String> sequencedTypes;

    public Table() {
    }

    public String getName() {
        return name;
    }

    public ArrayList<Field> getSchema() {
        return schema;
    }

    public ArrayList<String> getSequencedTypes() {
        return sequencedTypes;
    }
}
