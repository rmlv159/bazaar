package com.r.bazaar.output.report;

import com.r.bazaar.entity.schema.v1.AuditFile;
import com.r.bazaar.entity.schema.v1.AuditFile.GeneralLedgerEntries;
import com.r.bazaar.entity.schema.v1.AuditFile.GeneralLedgerEntries.Journal;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Packager {
//TODO where to map sourceDocumentID   customerID   supplierID from?
    @Autowired
    private XMLWriter xmlWriter = new XMLWriter();

    public void run(ArrayList<String> data) {
        Journal journal = new Journal();
        GeneralLedgerEntries generalLedgerEntries = new GeneralLedgerEntries();
        AuditFile auditFile = new AuditFile();
        auditFile.setGeneralLedgerEntries(generalLedgerEntries);
        xmlWriter.run(auditFile);
    }
}
