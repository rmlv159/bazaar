package com.r.bazaar;

import com.r.bazaar.lib.model.Backup;
import com.r.bazaar.lib.model.IntelliSyncConfig;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("BazaarAPIControllerV1")
public class BazaarAPI {

    @Autowired
    private BackupRepo repo;

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

    @PostMapping(path = "/v1/weekly-backup")
    public ResponseEntity<String> receiveGzippedData(HttpServletRequest request,
        @RequestHeader("Content-Encoding") String contentEncoding)
        throws IOException {

        if (contentEncoding != null && contentEncoding.equals("gzip")) {
            // If the content is gzipped, decompress it
            GZIPInputStream gzipInputStream = new GZIPInputStream(request.getInputStream());
            String decompressed = StreamUtils.copyToString(gzipInputStream, StandardCharsets.UTF_8);
            System.out.println("Received Gzipped Data: " + decompressed);

            // Process the data (just printing in this example)
            return ResponseEntity.ok("Data received and decompressed successfully");
        }
        return ResponseEntity.badRequest().body("Expected gzipped content");
    }
}