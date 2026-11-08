package com.maplenou.support.notification.application.port;

import com.maplenou.support.notification.domain.MessageTemplate;

import java.util.List;

public interface TemplateStore {

    List<MessageTemplate> allTemplates();

    void save(MessageTemplate template);
}
