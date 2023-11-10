package com.r.bazaar.output.persist;

import com.r.bazaar.entity.model.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public Map<Integer, List<String>> getTablesPerClient() {
        Map<Integer, List<String>> map = new HashMap<>();
        String query = "SELECT c.id,\n"
            + "\t\ts.system_default_table_name \n"
            + "FROM erp.client_system_mapping c\n"
            + "LEFT JOIN erp.system_schema_mapping s\n"
            + "\tON c.system_id = s.id";
        try (
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int customerId = resultSet.getInt("id");
                List<String> tables = map.getOrDefault(customerId, new ArrayList<>());
                tables.add(resultSet.getString("system_default_table_name"));
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
    public void put(String tableName, ArrayList<String> fieldNames, ArrayList<String> data) {
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
    public Map<String, String> getGenericFieldsBySystemDefaultFields(int clientId,
        String systemName, String tableName) {
        //TODO make nicer
        Map<String, String> genericFieldsBySystemDefaultFields = new HashMap<>();
        String query = "SELECT a.generic_field_name, a.system_default_field_name "
            + "FROM erp.system_schema_mapping a "
            + "LEFT JOIN erp.systems b "
            + " ON a.system_id = b.id "
            + "WHERE b.system_name = ? "
            + "AND a.generic_table_name = ?";
        try (
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            preparedStatement.setString(1, systemName);
            preparedStatement.setString(2, tableName);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                genericFieldsBySystemDefaultFields.put(
                    resultSet.getString("system_default_field_name"),
                    resultSet.getString("generic_field_name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return genericFieldsBySystemDefaultFields;
    }

    @Override
    public Map<String, String> getSystemDefaultFieldsByGenericFields(int clientId,
        String systemName, String tableName) {
        //TODO make nicer
        Map<String, String> systemDefaultFieldsByGenericFields = new HashMap<>();
        String query = "SELECT a.generic_field_name, a.system_default_field_name "
            + "FROM erp.system_schema_mapping a "
            + "LEFT JOIN erp.systems b "
            + " ON a.system_id = b.id "
            + "WHERE b.system_name = ? "
            + "AND a.generic_table_name = ?";
        try (
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            preparedStatement.setString(1, systemName);
            preparedStatement.setString(2, tableName);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                systemDefaultFieldsByGenericFields.put(
                    resultSet.getString("generic_field_name"),
                    resultSet.getString("system_default_field_name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return systemDefaultFieldsByGenericFields;
    }

    @Override
    public String getGenericTableName(String defaultSystemTableName) {
        //TODO make nicer
        String tableName = "";
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
                tableName = resultSet.getString("generic_table_name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tableName;
    }
}

