package com.company.commitet_jm.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

public enum StatusSheduler implements EnumClass<String> {
    NEW("A"),
    PROCESSED("B"),
    COMPLETE("C"),
    ERROR("D");

    private final String id;

    StatusSheduler(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public static StatusSheduler fromId(String id) {
        for (StatusSheduler status : StatusSheduler.values()) {
            if (status.id.equals(id)) {
                return status;
            }
        }
        return null;
    }
}