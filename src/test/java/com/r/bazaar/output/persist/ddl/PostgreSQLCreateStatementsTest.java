package com.r.bazaar.output.persist.ddl;

import static org.junit.jupiter.api.Assertions.*;

import com.r.bazaar.BackupRepo;
import com.r.bazaar.entity.internal.Attribute;
import com.r.bazaar.entity.schema.v1.AuditFile;
import com.r.bazaar.entity.schema.v1.ObjectFactory;
import com.r.bazaar.output.persist.PostgreSQLDAO;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PostgreSQLCreateStatementsTest {

    @Test
    void run() {
//        AutowireHelper autowireHelper = new AutowireHelper();
        PostgreSQLCreateStatements create = new PostgreSQLCreateStatements(BackupRepo.getIdFields());
//        create.run2();
        ObjectFactory objectFactory = new ObjectFactory();
//        Object line = objectFactory.createAuditFileGeneralLedgerEntriesJournalTransactionLine();
        Object line = objectFactory.createAuditFileGeneralLedgerEntries();
        List<Field> declaredFields = Arrays.stream(line.getClass().getDeclaredFields()).toList();
        List<String> fieldNames = create.getFieldNames(declaredFields);
        List<String> fieldTypeNames = create.getFieldTypes(declaredFields);
        System.out.println(fieldNames);
        System.out.println(fieldTypeNames);
    }

    @Test
    void split() {
        PostgreSQLCreateStatements create = new PostgreSQLCreateStatements(BackupRepo.getIdFields());
        System.out.println(create.stripType("java.math.BigDecimal"));
    }

    @Test
    void ddl() {
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
    void createTables() {
        PostgreSQLCreateStatements createStatements = new PostgreSQLCreateStatements(
            BackupRepo.getIdFields());
        Map<String, ArrayList<Attribute>> tablesToSchema = new HashMap<>();
        PostgreSQLCreateStatements.Result result = createStatements.run(AuditFile.class,
            tablesToSchema);
        PostgreSQLDAO postgreSQLDAO = new PostgreSQLDAO();
        createStatements.executeStatements(
            postgreSQLDAO.getConnectionString(),
            "CREATE SCHEMA IF NOT EXISTS " + result.getSchema() + ";" + result.getFullStatement());
    }

    @Test
    void deleteTables() {
        PostgreSQLCreateStatements createStatements = new PostgreSQLCreateStatements(
            BackupRepo.getIdFields());
        Map<String, ArrayList<Attribute>> tablesToSchema = new HashMap<>();
        PostgreSQLCreateStatements.Result result = createStatements.run(AuditFile.class,
            tablesToSchema);
        PostgreSQLDAO postgreSQLDAO = new PostgreSQLDAO();
        createStatements.executeStatements(
            postgreSQLDAO.getConnectionString(),
            "DROP SCHEMA IF EXISTS " + result.getSchema() + " CASCADE;");
    }

    @Test
    void findsTypeTest() {
        PostgreSQLCreateStatements create = new PostgreSQLCreateStatements(BackupRepo.getIdFields());
        assertAll(
            () -> assertTrue(create.isType("debitAmount", "AmountStructure"), "AmountStructure"),
            () -> assertFalse(create.isType("description", "String"), "String"),
            () -> assertTrue(create.isType("contactHeaderStructure", "ContactHeaderStructure"),
                "ContactHeaderStructure"),
            () -> assertTrue(create.isType("uomTable", "UOMTable"), "UOMTable")
        );
    }

    @Test
    void derivesSchemaPackageNameTest() {
        PostgreSQLCreateStatements create = new PostgreSQLCreateStatements(BackupRepo.getIdFields());
        assertAll(
            () -> assertEquals("com.r.bazaar.entity.schema.v1",
                create.derivePackageName(
                    "com.r.bazaar.entity.schema.v1.AuditFile$GeneralLedgerEntries$Journal$Transaction$Line"),
                "package name")
        );
    }

}