package com.r.bazaar.output.persist;

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
import com.r.bazaar.entity.internal.Attribute;
import com.r.bazaar.entity.schema.v1.AuditFile;
import jakarta.xml.bind.JAXBElement;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.util.StringUtils;

public class Create {

    private static final List<String> auditFileCollections = List.of("List", "ArrayList");

    public void run() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder
            .standard()
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "localhost"))
            .build();
        DynamoDB docClient = new DynamoDB(client);

//        YourPojo yourPojoInstance = new YourPojo(); // Create an instance of your POJO
//
//        // Convert your POJO to a map of attribute values
//        Map<String, AttributeValue> itemValues = YourPojoMapper.toMap(yourPojoInstance);
//
//        // Specify the table name
//        String tableName = "YourTableName";
//
//        // Build the PutItemRequest
//        PutItemRequest putItemRequest = PutItemRequest.builder()
//            .tableName(tableName)
//            .item(itemValues)
//            .build();
//
//        // Put the item into DynamoDB
//        docClient.putItem(putItemRequest);
//
//        // Close the DynamoDB client
//        dynamoDbClient.close();

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

    public ArrayList<String> getFieldNames(Field[] fields) {
        ArrayList<String> fieldNames = new ArrayList<>();
        for (Field field : fields) {
            fieldNames.add(field.getName());
        }
        return fieldNames;
    }

    public ArrayList<String> getFieldTypes(Field[] fields) {
        ArrayList<String> fieldNames = new ArrayList<>();
        for (Field field : fields) {
            fieldNames.add(field.getType().getName());
        }
        return fieldNames;
    }

    public ArrayList<Class<?>> getGenericClasses(Field[] fields) {
        ArrayList<Class<?>> fieldNames = new ArrayList<>();
        for (Field field : fields) {
            Type type = field.getGenericType();
            if (type instanceof Class<?>) {
                fieldNames.add(null);
            } else if (type instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
//                    if (actualTypeArguments[0] == JAXBElement.class) {
//                    if (actualTypeArguments[0] instanceof ParameterizedTypeImpl) {
//                        continue;
//                    }
                    try {
                        Class<?> rawType = (Class<?>) actualTypeArguments[0]; //TODO only work for one and one-dimensional generic classes
                        fieldNames.add(rawType);
                    } catch (ClassCastException e) {
                        //This should only happen when actualTypeArguments[0] == JAXBElement.class only present in schema.v1 at AuditFile$MasterFiles$PhysicalStock$PhysicalStockEntry$StockCharacteristics.stockCharacteristicAndStockCharacteristicValue
                        fieldNames.add(null);
                        continue;
                    }
            }
        }
        return fieldNames;
    }

    public void createDDL(Class<?> structure, Map<String, ArrayList<Attribute>> tablesToSchema) {
        String simpleName = structure.getSimpleName();

        Field[] declaredFields = structure.getDeclaredFields();
        final ArrayList<String> fieldNames = getFieldNames(declaredFields);
        final ArrayList<String> fieldTypeNames = getFieldTypes(declaredFields);
        final ArrayList<Class<?>> genericClasses = getGenericClasses(declaredFields);

        Map<String, Attribute> classAttributes = new HashMap<>();
        for (int i = 0; i < fieldNames.size(); i++) {
            classAttributes.put(fieldNames.get(i),
                new Attribute(fieldNames.get(i), fieldTypeNames.get(i), genericClasses.get(i)));
        }
        for (var entry : classAttributes.entrySet()) {
            String[] nameArr = entry.getKey().split("\\$");
            String name = nameArr[nameArr.length - 1];
            String[] typeArr = entry.getValue().getTypeName().split("\\$");
            String type = stripType(typeArr[typeArr.length - 1]);
//            String innerClassName = StringUtils.capitalize(name);
            if (StringUtils.capitalize(name).equals(type)) {
                List<Class<?>> classes = List.of(structure.getClasses());
                for (var c : classes) {
                    createDDL(c, tablesToSchema);
                }
            } else if (auditFileCollections.contains(type)) {
                if (null == entry.getValue().getGenericClass()) {
                    continue;
                }
                structure = entry.getValue().getGenericClass();
                List<Class<?>> classes = List.of(structure.getClasses());
                for (var c : classes) {
                    createDDL(c, tablesToSchema);
                }
            }
            Attribute attribute = new Attribute(name, type, null);
            ArrayList<Attribute> dbAttributes = tablesToSchema.getOrDefault(
                simpleName, new ArrayList<>());
            dbAttributes.add(attribute);
            tablesToSchema.put(simpleName, dbAttributes);
        }
    }

    public List<String> createStatement(Map<String, ArrayList<Attribute>> tableToSchema) {
        List<String> createStatementList = new ArrayList<>();
        for (var entry : tableToSchema.entrySet()) {
            String tableName = entry.getKey();
            StringBuilder createStatement = new StringBuilder(
                "CREATE TABLE erp." + tableName + " (");
            for (var attribute : entry.getValue()) {
                createStatement
                    .append(attribute.getName())
                    .append(" ")
                    .append(convertToSql(attribute.getTypeName()))
                    .append(",");
            }
            String stmt = createStatement.substring(0, createStatement.length() - 1);
            createStatementList.add(stmt);
        }
        return createStatementList;
    }

    private String convertToSql(String type) {
        if ("BigDecimal".equals(type)) {
            return "numeric";
        } else if ("int".equals(type)) {
            return "int";
        } else if ("XMLGregorianCalendar".equals(type)) {
            return "date";
        }
        return "varchar";
    }

    public String stripType(String type) {
        String[] split = type.split("\\.");
        return split[split.length - 1];
    }
}
