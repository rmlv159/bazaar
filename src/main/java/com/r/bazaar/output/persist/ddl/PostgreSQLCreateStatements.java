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
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PostgreSQLCreateStatements {

    private static final Logger logger = LogManager.getLogger();
    private static final List<String> AUDIT_FILE_COLLECTIONS = List.of("List", "ArrayList");
    private static final List<String> COMMON_SUB_ITEMS = List.of("Line", "Settlement", "Supplier");
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
        recursiveEntityIterationForTables(null, structure, tablesToSchema);
        List<String> statements = createStatement(tablesToSchema);
        result.setSeparateStatements(statements.stream().collect(Collectors.toSet()));
        result.setFullStatement(String.join("", statements));
        result.setSchema(safTSchema);
        return result;
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

    public String derivePackageName(String name) {
        String[] split = name.split("\\.");
        return name.replace("." + split[split.length - 1], "");
    }

    public ArrayList<String> getFieldNames(List<Field> fields) {
        ArrayList<String> fieldNames = new ArrayList<>();
        for (Field field : fields) {
            fieldNames.add(field.getName());
        }
        return fieldNames;
    }

    public ArrayList<String> getFieldTypes(List<Field> fields) {
        ArrayList<String> fieldNames = new ArrayList<>();
        for (Field field : fields) {
            fieldNames.add(field.getType().getName());
        }
        return fieldNames;
    }

    public ArrayList<Class<?>> getGenericClasses(List<Field> fields) {
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

    private void recursiveEntityIterationForTables(String parentName, Class<?> structure,
        Map<String, ArrayList<Attribute>> tablesToSchema) {
        String simpleName = structure.getSimpleName();
        if (COMMON_SUB_ITEMS.contains(simpleName)) {
            simpleName = parentName + simpleName;
        }

        List<Field> declaredFields = Arrays.stream(structure.getDeclaredFields())
            .collect(Collectors.toCollection(ArrayList::new));
        addInheritedFields(declaredFields, structure);
        ArrayList<String> fieldNames = getFieldNames(declaredFields);
        ArrayList<String> fieldTypeNames = getFieldTypes(declaredFields);
        ArrayList<Class<?>> genericClasses = getGenericClasses(declaredFields);

        Map<String, Attribute> classAttributes = getClassAttributes(
            fieldNames, fieldTypeNames, genericClasses);
        breakDownFields(parentName, structure, tablesToSchema, classAttributes, simpleName);
    }

    private static void addInheritedFields(List<Field> declaredFields, Class<?> structure) {
        Class<?> superclass = structure.getSuperclass();
        if (Object.class == superclass) {
            return;
        }
        addInheritedFields(declaredFields, superclass);
        declaredFields.addAll(Arrays.stream(superclass.getDeclaredFields())
            .collect(Collectors.toCollection(ArrayList::new)));
    }

    private void breakDownFields(String parentName, Class<?> structure,
        Map<String, ArrayList<Attribute>> tablesToSchema,
        Map<String, Attribute> classAttributes,
        String simpleName) {

        if (null != parentName) {
            for (String i : idFields) {
                storeAttribute(tablesToSchema, simpleName, i, "String");
            }
            storeAttribute(tablesToSchema, simpleName, parentName + "_id", "Integer");
        }

        List<Field> declaredFields;
        ArrayList<String> fieldNames;
        ArrayList<Class<?>> genericClasses;
        ArrayList<String> fieldTypeNames;

        for (var entry : classAttributes.entrySet()) {
            String[] nameArr = entry.getKey().split("\\$");
            String name = nameArr[nameArr.length - 1];
            String[] typeArr = entry.getValue().getTypeName().split("\\$");
            String type = stripType(typeArr[typeArr.length - 1]);
//            String innerClassName = StringUtils.capitalize(name);
            if (isType(name, type)) {
                Class<?> aClass = null;
                try {
                    aClass = Class.forName(
                        derivePackageName(structure.getName()) + "." + type);
                    recursiveEntityIterationForTables(null, aClass, tablesToSchema);
                } catch (ClassNotFoundException e) {
                    logger.debug("Probably an inner class");
//                    throw new RuntimeException(e);
                }
                List<Class<?>> classes = List.of(structure.getClasses());
                for (var c : classes) {
                    recursiveEntityIterationForTables(simpleName, c, tablesToSchema);
                }
            } else if (AUDIT_FILE_COLLECTIONS.contains(type)) {
                if (null == entry.getValue().getGenericClass()) {
                    continue;
                }
                parentName = simpleName;
                structure = entry.getValue().getGenericClass();
                List<Class<?>> classes = List.of(structure.getClasses());
                if (!classes.isEmpty()) {
                    for (var c : classes) {
                        recursiveEntityIterationForTables(simpleName, c, tablesToSchema);
                    }
                }
                declaredFields = Arrays.stream(structure.getDeclaredFields()).toList();
                fieldNames = getFieldNames(declaredFields);
                fieldTypeNames = getFieldTypes(declaredFields);
                genericClasses = getGenericClasses(declaredFields);
                classAttributes = getClassAttributes(
                    fieldNames, fieldTypeNames, genericClasses);
                simpleName = structure.getSimpleName();
                if (COMMON_SUB_ITEMS.contains(simpleName)) {
                    simpleName = parentName + simpleName;
                }
                breakDownFields(parentName, structure, tablesToSchema, classAttributes, simpleName);
            }
            if (!type.equals("List")) {
                storeAttribute(tablesToSchema, simpleName, name, type);
            }
        }
    }

    protected boolean isType(String name, String type) {
//        if (StringUtils.capitalize(name).equals(type) //TODO maybe not needed
//            || "uomTable".equals(name)) {
//            return true;
//        } else
        if (!JAKARTA_PARSED_TYPICAL_TYPES.contains(type)
            && !AUDIT_FILE_COLLECTIONS.contains(type)) {
            if (JAVA_ADDED_FIELD_TYPES.contains(type)) {
                return false;
            }
//            logger.debug("Add to retainedStructuresForLaterParsing");
//            retainedStructuresForLaterParsing.add(type);
            return true;
        }
        return false;
    }

    //TODO remove when sure that other recursive procesure is correct
//    void parseRetainedTypes(String schemaPackageName,
//        Map<String, ArrayList<Attribute>> tablesToSchema) {
//        Iterator<String> iterator = retainedStructuresForLaterParsing.iterator();
//        while (iterator.hasNext()) {
//            try {
//                String struct = iterator.next();
//                logger.info("parseRetainedTypes: {}", struct);
//                System.out.println(schemaPackageName + "." + struct);
//                Class<?> aClass = Class.forName(schemaPackageName + "." + struct);
//                createDDL(null, aClass, tablesToSchema);
//            } catch (ClassNotFoundException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }

    private static void storeAttribute(Map<String, ArrayList<Attribute>> tablesToSchema,
        String simpleName, String name, String type) {
        if (PSQL_SENSITIVE_NAMES.contains(name.toLowerCase())) {
            name += "_";
        }
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
            return "numeric";
        } else if ("int".equals(type)) {
            return "int";
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
