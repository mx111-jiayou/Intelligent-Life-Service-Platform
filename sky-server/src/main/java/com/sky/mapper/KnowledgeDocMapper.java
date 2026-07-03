package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.KnowledgeDocPageQueryDTO;
import com.sky.entity.KnowledgeDoc;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeDocMapper {

    void insert(KnowledgeDoc knowledgeDoc);

    void update(KnowledgeDoc knowledgeDoc);

    Page<KnowledgeDoc> pageQuery(KnowledgeDocPageQueryDTO knowledgeDocPageQueryDTO);

    @Delete("delete from knowledge_doc where id = #{id}")
    void deleteById(Long id);

    @Select("select * from knowledge_doc where status = 1 and (title like concat('%', #{keyword}, '%') or content like concat('%', #{keyword}, '%')) order by update_time desc limit #{limit}")
    List<KnowledgeDoc> searchEnabled(String keyword, Integer limit);
}
