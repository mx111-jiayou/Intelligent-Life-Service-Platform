package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class KnowledgeDocDTO implements Serializable {

    private Long id;

    private String title;

    private String category;

    private String content;

    private Integer status;
}
