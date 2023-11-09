package com.r.bazaar.output.persist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostgreSQLDAO implements DAO {

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
    public Map<Integer, List<String>> getTablesPerCustomer() {
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
    public <T> void put(T t) {
        String insertSql = "insert blab lla";
        try (Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {
            preparedStatement.executeUpdate();
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

}

