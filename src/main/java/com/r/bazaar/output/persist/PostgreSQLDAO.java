package com.r.bazaar.output.persist;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.r.bazaar.entity.model.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PostgreSQLDAO implements DAO {

    //TODO string is getting logged! Remove!
    private final String connectionString;

    //TODO avoid injection attacks
//TODO remove schema from here and add to table name

    //TODO schema needs to be passed in from higher up
    private static final String SCHEMA = "saf_t_v1";

    public PostgreSQLDAO() {
        String db = "SecuReplic";
        this.connectionString = "jdbc:postgresql://"
            + "localhost/"
            + db
            + "?user=" + System.getenv("POSTGRE_SQL_USER")
            + "&password=" + System.getenv("POSTGRE_SQL_PW")
            + "&ssl=true;";
    }

    @Override
    public Map<Integer, Set<String>> getTablesPerClient() {
        Map<Integer, Set<String>> map = new HashMap<>();
        String query = "SELECT c.id,\n"
            + "\t\ts.system_default_table_name \n"
            + "FROM erp.client_system_mapping c\n"
            + "LEFT JOIN erp.system_schema_mapping s\n"
            + "\tON c.system_id = s.system_id";
        try (
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int customerId = resultSet.getInt("id");
                Set<String> tables = map.getOrDefault(customerId, new HashSet<>());
                //TODO RK implement Optionals at some point
                String systemDefaultTableName = resultSet.getString("system_default_table_name");
                if (null != systemDefaultTableName) {
                    tables.add(systemDefaultTableName);
                }
                map.put(customerId, tables);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public String getCompanyCode(int clientId) {
        //TODO make nicer
        String cpCode = "";
        String query = "SELECT company_code_name FROM erp.company_codes WHERE client_id = ?";
        try (
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            preparedStatement.setInt(1, clientId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                cpCode = resultSet.getString("company_code_name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cpCode;
    }

    @Override
    public void put(String tableName, List<String> fieldNames, List<String> data) {
        String fieldNamesStr = String.join(", ", fieldNames);
        String dataValues = data.stream()
            .map(value -> "'" + value + "'")
            .collect(Collectors.joining(", "));

        //TODO avoid injection attacks
        //TODO remove schema from here and add to table name
        String insertSql = String.format("INSERT INTO " + SCHEMA + ".%s (%s) VALUES (%s);",
            tableName,
            fieldNamesStr, dataValues);

        try (Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            //TODO Handle exceptions appropriately
        }
    }

    @Override
    public long putAndGetId(String tableName, List<String> fieldNames, List<String> data) {
        String fieldNamesStr = String.join(", ", fieldNames);
        long generatedKey = 0L;
        String dataValues = data.stream()
            .map(value -> "'" + value + "'")
            .collect(Collectors.joining(", "));

        String insertSql = String.format("INSERT INTO " + SCHEMA + ".%s (%s) VALUES (%s);",
            tableName,
            fieldNamesStr, dataValues);

        try (Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(insertSql,
                Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.execute();
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            generatedKeys.next();
            generatedKey = generatedKeys.getLong(1);
        } catch (SQLException e) {
            e.printStackTrace();
            //TODO Handle exceptions appropriately
        }
        return generatedKey;
    }

    @Override
    public String getSystemName(Integer clientId) {
        //TODO make nicer
        String systemName = "";
        String query = "select system_name\n"
            + "from erp.client_system_mapping m\n"
            + "left join erp.systems s\n"
            + "\ton m.system_id = s.id "
            + "where m.id = ?";
        try (
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            preparedStatement.setInt(1, clientId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                systemName = resultSet.getString("system_name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return systemName;
    }

    @Override
    public Map<String, Set<String>> getGenericTablesToGenericFields(int clientId,
        String systemName, List<String> genericTableNames) {
        //TODO make nicer
        Map<String, Set<String>> genericTablesToGenericFields = new HashMap<>();
        String query = "SELECT distinct a.generic_table_name, a.generic_field_name "
            + "FROM erp.system_schema_mapping a "
            + "LEFT JOIN erp.systems b "
            + " ON a.system_id = b.id "
            + "WHERE b.system_name = ? "
            + "AND a.generic_table_name in (%s)";
        query = String.format(query,
            genericTableNames.stream()
                .map(v -> "?")
                .collect(Collectors.joining(", ")));
        try (
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            preparedStatement.setString(1, systemName);
            int i = 2;
            for (var t : genericTableNames) {
                preparedStatement.setString(i++, t);
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String genericFieldName = resultSet.getString("generic_field_name");
                if (null == genericFieldName) {
                    continue;
                }
                String genericTableName = resultSet.getString("generic_table_name");
                Set<String> genericFields = genericTablesToGenericFields.getOrDefault(
                    genericTableName,
                    new HashSet<>());
                genericFields.add(genericFieldName);
                genericTablesToGenericFields.put(
                    genericTableName,
                    genericFields);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return genericTablesToGenericFields;
    }

    //TODO RK remove
    @Override
    public Table<String, String, String> getSystemSchemaMappingTable(int clientId,
        String systemName, List<String> tableNames) {
        Table<String, String, String> weightedGraph = HashBasedTable.create();
        weightedGraph.put("v1", "v2", "4");
        weightedGraph.put("v1", "v3", "2");
        weightedGraph.put("v2", "v3", "5");
        return null;
    }

    @Override
    public Map<String, GenericTableRow> getSystemDefaultToGeneric(int clientId,
        String systemName, List<String> tableNames) {
        //TODO make nicer
        Map<String, GenericTableRow> systemDefaultToGeneric = new HashMap<>();
        String query =
            "SELECT a.generic_table_name, "
                + "a.generic_field_name, "
                + "a.system_default_table_name, "
                + "a.systemDefaultFieldName, "
                + "a.schema, "
                + "a.aux_relation_target_system_default, "
                + "a.aux_relation_source_generic, "
                + "a.cache_1to1_generic, "
                + "a.cache_fact_generic "
                + "FROM erp.system_schema_mapping a "
                + "LEFT JOIN erp.systems b "
                + " ON a.system_id = b.id "
                + "WHERE b.system_name = ? "
                + "AND a.generic_table_name in (%s)";
        query = String.format(query,
            tableNames.stream()
                .map(v -> "?")
                .collect(Collectors.joining(", ")));
        try (
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            preparedStatement.setString(1, systemName);
            int i = 2;
            for (var t : tableNames) {
                preparedStatement.setString(i++, t);
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String systemDefaultFieldName = resultSet.getString("systemDefaultFieldName");
                if (null == systemDefaultFieldName) {
                    continue;
                }
                GenericTableRow genericTableRow = new GenericTableRow(
                    resultSet.getString("generic_table_name"),
                    resultSet.getString("generic_field_name"),
                    resultSet.getString("schema"),
                    resultSet.getString("aux_relation_target_system_default"),
                    resultSet.getString("aux_relation_source_generic"),
                    resultSet.getString("cache_1to1_generic"),
                    resultSet.getString("cache_fact_generic")
                );
                systemDefaultToGeneric.put(
                    resultSet.getString("system_default_table_name") + "." + systemDefaultFieldName,
                    genericTableRow);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return systemDefaultToGeneric;
    }

    public record GenericTableRow(String genericTableName,
                                  String genericFieldName,
                                  String schema,
                                  String auxRelationTargetSystemDefault,
                                  String auxRelationSourceGeneric,
                                  String cache1To1Generic,
                                  String cacheFactGeneric) {

    }

    @Override
    public Map<String, String> getGenericFieldsToSystemDefaultFields(int clientId,
        String systemName, List<String> genericTableNames) {
        //TODO make nicer
        Map<String, String> genericFieldsToSystemDefaultFields = new HashMap<>();
        String query = "SELECT a.generic_field_name, a.systemDefaultFieldName "
            + "FROM erp.system_schema_mapping a "
            + "LEFT JOIN erp.systems b "
            + " ON a.system_id = b.id "
            + "WHERE b.system_name = ? "
            + "AND a.generic_table_name in (%s)";
        query = String.format(query,
            genericTableNames.stream()
                .map(v -> "?")
                .collect(Collectors.joining(", ")));
        try (
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            preparedStatement.setString(1, systemName);
            int i = 2;
            for (var t : genericTableNames) {
                preparedStatement.setString(i++, t.strip());
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                genericFieldsToSystemDefaultFields.put(
                    resultSet.getString("generic_field_name"),
                    resultSet.getString("systemDefaultFieldName"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return genericFieldsToSystemDefaultFields;
    }

    @Override
    public List<String> getGenericTableNames(String defaultSystemTableName, String systemName) {
        //TODO make nicer
        List<String> tableNames = new ArrayList<>();
        String query = "SELECT generic_table_name "
            + "FROM erp.system_schema_mapping m "
            + "LEFT JOIN erp.systems s "
            + "     ON m.system_id = s.id "
            + "WHERE system_default_table_name = ? "
            + "AND s.system_name = ?;";
        try (
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            preparedStatement.setString(1, defaultSystemTableName);
            preparedStatement.setString(2, systemName);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                tableNames.add(resultSet.getString("generic_table_name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tableNames;
    }

    @Override
    public void fillAllSystemSchemaMapping(final Table<String, String, String> container) {
        String query = "SELECT a.id,"
            + "a.system_id, "
            + "a.generic_table_name, "
            + "a.generic_field_name, "
            + "a.system_default_table_name, "
            + "a.systemDefaultFieldName, "
            + "a.schema, "
            + "a.aux_relation_target_system_default, "
            + "a.aux_relation_source_generic, "
            + "a.cache_1to1_generic, "
            + "a.cache_fact_generic, "
            + "b.system_name " //TODO RK later swap with id, further upstream
            + "FROM erp.system_schema_mapping a "
            + "LEFT JOIN erp.systems b "
            + " ON a.system_id = b.id";
        try (
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String tableIndex = resultSet.getString("generic_table_name");
                container.put(tableIndex, "system_id",
                    Optional.ofNullable(resultSet.getString("system_id")).orElse(
                        "")); //TODO, how to handle this? Is null fine or should it be "" or should fail loudly?
                container.put(tableIndex, "generic_field_name",
                    Optional.ofNullable(resultSet.getString("generic_field_name")).orElse(""));
                container.put(tableIndex, "system_default_table_name",
                    Optional.ofNullable(resultSet.getString("system_default_table_name"))
                        .orElse(""));
                container.put(tableIndex, "systemDefaultFieldName",
                    Optional.ofNullable(resultSet.getString("systemDefaultFieldName"))
                        .orElse(""));
                container.put(tableIndex, "schema",
                    Optional.ofNullable(resultSet.getString("schema")).orElse(""));
                container.put(tableIndex, "auxRelationTargetSystemDefault",
                    Optional.ofNullable(resultSet.getString("aux_relation_target_system_default"))
                        .orElse(""));
                container.put(tableIndex, "auxRelationSourceGeneric",
                    Optional.ofNullable(resultSet.getString("aux_relation_source_generic"))
                        .orElse(""));
                container.put(tableIndex, "cache_1to1_generic",
                    Optional.ofNullable(resultSet.getString("cache_1to1_generic")).orElse(""));
                container.put(tableIndex, "cache_fact_generic",
                    Optional.ofNullable(resultSet.getString("cache_fact_generic")).orElse(""));
                container.put(tableIndex, "system_id",
                    Optional.ofNullable(resultSet.getString("system_id")).orElse(""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Field> getSchema(String genericTableName) {
        List<Field> schema = new ArrayList<>();
        String query = "SELECT column_name, data_type\n"
            + "    FROM information_schema.columns\n"
            + "WHERE table_name = ?; ";
        try (
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            //TODO RK will lowercase always be correct?
            preparedStatement.setString(1, genericTableName.toLowerCase());
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                schema.add(new Field(resultSet.getString("column_name"),
                    resultSet.getString("data_type")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return schema;
    }

    @Override
    public void getAndProcessAuxRelationsData(int clientId, String backupId,
        List<String> auxRelationsFieldNames, Consumer<AuxRelationRow> consumer) {
        String query = "SELECT %s"
            + "    FROM " + SCHEMA + ".aux_relations"
            + "    WHERE meta_customer_id = ?"
            + "    AND meta_backup_id = ?;";
        query = String.format(query, String.join(", ", auxRelationsFieldNames));
        try (
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            //TODO RK will lowercase always be correct?
            preparedStatement.setString(1, String.valueOf(clientId));
            preparedStatement.setString(2, backupId);
            ResultSet resultSet = preparedStatement.executeQuery();
            String companyCode = getCompanyCode(clientId);
            String systemName = getSystemName(clientId);
            while (resultSet.next()) {
                String systemDefaultTableName = resultSet.getString(
                    "system_default_table_name");
                String systemDefaultFieldName = resultSet.getString("system_default_field_name");
                String value = resultSet.getString("value");
                String auxRelationTargetSystemDefault = resultSet.getString(
                    "aux_relation_target_system_default");
                String auxRelationSourceGeneric = resultSet.getString(
                    "aux_relation_source_generic");
                String cache1to1Generic = resultSet.getString("cache_1to1_generic");
                String cacheFactGeneric = resultSet.getString("cache_fact_generic");

                //TODO bad bad bad, new objects, garbage?
                AuxRelationRow row = new AuxRelationRow(systemDefaultTableName,
                    systemDefaultFieldName, value,
                    auxRelationTargetSystemDefault, auxRelationSourceGeneric,
                    cache1to1Generic, cacheFactGeneric,
                    companyCode, clientId, systemName, backupId);

                consumer.accept(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public AuxRelationRow getOneAuxRelationRow(int clientId, String backupId,
        List<String> auxRelationsFieldNames, String systemDefaultTableName,
        String systemDefaultFieldName, String value) {

        Map<String, List<String>> data = new HashMap<>();
        String query = "SELECT %s"
            + "    FROM " + SCHEMA + ".aux_relations"
            + "    WHERE meta_customer_id = ?"
            + "    AND meta_backup_id = ?"
            + "    AND system_default_table_name = ?"
            + "    AND system_default_field_name = ?"
            + "    AND value = ?;";
        query = String.format(query, String.join(", ", auxRelationsFieldNames));
        try (
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            //TODO RK will lowercase always be correct?
            preparedStatement.setString(1, String.valueOf(clientId));
            preparedStatement.setString(2, backupId);
            preparedStatement.setString(3, systemDefaultTableName);
            preparedStatement.setString(4, systemDefaultFieldName);
            preparedStatement.setString(5, value);

            ResultSet resultSet = preparedStatement.executeQuery();

            String companyCode = getCompanyCode(clientId);
            String systemName = getSystemName(clientId);

            resultSet.next();

            value = resultSet.getString("value");
            String auxRelationTargetSystemDefault = resultSet.getString(
                "aux_relation_target_system_default");
            String auxRelationSourceGeneric = resultSet.getString(
                "aux_relation_source_generic");
            String cache1to1Generic = resultSet.getString("cache_1to1_generic");
            String cacheFactGeneric = resultSet.getString("cache_fact_generic");

            //TODO bad bad bad, new objects, garbage?
            return new AuxRelationRow(resultSet.getString("system_default_table_name"),
                resultSet.getString("system_default_field_name"), value,
                auxRelationTargetSystemDefault, auxRelationSourceGeneric, cache1to1Generic,
                cacheFactGeneric, companyCode, clientId, systemName, backupId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void removeAuxRelationRow(AuxRelationRow row) {
        //TODO RK impl
    }

    @Override
    public void updateGenericTable(GenericTableRow originalGenericRow, String lookUpId,
        String insertId) {
        String idField = originalGenericRow.auxRelationSourceGeneric + "_id";
        String query = "UPDATE " + SCHEMA + "." + originalGenericRow.genericTableName
            + " SET " + idField + "=?"
            + " WHERE " + +" =?";
        query = String.format(query, String.join(", ", auxRelationsFieldNames));
        try (
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            preparedStatement.setString(1, insertId);
            ResultSet resultSet = preparedStatement.executeQuery();
            String companyCode = getCompanyCode(clientId);
            String systemName = getSystemName(clientId);
            while (resultSet.next()) {
                String systemDefaultTableName = resultSet.getString(
                    "system_default_table_name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getConnectionString() {
        return connectionString;
    }
}

