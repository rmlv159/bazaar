package com.r.bazaar.output.report;

import com.r.bazaar.entity.schema.v1.AuditFile;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.File;
import org.springframework.stereotype.Component;

@Component
public class XMLWriter  {

    public void run(AuditFile data) {
        try {
            JAXBContext context = JAXBContext.newInstance(AuditFile.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            File outputFile = new File("myData.xml");
            marshaller.marshal(data, outputFile);
            System.out.println("XML file saved to: " + outputFile.getAbsolutePath());
        } catch (jakarta.xml.bind.JAXBException e) {
            e.printStackTrace();
        }
    }
}
