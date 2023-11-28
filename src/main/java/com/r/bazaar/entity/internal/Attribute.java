package com.r.bazaar.entity.internal;

import java.util.Objects;

public class Attribute {

    private String name;
    private String typeName;
    private Class<?> genericClass;

    public Attribute(String name, String typeName, Class<?> genericClass) {
        this.name = name;
        this.typeName = typeName;
        this.genericClass = genericClass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Class<?> getGenericClass() {
        return genericClass;
    }

    public void setGenericClass(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Attribute attribute = (Attribute) o;
        return Objects.equals(name, attribute.name) && Objects.equals(typeName,
            attribute.typeName) && Objects.equals(genericClass, attribute.genericClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, typeName, genericClass);
    }
}
