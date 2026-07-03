package com.sky.controller.admin;

import com.sky.dto.SqlOptimizationResultDTO;
import com.sky.entity.SqlOptimizationResult;
import com.sky.result.Result;
import com.sky.service.SqlOptimizationResultService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/sql-optimization")
@Api(tags = "SQL优化结果接口")
public class SqlOptimizationResultController {

    @Autowired
    private SqlOptimizationResultService sqlOptimizationResultService;

    @PostMapping("/result")
    @ApiOperation("保存SQL优化结果")
    public Result<String> saveOrUpdate(@RequestBody SqlOptimizationResultDTO sqlOptimizationResultDTO) {
        sqlOptimizationResultService.saveOrUpdate(sqlOptimizationResultDTO);
        return Result.success();
    }

    @GetMapping("/result/{sqlSignature}")
    @ApiOperation("按SQL签名查询优化结果")
    public Result<SqlOptimizationResult> getBySignature(@PathVariable String sqlSignature) {
        return Result.success(sqlOptimizationResultService.getBySignature(sqlSignature));
    }
}
