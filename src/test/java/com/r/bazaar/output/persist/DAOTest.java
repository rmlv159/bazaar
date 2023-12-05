package com.r.bazaar.output.persist;

import com.r.bazaar.util.AutowireHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

//@SpringBootTest
//@ActiveProfiles("test")
class DAOTest {

    @Autowired
    private DynamoDBDAO dao;

    @BeforeEach
    void setUp() {
        AutowireHelper autowireHelper = new AutowireHelper();
        dao = new DynamoDBDAO(autowireHelper.appConfig());
    }

    @AfterEach
    void tearDown() {
    }

    @Disabled
    @Test
    void get() {
        dao.get();
    }
}