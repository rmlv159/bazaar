package com.r.bazaar;

import static org.junit.jupiter.api.Assertions.*;

import com.r.bazaar.lib.model.IntelliSyncConfig;
import com.r.bazaar.output.persist.DAO;
import com.r.bazaar.output.persist.PostgreSQLDAO;
import com.r.bazaar.output.persist.SystemSpecifics;
import java.util.HashMap;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BackupRepoTest {

    private BackupRepo backupRepo;
    private DAO dao;

    @BeforeEach
    void setUp() {
        dao = EasyMock.niceMock(PostgreSQLDAO.class);
        backupRepo = new BackupRepo(dao, new SystemSpecifics(new PostgreSQLDAO()));
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

    @Test
    void filter_CorrectValues() {
        Map<String, String> exp = new HashMap<>();
        exp.put("aux_relations", "value");
        EasyMock.expect(dao.getGenericTablesToGenericFields(EasyMock.anyInt(), EasyMock.anyString(),
            EasyMock.anyObject())).andReturn(exp);
    }
}