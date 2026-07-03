package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SqlOptimizationResultDTO implements Serializable {

    private String sqlSignature;

    private String originalSql;

    private String optimizedSql;

    private String suggestion;

    private Integer score;

    private String modelName;
}
