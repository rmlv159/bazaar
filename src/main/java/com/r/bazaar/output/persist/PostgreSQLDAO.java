package com.r.bazaar.output.persist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PostgreSQLDAO implements DAO {

    //TODO string is getting logged! Remove!
    private final String connectionString;

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
        String insertSql = String.format("INSERT INTO erp.%s (%s) VALUES (%s);", tableName,
            fieldNamesStr, dataValues);

        try (Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exceptions appropriately
        }
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
    public Map<String, String> getGenericTablesToGenericFields(int clientId,
        String systemName, List<String> genericTableNames) {
        //TODO make nicer
        Map<String, String> genericTablesToGenericFields = new HashMap<>();
        String query = "SELECT a.generic_table_name, a.generic_field_name "
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
                genericTablesToGenericFields.put(
                    resultSet.getString("generic_table_name"),
                    resultSet.getString("generic_field_name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return genericTablesToGenericFields;
    }

    @Override
    public Map<String, Row> getSystemDefaultToGeneric(int clientId,
        String systemName, List<String> tableNames) {
        //TODO make nicer
        Map<String, Row> systemDefaultToGeneric = new HashMap<>();
        String query =
            "SELECT a.generic_table_name, a.generic_field_name, a.system_default_field_name "
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
                systemDefaultToGeneric.put(
                    resultSet.getString("system_default_field_name"),
                    new Row(resultSet.getString("generic_table_name"),
                        resultSet.getString("generic_field_name")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return systemDefaultToGeneric;
    }

    public record Row(String genericTableName, String genericFieldName) {

    }

    @Override
    public Map<String, String> getGenericFieldsToSystemDefaultFields(int clientId,
        String systemName, List<String> genericTableNames) {
        //TODO make nicer
        Map<String, String> genericFieldsToSystemDefaultFields = new HashMap<>();
        String query = "SELECT a.generic_field_name, a.system_default_field_name "
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
                    resultSet.getString("system_default_field_name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return genericFieldsToSystemDefaultFields;
    }

    @Override
    public List<String> getGenericTableNames(String defaultSystemTableName) {
        //TODO make nicer
        List<String> tableNames = new ArrayList<>();
        String query = "SELECT distinct generic_table_name "
            + "FROM erp.system_schema_mapping "
            + "WHERE system_default_table_name = ?";
        try (
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            preparedStatement.setString(1, defaultSystemTableName);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                tableNames.add(resultSet.getString("generic_table_name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tableNames;
    }

    public String getConnectionString() {
        return connectionString;
    }
}

