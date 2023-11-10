package com.r.bazaar;

import static org.junit.jupiter.api.Assertions.*;

import com.r.bazaar.lib.model.IntelliSyncConfig;
import com.r.bazaar.output.persist.PostgreSQLDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

class BackupRepoTest {

    private BackupRepo backupRepo;

    @BeforeEach
    void setUp() {
        backupRepo = new BackupRepo(new PostgreSQLDAO());
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getIntelliSyncConfig() {
        IntelliSyncConfig expected = new IntelliSyncConfig();
        expected.getRelevantTables().add("[dbo].[CRONUS Danmark A_S$G_L Entry]");
        assertAll(
            () -> assertEquals(expected.getRelevantTables(), backupRepo.getIntelliSyncConfig(1).getRelevantTables())
        );
    }
}