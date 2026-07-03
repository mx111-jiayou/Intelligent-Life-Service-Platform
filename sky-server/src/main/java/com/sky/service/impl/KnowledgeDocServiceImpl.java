package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.StatusConstant;
import com.sky.dto.KnowledgeDocDTO;
import com.sky.dto.KnowledgeDocPageQueryDTO;
import com.sky.entity.KnowledgeDoc;
import com.sky.mapper.KnowledgeDocMapper;
import com.sky.result.PageResult;
import com.sky.service.KnowledgeDocService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KnowledgeDocServiceImpl implements KnowledgeDocService {

    @Autowired
    private KnowledgeDocMapper knowledgeDocMapper;

    @Override
    public void save(KnowledgeDocDTO knowledgeDocDTO) {
        KnowledgeDoc knowledgeDoc = new KnowledgeDoc();
        BeanUtils.copyProperties(knowledgeDocDTO, knowledgeDoc);
        if (knowledgeDoc.getStatus() == null) {
            knowledgeDoc.setStatus(StatusConstant.ENABLE);
        }
        LocalDateTime now = LocalDateTime.now();
        knowledgeDoc.setCreateTime(now);
        knowledgeDoc.setUpdateTime(now);
        knowledgeDocMapper.insert(knowledgeDoc);
    }

    @Override
    public void update(KnowledgeDocDTO knowledgeDocDTO) {
        KnowledgeDoc knowledgeDoc = new KnowledgeDoc();
        BeanUtils.copyProperties(knowledgeDocDTO, knowledgeDoc);
        knowledgeDoc.setUpdateTime(LocalDateTime.now());
        knowledgeDocMapper.update(knowledgeDoc);
    }

    @Override
    public PageResult pageQuery(KnowledgeDocPageQueryDTO knowledgeDocPageQueryDTO) {
        PageHelper.startPage(knowledgeDocPageQueryDTO.getPage(), knowledgeDocPageQueryDTO.getPageSize());
        Page<KnowledgeDoc> page = knowledgeDocMapper.pageQuery(knowledgeDocPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public void deleteById(Long id) {
        knowledgeDocMapper.deleteById(id);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        KnowledgeDoc knowledgeDoc = KnowledgeDoc.builder()
                .id(id)
                .status(status)
                .updateTime(LocalDateTime.now())
                .build();
        knowledgeDocMapper.update(knowledgeDoc);
    }

    @Override
    public List<KnowledgeDoc> searchEnabled(String keyword, Integer limit) {
        Integer queryLimit = limit == null || limit <= 0 ? 5 : limit;
        return knowledgeDocMapper.searchEnabled(keyword, queryLimit);
    }
}
