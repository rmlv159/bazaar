package com.r.bazaar.output.persist;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.r.bazaar.output.persist.PostgreSQLDAO.Row;
import com.typesafe.config.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;

public class DynamoDBDAO implements DAO {

    public Config config;

    public final AmazonDynamoDB client;
    public final DynamoDBMapper mapper;

    public DynamoDBDAO(
        @Qualifier("conf") //TODO make nicer
            Config config) {
        this.config = config;
        //TODO make this work
//        String hostStr = "environment."
//            + AppConfig.thisEnvironment()
//            + ".ddb-host";
//        String portStr = "environment."
//            + AppConfig.thisEnvironment()
//            + ".ddb-port";
//        client = AmazonDynamoDBClientBuilder
//            .standard()
//            .withEndpointConfiguration(
//                new AwsClientBuilder.EndpointConfiguration(
//                    config.getString(hostStr) + ":" + config.getString(portStr),
//                    "localhost")) //TODO Put in conf
//            .build();
                client = AmazonDynamoDBClientBuilder
            .standard()
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "localhost"))
            .build();
        mapper = new DynamoDBMapper(client);
    }

    public void get() {
//        SkiLift partitionKey = new SkiLift();
//
//        partitionKey.setLift(10);
//        DynamoDBQueryExpression<SkiLift> queryExpression = new DynamoDBQueryExpression<SkiLift>()
//            .withHashKeyValues(partitionKey);
//
//        List<SkiLift> itemList = mapper.query(SkiLift.class, queryExpression);
//
//        for (int i = 0; i < itemList.size(); i++) {
//            System.out.println(itemList.get(i).getStatus());
//        }

        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable("SkiLifts");
        GetItemSpec  spec = new GetItemSpec()
            .withPrimaryKey("Lift", "Lift 3", "Metadata", "01/01/20")
//        .withPrimaryKey(new PrimaryKey("Lift", "Lift 3"));
            .withProjectionExpression("TotalUniqueLiftRiders, AverageSnowCoverageInches, LiftStatus, AvalancheDanger");
        Item item = table.getItem(spec);
        System.out.println(item.toJSONPretty());
    }

    @Override
    public Map<Integer, Set<String>> getTablesPerClient() {
        //TODO implement
        return null;
    }

    @Override
    public String getCompanyCode(int clientId) {
        //TODO implement
        return null;
    }

    @Override
    public void put(String tableName, List<String> fieldNames, List<String> data) {
        //TODO implement
    }

    @Override
    public String getSystemName(Integer clientId) {
        //TODO implement
        return null;
    }

    @Override
    public Map<String, String> getGenericTablesToGenericFields(int clientId, String systemName,
        List<String> tableNames) {
        //TODO impl
        return null;
    }

    @Override
    public Map<String, Row> getSystemDefaultToGeneric(int clientId, String systemName,
        List<String> tableNames) {
        //TODO impl
        return null;
    }

    //TODO record Row externalized?
    @Override
    public Map<String, String> getGenericFieldsToSystemDefaultFields(int clientId,
        String systemName, List<String> tableNames) {
        //TODO impl
        return null;
    }

    @Override
    public List<String> getGenericTableNames(String defaultSystemTableName) {
        //TODO impl
        return null;
    }
}
