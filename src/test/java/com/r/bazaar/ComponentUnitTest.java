package com.r.bazaar;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.collect.Table;
import com.r.bazaar.input.Decoder;
import com.r.bazaar.input.InputValidator;
import com.r.bazaar.output.persist.DAO;
import com.r.bazaar.output.persist.SystemSpecifics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ComponentUnitTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void givenInScopeComponents_whenSearchingInApplicationContext_thenFindThem() {
        assertNotNull(applicationContext.getBean(BackupRepo.class));
        assertNotNull(applicationContext.getBean(SystemSpecifics.class));
        assertNotNull(applicationContext.getBean(MatchingAlgo.class));
        assertNotNull(applicationContext.getBean(MatchingRepo.class));
        assertNotNull(applicationContext.getBean(Decoder.class));
        assertNotNull(applicationContext.getBean(InputValidator.class));
        assertNotNull(applicationContext.getBean("dao", DAO.class));
        assertNotNull(applicationContext.getBean("clientSystemMapping", Table.class));
    }
}
