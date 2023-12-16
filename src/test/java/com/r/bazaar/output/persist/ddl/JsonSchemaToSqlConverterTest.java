package com.r.bazaar.output.persist.ddl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class JsonSchemaToSqlConverterTest {

    //TODO remove if this turns out to be a dead end, see OneNote about story about highsource and jsonschema not very popular
    @Disabled
    @Test
    void convertJsonSchemaToSql() {
        try {
            String schemaFilePath = "src/main/resources/schema/v1/financial_taxation_standardauditfile_dk.jsonschema";
            String tableName = "YourTableName";

            String createTableSql = JsonSchemaToSqlConverter.convertJsonSchemaToSql(schemaFilePath, tableName);
            System.out.println(createTableSql);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}