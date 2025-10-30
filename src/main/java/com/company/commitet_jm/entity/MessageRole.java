package com.company.commitet_jm.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

public enum MessageRole implements EnumClass<String> {
    USER("A"),
    ASSISTANT("B"),
    SYSTEM("C");

    private final String id;

    MessageRole(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public static MessageRole fromId(String id) {
        for (MessageRole role : MessageRole.values()) {
            if (role.id.equals(id)) {
                return role;
            }
        }
        return null;
    }
}