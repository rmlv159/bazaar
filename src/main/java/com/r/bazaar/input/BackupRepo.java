package com.r.bazaar.input;

import com.r.bazaar.output.report.Packager;
import com.r.bazaar.lib.model.Backup;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BackupRepo {

    //TODO implement eviction
    private final Map<String, ArrayList<String>> schemaCache = new HashMap<>();
    @Autowired
    private Decoder decoder;
    //TODO make separate
//            System.out.println(decoder.base64(type, data.get(i)));
    @Autowired
    private Packager packager;

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

    public void cacheSchema(String tableName, ArrayList<String> schema)
        throws IllegalArgumentException {
        if (schemaCache.containsKey(tableName)) {
            //TODO enable again once table cache is evicted after each backup
//            throw new IllegalArgumentException("table exists already");
        }
        schemaCache.put(tableName, schema);
    }

    public void saveRow(String tableName, ArrayList<String> data) {
        ArrayList<String> schema = schemaCache.get(tableName);
        if (null == schema) {
            throw new IllegalArgumentException("Table not present in schema");
        }
        //input validation has already been done
        for (int i = 0; i < data.size(); i++) {
            String type = schema.get(i);
            System.out.println(data.get(i));

            packager.run(data);
        }
    }
}
