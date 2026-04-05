package com.kairos.domain.port;

import com.kairos.domain.model.Source;

import java.util.List;

public interface SourceRepository {

    void save(Source source);

    Source findById(String id);

    List<Source> findAll();

}
