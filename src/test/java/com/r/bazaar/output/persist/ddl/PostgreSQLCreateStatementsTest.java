package com.r.bazaar.output.persist.ddl;

import static org.junit.jupiter.api.Assertions.*;

import com.r.bazaar.BackupRepo;
import com.r.bazaar.entity.internal.Attribute;
import com.r.bazaar.entity.schema.v1.AuditFile;
import com.r.bazaar.entity.schema.v1.ObjectFactory;
import com.r.bazaar.output.persist.PostgreSQLDAO;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class PostgreSQLCreateStatementsTest {

    @Test
    void split() {
        PostgreSQLCreateStatements create = new PostgreSQLCreateStatements(
            BackupRepo.getIdFields());
        System.out.println(create.stripType("java.math.BigDecimal"));
    }

    @Test
    void ddlNoDuplicates() {
        PostgreSQLCreateStatements createStatement = new PostgreSQLCreateStatements(
            BackupRepo.getIdFields());
        Map<String, ArrayList<Attribute>> tablesToSchema = new HashMap<>();
        PostgreSQLCreateStatements.Result result = createStatement.run(AuditFile.class,
            tablesToSchema);
        for (var sepStat : result.getSeparateStatements()) {
            System.out.println("Iterating through: " + sepStat);
            String[] cols = sepStat.split("\\(")[1].split(",");
            ArrayList<String> checkList = new ArrayList<>();
            for (int i = 0; i < cols.length; i++) {
                String colName = cols[i].split(" ")[0];
                if (checkList.contains(colName.toLowerCase())) {
                    System.out.println("Failed colName=" + colName);
                    fail();
                }
                checkList.add(colName.toLowerCase());
            }
        }
    }

    @Test
    void ddlFinalStatementCorrect() {
        PostgreSQLCreateStatements createStatement = new PostgreSQLCreateStatements(
            BackupRepo.getIdFields());
        Map<String, ArrayList<Attribute>> tablesToSchema = new HashMap<>();

        class SuperStruct {

            protected String superField;
        }

        class SomeAdjacentStruct {

            protected String adjacency;
        }

        class Struct1 extends SuperStruct {

            protected String field1;
            protected BigDecimal field2;
            protected List<Struct2> structs;
            protected Struct2 aStruct;

            static class Struct2 {

                protected String field3;
                protected BigDecimal field4;
                protected SomeAdjacentStruct nabor;
            }
        }

        PostgreSQLCreateStatements.Result result = createStatement.run(Struct1.class,
            tablesToSchema);
        String[] splitPackage = this.getClass().getPackageName().split("\\.");
        String expected = "CREATE TABLE saf_t_" + splitPackage[splitPackage.length - 1]
            + ".SomeAdjacentStruct (customer_id varchar,backup_id varchar,company_code varchar,Struct2_id varchar,adjacency varchar);"
            + "CREATE TABLE saf_t_" + splitPackage[splitPackage.length - 1]
            + ".Struct1 (aStruct varchar,field1 varchar,field2 numeric,superField varchar);"
            + "CREATE TABLE saf_t_" + splitPackage[splitPackage.length - 1]
            + ".Struct2 (customer_id varchar,backup_id varchar,company_code varchar,Struct1_id varchar,field3 varchar,field4 numeric);";
        assertEquals(expected, result.getFullStatement());
    }

    @Disabled
    @Test
    void createTables() {
        PostgreSQLCreateStatements createStatements = new PostgreSQLCreateStatements(
            BackupRepo.getIdFields());
        Map<String, ArrayList<Attribute>> tablesToSchema = new HashMap<>();
        PostgreSQLCreateStatements.Result result = createStatements.run(AuditFile.class,
            tablesToSchema);
        PostgreSQLDAO postgreSQLDAO = new PostgreSQLDAO();
        createStatements.executeStatements(postgreSQLDAO.getConnectionString(),
            "CREATE SCHEMA IF NOT EXISTS " + result.getSchema() + ";" + result.getFullStatement());
    }

    @Disabled
    @Test
    void deleteTables() {
        PostgreSQLCreateStatements createStatements = new PostgreSQLCreateStatements(
            BackupRepo.getIdFields());
        Map<String, ArrayList<Attribute>> tablesToSchema = new HashMap<>();
        PostgreSQLCreateStatements.Result result = createStatements.run(AuditFile.class,
            tablesToSchema);
        PostgreSQLDAO postgreSQLDAO = new PostgreSQLDAO();
        String dangerousStatement =
//            "DROP SCHEMA IF EXISTS " + result.getSchema() + " CASCADE;" + //FIXME only comment this in if you want to lose all data
            "";
        createStatements.executeStatements(postgreSQLDAO.getConnectionString(), dangerousStatement);
    }

    @Test
    void findsTypeTest() {
        PostgreSQLCreateStatements create = new PostgreSQLCreateStatements(
            BackupRepo.getIdFields());
        assertAll(
            () -> assertTrue(create.isCustomType("debitAmount", "AmountStructure"),
                "AmountStructure"),
            () -> assertFalse(create.isCustomType("description", "String"), "String"),
            () -> assertTrue(
                create.isCustomType("contactHeaderStructure", "ContactHeaderStructure"),
                "ContactHeaderStructure"),
            () -> assertTrue(create.isCustomType("uomTable", "UOMTable"), "UOMTable"));
    }

    @Test
    void derivesSchemaPackageNameTest() {
        PostgreSQLCreateStatements create = new PostgreSQLCreateStatements(
            BackupRepo.getIdFields());
        assertAll(() -> assertEquals("com.r.bazaar.entity.schema.v1", create.derivePackageName(
                "com.r.bazaar.entity.schema.v1.AuditFile$GeneralLedgerEntries$Journal$Transaction$Line"),
            "package name"));
    }

}