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
public class KnowledgeDoc implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String category;

    private String content;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
