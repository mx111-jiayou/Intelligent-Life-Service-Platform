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
public class SlowSqlLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String mapperId;

    private String sqlText;

    private String normalizedSql;

    private String sqlSignature;

    private Long costTime;

    private Long thresholdTime;

    private LocalDateTime createTime;
}
