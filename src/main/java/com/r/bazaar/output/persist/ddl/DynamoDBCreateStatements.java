package com.r.bazaar.output.persist.ddl;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.r.bazaar.entity.schema.v1.AuditFile;
import java.util.Arrays;

public class DynamoDBCreateStatements {

    public void run() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder
            .standard()
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "localhost"))
            .build();
        DynamoDB docClient = new DynamoDB(client);

        DynamoDB dynamoDB = new DynamoDB(client);
        String tableName = "AuditFile";
        try {
            System.out.println("Creating the table, wait...");
            Table table = dynamoDB.createTable(tableName,
                Arrays.asList(
                    new KeySchemaElement("ID", KeyType.HASH), // the partition key
                    // the sort key
                    new KeySchemaElement("Nomenclature", KeyType.RANGE)
                ),
                Arrays.asList(
                    new AttributeDefinition("ID", ScalarAttributeType.N),
                    new AttributeDefinition("Nomenclature", ScalarAttributeType.S)
                ),
                new ProvisionedThroughput(10L, 10L)
            );
            table.waitForActive();
            System.out.println("Table created successfully.  Status: " +
                table.getDescription().getTableStatus());

        } catch (Exception e) {
            System.err.println("Cannot create the table: ");
            System.err.println(e.getMessage());
        }
    }

    public void run2() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder
            .standard()
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "localhost"))
            .build();
        DynamoDB dynamoDB = new DynamoDB(client);
        DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder()
            .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.CLOBBER)
            .build();
        DynamoDBMapper mapper = new DynamoDBMapper(client, mapperConfig);

        try {
            System.out.println("Creating the table, wait...");
            CreateTableRequest createTableRequest = mapper.generateCreateTableRequest(
                    AuditFile.class)
                .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L));
            Table table = dynamoDB.createTable(createTableRequest);
            table.waitForActive();
            System.out.println("Table created successfully.  Status: " +
                table.getDescription().getTableStatus());
        } catch (Exception e) {
            System.err.println("Cannot create the table: ");
            System.err.println(e.getMessage());
        }
    }
}



