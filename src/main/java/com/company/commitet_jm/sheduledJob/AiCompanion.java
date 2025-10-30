package com.company.commitet_jm.sheduledJob;

import org.springframework.context.ApplicationEvent;

public class AiCompanion extends ApplicationEvent {
    public AiCompanion(Object source) {
        super(source);
    }
}