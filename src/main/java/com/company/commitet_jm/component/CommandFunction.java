package com.company.commitet_jm.component;

@FunctionalInterface
public interface CommandFunction {
    void invoke() throws Exception;
}