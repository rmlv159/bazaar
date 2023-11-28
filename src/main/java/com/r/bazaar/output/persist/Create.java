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
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                }
            }
        }
        return fieldNames;
    }

    public void createDDL(String parentName, Class<?> structure,
        Map<String, ArrayList<Attribute>> tablesToSchema) {
        String simpleName = structure.getSimpleName();

        Field[] declaredFields = structure.getDeclaredFields();
        ArrayList<String> fieldNames = getFieldNames(declaredFields);
        ArrayList<String> fieldTypeNames = getFieldTypes(declaredFields);
        ArrayList<Class<?>> genericClasses = getGenericClasses(declaredFields);

        Map<String, Attribute> classAttributes = getClassAttributes(
            fieldNames, fieldTypeNames, genericClasses);
        breakDownFields(parentName, structure, tablesToSchema, classAttributes, simpleName);
    }

    private void breakDownFields(String parentName, Class<?> structure,
        Map<String, ArrayList<Attribute>> tablesToSchema,
        Map<String, Attribute> classAttributes,
        String simpleName) {

        if (null != parentName) {
            storeAttribute(tablesToSchema, simpleName, parentName + "Id", "Integer");
        }

        Field[] declaredFields;
        ArrayList<String> fieldNames;
        ArrayList<Class<?>> genericClasses;
        ArrayList<String> fieldTypeNames;

        for (var entry : classAttributes.entrySet()) {
            String[] nameArr = entry.getKey().split("\\$");
            String name = nameArr[nameArr.length - 1];
            String[] typeArr = entry.getValue().getTypeName().split("\\$");
            String type = stripType(typeArr[typeArr.length - 1]);
//            String innerClassName = StringUtils.capitalize(name);
            if (StringUtils.capitalize(name).equals(type)) {
                List<Class<?>> classes = List.of(structure.getClasses());
                for (var c : classes) {
                    createDDL(simpleName, c, tablesToSchema);
                }
            } else if (auditFileCollections.contains(type)) {
                if (null == entry.getValue().getGenericClass()) {
                    continue;
                }
                parentName = simpleName;
                structure = entry.getValue().getGenericClass();
                List<Class<?>> classes = List.of(structure.getClasses());
                if (!classes.isEmpty()) {
                    for (var c : classes) {
                        createDDL(simpleName, c, tablesToSchema);
                    }
                }
                declaredFields = structure.getDeclaredFields();
                fieldNames = getFieldNames(declaredFields);
                fieldTypeNames = getFieldTypes(declaredFields);
                genericClasses = getGenericClasses(declaredFields);
                classAttributes = getClassAttributes(
                    fieldNames, fieldTypeNames, genericClasses);
                simpleName = structure.getSimpleName();
                breakDownFields(parentName, structure, tablesToSchema, classAttributes, simpleName);
            }
            if (!type.equals("List")) {
                storeAttribute(tablesToSchema, simpleName, name, type);
            }
        }
    }

    private static void storeAttribute(Map<String, ArrayList<Attribute>> tablesToSchema,
        String simpleName, String name, String type) {
        Attribute attribute = new Attribute(name, type, null);
        ArrayList<Attribute> dbAttributes = tablesToSchema.getOrDefault(
            simpleName, new ArrayList<>());
        if (!dbAttributes.contains(attribute)) {
            dbAttributes.add(attribute);
        }
        tablesToSchema.put(simpleName, dbAttributes);
    }

    private static Map<String, Attribute> getClassAttributes(ArrayList<String> fieldNames,
        ArrayList<String> fieldTypeNames, ArrayList<Class<?>> genericClasses) {
        Map<String, Attribute> classAttributes = new HashMap<>();
        for (int i = 0; i < fieldNames.size(); i++) {
            classAttributes.put(fieldNames.get(i),
                new Attribute(fieldNames.get(i), fieldTypeNames.get(i), genericClasses.get(i)));
        }
        return classAttributes;
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
            stmt += ");";
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
