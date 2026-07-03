package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlOptimizationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String sqlSignature;

    private String originalSql;

    private String optimizedSql;

    private String suggestion;

    private Integer score;

    private String modelName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
