package com.maplenou.support.ticket.application.port;

import com.maplenou.support.ticket.domain.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryStore {

    List<Category> all();

    Optional<Category> find(String code);

    void save(Category category);
}
