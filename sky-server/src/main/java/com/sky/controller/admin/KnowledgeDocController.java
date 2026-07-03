package com.sky.controller.admin;

import com.sky.dto.KnowledgeDocDTO;
import com.sky.dto.KnowledgeDocPageQueryDTO;
import com.sky.entity.KnowledgeDoc;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.KnowledgeDocService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/knowledge-doc")
@Api(tags = "客服知识库接口")
@Slf4j
public class KnowledgeDocController {

    @Autowired
    private KnowledgeDocService knowledgeDocService;

    @PostMapping
    @ApiOperation("新增知识库文档")
    public Result<String> save(@RequestBody KnowledgeDocDTO knowledgeDocDTO) {
        log.info("save knowledge doc: {}", knowledgeDocDTO);
        knowledgeDocService.save(knowledgeDocDTO);
        return Result.success();
    }

    @PutMapping
    @ApiOperation("修改知识库文档")
    public Result<String> update(@RequestBody KnowledgeDocDTO knowledgeDocDTO) {
        knowledgeDocService.update(knowledgeDocDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("知识库文档分页查询")
    public Result<PageResult> page(KnowledgeDocPageQueryDTO knowledgeDocPageQueryDTO) {
        return Result.success(knowledgeDocService.pageQuery(knowledgeDocPageQueryDTO));
    }

    @DeleteMapping
    @ApiOperation("删除知识库文档")
    public Result<String> deleteById(Long id) {
        knowledgeDocService.deleteById(id);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation("启用禁用知识库文档")
    public Result<String> startOrStop(@PathVariable Integer status, Long id) {
        knowledgeDocService.startOrStop(status, id);
        return Result.success();
    }

    @GetMapping("/search")
    @ApiOperation("检索启用知识库文档")
    public Result<List<KnowledgeDoc>> search(String keyword, Integer limit) {
        return Result.success(knowledgeDocService.searchEnabled(keyword, limit));
    }
}
