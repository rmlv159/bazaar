package com.r.bazaar;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.sql.Types;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class Decoder {

    public static final Map<String, Integer> SQL_TYPE_MAP = new HashMap<>() {{
        put("INTEGER", Types.INTEGER);
        put("INT", Types.INTEGER);
        put("BIGINT", Types.BIGINT);
        put("TINYINT", Types.TINYINT);
        put("SMALLINT", Types.SMALLINT);

        put("FLOAT", Types.FLOAT);
        put("DOUBLE", Types.DOUBLE);
        put("DECIMAL", Types.DECIMAL);
        put("REAL", Types.REAL);
        put("NUMERIC", Types.NUMERIC);

        put("DATE", Types.DATE);
        put("TIME", Types.TIME);
        put("DATETIME", Types.TIMESTAMP);
        put("TIMESTAMP", Types.TIMESTAMP);
        put("DATETIME2", Types.TIMESTAMP);

        put("BOOLEAN", Types.BOOLEAN);
        put("BIT", Types.BOOLEAN);
        put("BINARY", Types.BINARY);
        put("VARBINARY", Types.VARBINARY);
        put("IMAGE", Types.VARBINARY);

        put("VARCHAR", Types.VARCHAR);
        put("NVARCHAR", Types.NVARCHAR);
        put("CHAR", Types.CHAR);
        put("TEXT", Types.VARCHAR);
        put("NTEXT", Types.NVARCHAR);
        put("NCHAR", Types.NCHAR);
        put("UNIQUEIDENTIFIER", Types.VARCHAR); //TODO handle this properly
    }};

    public Object base64(String type, String value) throws ClassNotFoundException {
        Integer sqlType = SQL_TYPE_MAP.get(type.toUpperCase());
        byte[] bytes = Base64.getDecoder().decode(value);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        Class<?> javaType = getJavaType(sqlType);
        try {
            if (Integer.class.equals(javaType)) {
                return bb.getInt();
            } else if (Double.class.equals(javaType)) {
                return bb.getDouble();
            } else { //TODO good idea to take String as default?
                return bb.toString();
            }
        } catch (BufferUnderflowException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Class<?> getJavaType(Integer sqlType) throws ClassNotFoundException {
        //TODO unify in bazaar-lib for other side?
        //noinspection EnhancedSwitchMigration
        switch (sqlType) {
            case Types.INTEGER,
                Types.BIGINT,
                Types.TINYINT,
                Types.SMALLINT:
                return Integer.class;
            case Types.FLOAT,
                Types.DOUBLE,
                Types.DECIMAL,
                Types.REAL,
                Types.NUMERIC:
                return Double.class;
            case Types.DATE,
                Types.TIME,
                Types.TIMESTAMP:
                return String.class; //TODO parse this
            case Types.VARCHAR,
                Types.NVARCHAR,
                Types.CHAR,
                Types.NCHAR:
                return String.class;
            default:
                //TODO add more types
                throw new ClassNotFoundException("cannot parse this type");
        }
    }
}