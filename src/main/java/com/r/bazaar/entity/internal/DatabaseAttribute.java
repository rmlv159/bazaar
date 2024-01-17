package com.r.bazaar.entity.internal;

public class DatabaseAttribute extends Attribute {

    private final String constraint;
    public DatabaseAttribute(String name, Class<?> type, String typeName, Class<?> genericClass,
        String constraint) {
        super(name, type, typeName, genericClass);
        this.constraint = constraint;
    }

    public String getConstraint() {
        return constraint;
    }
}
