package com.sky.controller.admin;

import com.sky.dto.SlowSqlLogPageQueryDTO;
import com.sky.entity.SlowSqlLog;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SlowSqlLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/slow-sql")
@Api(tags = "慢SQL监控接口")
public class SlowSqlLogController {

    @Autowired
    private SlowSqlLogService slowSqlLogService;

    @GetMapping("/page")
    @ApiOperation("慢SQL分页查询")
    public Result<PageResult> page(SlowSqlLogPageQueryDTO slowSqlLogPageQueryDTO) {
        return Result.success(slowSqlLogService.pageQuery(slowSqlLogPageQueryDTO));
    }

    @GetMapping("/signature/{sqlSignature}")
    @ApiOperation("按SQL签名查询同类慢SQL")
    public Result<List<SlowSqlLog>> listBySignature(@PathVariable String sqlSignature) {
        return Result.success(slowSqlLogService.listBySignature(sqlSignature));
    }
}
