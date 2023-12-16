package com.r.bazaar.output.persist.ddl;

import com.r.bazaar.entity.internal.Attribute;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PostgreSQLCreateStatements {

    private static final Logger logger = LogManager.getLogger();
    private static final List<String> AUDIT_FILE_COLLECTIONS = List.of("List", "ArrayList");
    private static final List<String> COMMON_CLASHING_SUB_STRUCTS = List.of("Line", "Settlement",
        "Supplier");
    private static final List<String> JAKARTA_PARSED_TYPICAL_TYPES = List.of("String", "BigDecimal",
        "BigInteger", "Integer", "int", "long", "XMLGregorianCalendar");
    private static final List<String> JAVA_ADDED_FIELD_TYPES = List.of("byte", "boolean",
        "char", "Comparator", "[B", "ObjectStreamField;");
    private static final List<String> PSQL_SENSITIVE_NAMES = List.of("references");
    private final List<String> idFields;
    private String safTSchema;
//    private final Set<String> retainedStructuresForLaterParsing = new HashSet<>();

    static class Result {

        private String fullStatement;
        private Set<String> separateStatements = new HashSet<>();

        private String schema;

        private Result() {
        }

        protected String getFullStatement() {
            return fullStatement;
        }

        protected void setFullStatement(String fullStatement) {
            this.fullStatement = fullStatement;
        }

        protected String getSchema() {
            return schema;
        }

        protected void setSchema(String schema) {
            this.schema = schema;
        }

        protected Set<String> getSeparateStatements() {
            return separateStatements;
        }

        protected void setSeparateStatements(Set<String> separateStatements) {
            this.separateStatements = separateStatements;
        }
    }

    private static final Result result = new Result();

    public PostgreSQLCreateStatements(List<String> idFields) {
        this.idFields = idFields;
    }

    @SuppressWarnings("ClassEscapesDefinedScope")
    public Result run(Class<?> structure, Map<String, ArrayList<Attribute>> tablesToSchema) {
        String[] split = structure.getPackageName().split("\\.");
        safTSchema = "saf_t_" + split[split.length - 1];
        parseStructuresForRelationalDB(null, structure, tablesToSchema, false);
        List<String> statements = createStatement(tablesToSchema);
        result.setSeparateStatements(new HashSet<>(statements));
        result.setFullStatement(String.join("", statements));
        result.setSchema(safTSchema);
        return result;
    }

    private void parseStructuresForRelationalDB(String parentName,
        Class<?> structure,
        Map<String, ArrayList<Attribute>> tablesToSchema,
        boolean isParent1To1) {

        String simpleName = structure.getSimpleName();
        if (COMMON_CLASHING_SUB_STRUCTS.contains(simpleName)) {
            simpleName = parentName + simpleName;
        }

        List<Field> declaredFields = Arrays.stream(structure.getDeclaredFields())
            .collect(Collectors.toCollection(ArrayList::new));
        addInheritedFields(declaredFields, structure);
        addIdFields(parentName, tablesToSchema, simpleName, isParent1To1);

        breakDownFields(parentName, structure, tablesToSchema, declaredFields, simpleName);
    }

    private void breakDownFields(String parentName,
        Class<?> structure,
        Map<String, ArrayList<Attribute>> tablesToSchema,
        List<Field> declaredFields,
        String simpleName) {

        Map<String, Attribute> attributes = getClassAttributes(getFieldNames(declaredFields),
            getFieldTypes(declaredFields),
            getFieldTypeNames(declaredFields),
            getGenericClasses(declaredFields));

        for (var attributeEntry : attributes.entrySet()) {
            String[] fieldNameArr = attributeEntry.getKey().split("\\$");
            String fieldName = fieldNameArr[fieldNameArr.length - 1];
            String[] typeNameArr = attributeEntry.getValue().getTypeName().split("\\$");
            String typeName = stripType(typeNameArr[typeNameArr.length - 1]);
            Class<?> type = attributeEntry.getValue().getType();
            if (isCustomType(typeName)) {
                process1To1Relation(structure, tablesToSchema, simpleName, parentName, fieldName,
                    typeName, type);
                continue;
            } else if (isCustomType(typeName) || AUDIT_FILE_COLLECTIONS.contains(typeName)) {
                if (null == attributeEntry.getValue().getGenericClass()) {
                    continue;
                }
                structure = process1toManyRelation(tablesToSchema, simpleName, attributeEntry);
            }
            if (!typeName.equals("List")
                && !fieldName.equals("0")) { //this$0 is hidden reference of inner class to outer
                storeAttribute(tablesToSchema, simpleName, fieldName, typeName, type);
            }
        }
    }

    private void process1To1Relation(
        Class<?> structure,
        Map<String, ArrayList<Attribute>> tablesToSchema,
        String simpleName,
        String parentName,
        String name,
        String typeName,
        Class<?> type) {
        List<Class<?>> classes = List.of(structure.getClasses());
        for (var c : classes) {
            parseStructuresForRelationalDB(simpleName, c, tablesToSchema, false);
        }
        if ("0".equals(name)) {
            return;
        }
        if (List.of("1", "2", "3", "4", "5", "6", "7", "8", "9")
            .contains(typeName.substring(0, 1))) {
            //for field that Reflection prefixes with numbers
            typeName = typeName.substring(1);
        }
        storeAttribute(tablesToSchema, simpleName, name + "_" + typeName + "_1to1_id",
            "int", Integer.class);
        String simpleName1to1 = type.getSimpleName();
        if (COMMON_CLASHING_SUB_STRUCTS.contains(simpleName1to1)) {
            simpleName1to1 = parentName + simpleName1to1;
        }
        storeAttribute(tablesToSchema, simpleName1to1, "id", "int",
            Integer.class);
        parseStructuresForRelationalDB(parentName, type, tablesToSchema, true);
    }

    private Class<?> process1toManyRelation(Map<String, ArrayList<Attribute>> tablesToSchema,
        String simpleName,
        Entry<String, Attribute> attributeEntry) {
        Class<?> structure;
        structure = attributeEntry.getValue().getGenericClass();
        parseStructuresForRelationalDB(simpleName, structure, tablesToSchema, false);
        return structure;
    }

    public void executeStatements(String connectionString, String statement) {
        try (
            Connection connection = DriverManager.getConnection(connectionString);
            PreparedStatement preparedStatement = connection.prepareStatement(
                statement)
        ) {
            preparedStatement.executeQuery();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
        }
    }

    protected String derivePackageName(String name) {
        String[] split = name.split("\\.");
        return name.replace("." + split[split.length - 1], "");
    }

    private ArrayList<String> getFieldNames(List<Field> fields) {
        ArrayList<String> fieldNames = new ArrayList<>();
        for (Field field : fields) {
            fieldNames.add(field.getName());
        }
        return fieldNames;
    }

    private ArrayList<String> getFieldTypeNames(List<Field> fields) {
        ArrayList<String> fieldNames = new ArrayList<>();
        for (Field field : fields) {
            fieldNames.add(field.getType().getName());
        }
        return fieldNames;
    }

    private ArrayList<Class<?>> getFieldTypes(List<Field> fields) {
        return fields.stream().map(Field::getType)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private ArrayList<Class<?>> getGenericClasses(List<Field> fields) {
        ArrayList<Class<?>> fieldNames = new ArrayList<>();
        for (Field field : fields) {
            Type type = field.getGenericType();
            if (type instanceof Class<?>) {
                fieldNames.add(null);
            } else if (type instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
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

    private static void addInheritedFields(List<Field> declaredFields, Class<?> structure) {
        Class<?> superclass = structure.getSuperclass();
        if (Object.class == superclass) {
            return;
        }
        declaredFields.addAll(Arrays.stream(superclass.getDeclaredFields())
            .collect(Collectors.toCollection(ArrayList::new)));
        addInheritedFields(declaredFields, superclass);
    }

    private void addIdFields(String parentName, Map<String, ArrayList<Attribute>> tablesToSchema,
        String simpleName, boolean isParent1To1) {
        for (String idField : idFields) {
            storeAttribute(tablesToSchema, simpleName, "meta_" + idField, "String", String.class);
        }
        if (null != parentName && !isParent1To1) {
            storeAttribute(tablesToSchema, simpleName, parentName + "_id", "Integer",
                Integer.class);
        }
    }

    protected static boolean isCustomType(String type) {
        if (!JAKARTA_PARSED_TYPICAL_TYPES.contains(type)
            && !AUDIT_FILE_COLLECTIONS.contains(type)) {
            //for readability
            //noinspection RedundantIfStatement
            if (JAVA_ADDED_FIELD_TYPES.contains(type)) {
                return false;
            }
            return true;
        }
        return false;
    }

    private static void storeAttribute(Map<String, ArrayList<Attribute>> tablesToSchema,
        String simpleName, String name, String typeName, Class<?> type) {
        if (PSQL_SENSITIVE_NAMES.contains(name.toLowerCase())) {
            name += "_";
        }
        Attribute attribute = new Attribute(name, type, typeName, null);
        ArrayList<Attribute> dbAttributes = tablesToSchema.getOrDefault(
            simpleName, new ArrayList<>());
        if (!dbAttributes.contains(attribute)) {
            dbAttributes.add(attribute);
        }
        tablesToSchema.put(simpleName, dbAttributes);
    }

    private static Map<String, Attribute> getClassAttributes(ArrayList<String> fieldNames,
        ArrayList<Class<?>> fieldTypes, ArrayList<String> fieldTypeNames,
        ArrayList<Class<?>> genericClasses) {
        Map<String, Attribute> classAttributes = new HashMap<>();
        for (int i = 0; i < fieldNames.size(); i++) {
            classAttributes.put(fieldNames.get(i),
                new Attribute(fieldNames.get(i), fieldTypes.get(i), fieldTypeNames.get(i),
                    genericClasses.get(i)));
        }
        return classAttributes;
    }

    private List<String> createStatement(Map<String, ArrayList<Attribute>> tableToSchema) {
        List<String> createStatementList = new ArrayList<>();
        for (var entry : tableToSchema.entrySet()) {
            String tableName = entry.getKey();
            StringBuilder createStatement = new StringBuilder(
                "CREATE TABLE " + safTSchema + "." + tableName + " (");
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
            return "decimal";
        } else if ("int".equals(type)) {
            return "int";
        } else if ("BigInteger".equals(type)) {
            return "bigint";
        } else if ("XMLGregorianCalendar".equals(type)) {
            return "date";
        }
        return "varchar";
    }

    protected String stripType(String type) {
        String[] split = type.split("\\.");
        return split[split.length - 1];
    }
}
