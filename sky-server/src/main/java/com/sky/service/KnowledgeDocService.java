package com.sky.service;

import com.sky.dto.KnowledgeDocDTO;
import com.sky.dto.KnowledgeDocPageQueryDTO;
import com.sky.entity.KnowledgeDoc;
import com.sky.result.PageResult;

import java.util.List;

public interface KnowledgeDocService {

    void save(KnowledgeDocDTO knowledgeDocDTO);

    void update(KnowledgeDocDTO knowledgeDocDTO);

    PageResult pageQuery(KnowledgeDocPageQueryDTO knowledgeDocPageQueryDTO);

    void deleteById(Long id);

    void startOrStop(Integer status, Long id);

    List<KnowledgeDoc> searchEnabled(String keyword, Integer limit);
}
