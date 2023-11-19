package com.r.bazaar.output.persist;

import static org.junit.jupiter.api.Assertions.*;

import com.r.bazaar.entity.internal.Attribute;
import com.r.bazaar.entity.schema.v1.AuditFile;
import com.r.bazaar.entity.schema.v1.ObjectFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CreateTest {

    @Test
    void run() {
//        AutowireHelper autowireHelper = new AutowireHelper();
        Create create = new Create();
//        create.run2();
        ObjectFactory objectFactory = new ObjectFactory();
//        Object line = objectFactory.createAuditFileGeneralLedgerEntriesJournalTransactionLine();
        Object line = objectFactory.createAuditFileGeneralLedgerEntries();
        Field[] declaredFields = line.getClass().getDeclaredFields();
        List<String> fieldNames = create.getFieldNames(declaredFields);
        List<String> fieldTypeNames = create.getFieldTypes(declaredFields);
        System.out.println(fieldNames);
        System.out.println(fieldTypeNames);
    }

    @Test
    void split() {
        Create create = new Create();
        System.out.println(create.stripType("java.math.BigDecimal"));
    }

    @Test
    void ddl() {
        Create create = new Create();
        Map<String, ArrayList<Attribute>> tablesToSchema = new HashMap<>();
        create.createDDL(AuditFile.class, tablesToSchema);
        List<String> statement = create.createStatement(tablesToSchema);
        List<String> expected = List.of();
        assertEquals(expected, statement);
    }
}