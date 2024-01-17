package com.r.bazaar.output.persist.ddl;

import static org.junit.jupiter.api.Assertions.*;

import com.r.bazaar.BackupRepo;
import com.r.bazaar.entity.internal.Attribute;
import com.r.bazaar.entity.schema.v1.AuditFile;
import com.r.bazaar.output.persist.PostgreSQLDAO;
import com.r.bazaar.util.AutowireHelper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class PostgreSQLCreateStatementsTest {

    private static final Logger logger = LogManager.getLogger();

    @Test
    void split() {
        PostgreSQLCreateStatements creator = new PostgreSQLCreateStatements(
            BackupRepo.getIdFields(), new AutowireHelper().createChoiceTypes());
        System.out.println(creator.stripType("java.math.BigDecimal"));
    }

    @Test
    void ddlNoDuplicates() throws Exception {
        PostgreSQLCreateStatements createStatement = new PostgreSQLCreateStatements(
            BackupRepo.getIdFields(), new AutowireHelper().createChoiceTypes());
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
                    fail("Failed colName=" + colName);
                }
                checkList.add(colName.toLowerCase());
            }
        }
    }

    @Test
    void ddlFinalStatementCorrect() throws Exception {
        PostgreSQLCreateStatements createStatement = new PostgreSQLCreateStatements(
            BackupRepo.getIdFields(), new AutowireHelper().createChoiceTypes());
        Map<String, ArrayList<Attribute>> tablesToSchema = new HashMap<>();

        class SuperStruct {

            protected String superField;
        }

        class SomeAdjacentStruct {

            protected String adjacencyData;
        }

        class Struct1 extends SuperStruct {

            protected String field1;
            protected BigDecimal field2;
            protected List<Struct2> structs;
            protected Struct3 aStruct;

            static class Struct2 {

                protected String field3;
                protected BigDecimal field4;
                protected SomeAdjacentStruct nabour;
            }

            static class Struct3 {

                protected String field5;
            }
        }

        PostgreSQLCreateStatements.Result result = createStatement.run(Struct1.class,
            tablesToSchema);
        String[] splitPackage = this.getClass().getPackageName().split("\\.");
        String expected = "CREATE TABLE saf_t_" + splitPackage[splitPackage.length - 1]
            + ".SomeAdjacentStruct (id int,meta_customer_id varchar,meta_backup_id varchar,meta_company_code varchar,adjacencyData varchar);"
            + "CREATE TABLE saf_t_" + splitPackage[splitPackage.length - 1]
            + ".Struct1 (meta_customer_id varchar,meta_backup_id varchar,meta_company_code varchar,aStruct_Struct3_1to1_id int,field1 varchar,field2 decimal,superField varchar);"
            + "CREATE TABLE saf_t_" + splitPackage[splitPackage.length - 1]
            + ".Struct2 (meta_customer_id varchar,meta_backup_id varchar,meta_company_code varchar,Struct1_id varchar,nabour_SomeAdjacentStruct_1to1_id int,field3 varchar,field4 decimal);"
            + "CREATE TABLE saf_t_" + splitPackage[splitPackage.length - 1]
            + ".Struct3 (id int,meta_customer_id varchar,meta_backup_id varchar,meta_company_code varchar,field5 varchar);";
        assertEquals(expected, result.getFullStatement());
    }

//    @Disabled
    @Test
        //TODO RK all of this code should maybe be moved into a separate application?
    void createTables() throws Exception {
        PostgreSQLCreateStatements createStatements = new PostgreSQLCreateStatements(
            BackupRepo.getIdFields(), new AutowireHelper().createChoiceTypes());
        Map<String, ArrayList<Attribute>> tablesToSchema = new HashMap<>();
        PostgreSQLCreateStatements.Result result = createStatements.run(AuditFile.class,
            tablesToSchema);
        PostgreSQLDAO postgreSQLDAO = new PostgreSQLDAO();
        createStatements.executeStatements(postgreSQLDAO.getConnectionString(),
            "CREATE SCHEMA IF NOT EXISTS " + result.getSchema() + ";" + result.getFullStatement());
    }

    @Disabled
    @Test
        //TODO RK all of this code should maybe be moved into a separate application?
    void deleteTables() throws Exception {
        PostgreSQLCreateStatements createStatements = new PostgreSQLCreateStatements(
            BackupRepo.getIdFields(), new AutowireHelper().createChoiceTypes());
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
        assertAll(
            () -> assertTrue(PostgreSQLCreateStatements.isCustomType("AmountStructure"),
                "AmountStructure"),
            () -> assertFalse(PostgreSQLCreateStatements.isCustomType("String"), "String"),
            () -> assertTrue(
                PostgreSQLCreateStatements.isCustomType("ContactHeaderStructure"),
                "ContactHeaderStructure"),
            () -> assertTrue(PostgreSQLCreateStatements.isCustomType("UOMTable"), "UOMTable"));
    }

    @Test
    void derivesSchemaPackageNameTest() {
        PostgreSQLCreateStatements create = new PostgreSQLCreateStatements(
            BackupRepo.getIdFields(), new AutowireHelper().createChoiceTypes());
        assertAll(() -> assertEquals("com.r.bazaar.entity.schema.v1", create.derivePackageName(
                "com.r.bazaar.entity.schema.v1.AuditFile$GeneralLedgerEntries$Journal$Transaction$Line"),
            "package name"));
    }

    @Test
    void getAuxRelations() throws Exception {
        PostgreSQLCreateStatements create = new PostgreSQLCreateStatements(
            BackupRepo.getIdFields(), new AutowireHelper().createChoiceTypes());
        logger.info(create.getAuxRelations("schema_for_testing"));
    }
}