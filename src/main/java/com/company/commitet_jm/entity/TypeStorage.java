package com.company.commitet_jm.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

public enum TypeStorage implements EnumClass<Integer> {
    CF(10),
    CFE(20);

    private final Integer id;

    TypeStorage(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public static TypeStorage fromId(Integer id) {
        for (TypeStorage type : TypeStorage.values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}