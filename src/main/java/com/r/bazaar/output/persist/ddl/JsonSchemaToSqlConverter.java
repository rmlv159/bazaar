package com.r.bazaar.output.persist.ddl;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public class JsonSchemaToSqlConverter {

    //TODO remove if this turns out to be a dead end, see OneNote about story about highsource and jsonschema not very popular
    public static String convertJsonSchemaToSql(String schemaFilePath, String tableName) throws IOException {
        InputStream inputStream = new FileInputStream(schemaFilePath);
        JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
        Schema schema = SchemaLoader.load(rawSchema);

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("CREATE TABLE ").append(tableName).append(" (");

        // Traverse properties in the JSON Schema and generate SQL statements
        JSONObject properties = rawSchema.getJSONObject("properties");
        Iterator<String> propertyNames = properties.keys();

        while (propertyNames.hasNext()) {
            String propertyName = propertyNames.next();
            JSONObject property = properties.getJSONObject(propertyName);

            // Add logic to map JSON Schema properties to SQL data types
            // Modify this based on your specific requirements and SQL CE syntax
            String sqlDataType = mapJsonTypeToSql(property);

            sqlBuilder.append(propertyName).append(" ").append(sqlDataType).append(",");

            // Add constraints, primary key, etc., based on your requirements
            // ...

        }

        sqlBuilder.deleteCharAt(sqlBuilder.length() - 1); // Remove the trailing comma
        sqlBuilder.append(");");

        return sqlBuilder.toString();
    }

    private static String mapJsonTypeToSql(JSONObject property) {
        // Add logic to map JSON Schema types to SQL data types
        // Modify this based on your specific requirements and SQL CE syntax
        return "VARCHAR(255)";
    }
}

