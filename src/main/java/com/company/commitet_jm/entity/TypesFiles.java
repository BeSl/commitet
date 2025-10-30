package com.company.commitet_jm.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

public enum TypesFiles implements EnumClass<String> {
    EXTERNAL_CODE("A"),
    DATAPROCESSOR("B"),
    REPORT("C"),
    SCHEDULEDJOBS("D"),
    EXCHANGE_RULES("E");

    private final String id;

    TypesFiles(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public static TypesFiles fromId(String id) {
        for (TypesFiles type : TypesFiles.values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}