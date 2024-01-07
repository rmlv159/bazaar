package com.r.bazaar.output.persist;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostgreSQLDAOTest {

//    @Autowired
    private PostgreSQLDAO postgreSQLDAO;

    @BeforeEach
    void setUp() {
        //TODO set up an in-memory database
        //TODO RK not a real test, remove at some point
        postgreSQLDAO = new PostgreSQLDAO();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getTablesPerCustomer() {
        HashMap<Integer, Set<String>> expected = new HashMap<>() {
            {
                put(1, Set.of("G_L Entry"));
            }
        };
        assertEquals(expected, postgreSQLDAO.getTablesPerClient());
    }

    @Test
    void getCompanyCode() {
        assertAll(
            () -> assertEquals("CRONUS Danmark A_S", postgreSQLDAO.getCompanyCode(1),
                "Company code extracted correct")
        );
    }

    @Test
    void invokeGetTablesPerClient() {
        postgreSQLDAO.getTablesPerClient();
    }
}