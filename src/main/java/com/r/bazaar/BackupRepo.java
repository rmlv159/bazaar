package com.r.bazaar;

import com.r.bazaar.lib.model.Backup;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BackupRepo {

    public List<Backup> getWeekly() {
        return List.of(
            new Backup(LocalDateTime.of(2023, 10, 20, 17, 0)),
            new Backup(LocalDateTime.of(2023, 10, 13, 17, 0)),
            new Backup(LocalDateTime.of(2023, 10, 6, 17, 0)),
            new Backup(LocalDateTime.of(2023, 10, 29, 17, 0)),
            new Backup(LocalDateTime.of(2023, 9, 22, 17, 0)),
            new Backup(LocalDateTime.of(2023, 9, 15, 17, 0))
    );
    }
}
