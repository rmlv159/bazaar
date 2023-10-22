package com.r.bazaar;

import com.r.bazaar.model.Backup;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("BazaarAPIControllerV1")
public class BazaarAPI {

    @Autowired
    private BackupRepo repo;

    @GetMapping("/v1/weekly-backups")
    public List<Backup> getWeekly() {
        return repo.getWeekly();
    }
}
