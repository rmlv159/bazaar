package com.r.bazaar;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.r.bazaar.lib.model.Backup;
import com.r.bazaar.lib.model.IntelliSyncConfig;
import com.r.bazaar.model.Field;
import com.r.bazaar.model.Table;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("BazaarAPIControllerV1")
public class BazaarAPI {

    @Autowired
    private BackupRepo repo;
    @Autowired
    private InputValidator inputValidator;
    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/v1/weekly-backups")
    public List<Backup> getWeekly() {
        return repo.getWeekly();
    }

    @GetMapping("/v1/fetch")
    @ResponseBody
    public IntelliSyncConfig fetch() {
        IntelliSyncConfig intelliSyncConfig = new IntelliSyncConfig();
        intelliSyncConfig.getRelevantTables().add("[dbo].[CRONUS Danmark A_S$Customer]");
        return intelliSyncConfig;
    }

    @PostMapping(path = "/v1/initialize/{customer}", produces = "application/json")
    @ResponseBody
    public IntelliSyncConfig postInitialization(
        //plus client id and secret for verification
        @PathVariable("customer")
        @Parameter(description = "Pre-registered and pre-configured customer id", required = true, example = "81348123")
            String customer) {

        IntelliSyncConfig intelliSyncConfig = new IntelliSyncConfig();

        intelliSyncConfig.getRelevantTables().add("CRONUS Danmark A_S$Customer");
        intelliSyncConfig.getSubstituteFields2CustomizedFields().put("[Has Error]", "[Has Error2]");

        return intelliSyncConfig;
    }

    @PostMapping(path = "/v1/schema")
    public ResponseEntity<String> receiveTableSchema(
        //plus client id and secret for verification
//        @PathVariable("customer")
//        @Parameter(description = "Pre-registered and pre-configured customer id", required = true, example = "81348123")
//            String customer,
        @RequestBody
            Table table
    ) {
        try {
            inputValidator.runForSequencedTypes(table.getSequencedTypes());
            //TODO use schema
            repo.cacheSchema(table.getName(), table.getSequencedTypes());
            return ResponseEntity.ok("Data received successfully");
            //TODO handle more specifically
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Expected gzipped content");
        }
    }

    @PostMapping(path = "/v1/row")
    public ResponseEntity<String> receiveZippedEncodedRowData(
        HttpServletRequest request,
        @RequestHeader("Content-Encoding") String contentEncoding
        ,
        //plus client id and secret for verification
//        @PathVariable("customer")
//        @Parameter(description = "Pre-registered and pre-configured customer id", required = true, example = "81348123")
//            String customer,
        @RequestParam("table-name")
        @Parameter(description = "", required = true, example = "CRONUS Danmark A_S$Customer")
            String tableName
    ) throws IOException {
        if (contentEncoding != null && contentEncoding.equals("gzip")) {
            GZIPInputStream gzipInputStream = new GZIPInputStream(request.getInputStream());
            String decompressed = StreamUtils.copyToString(gzipInputStream, StandardCharsets.UTF_8);

            ArrayList<String> data = objectMapper.readValue(decompressed,
                new TypeReference<>() {
                });
            try {
                repo.saveRow(tableName, data);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Expected gzipped content");
            }
            return ResponseEntity.ok("Data received and decompressed successfully");
        }
        return ResponseEntity.badRequest().body("Expected gzipped content");
    }
}