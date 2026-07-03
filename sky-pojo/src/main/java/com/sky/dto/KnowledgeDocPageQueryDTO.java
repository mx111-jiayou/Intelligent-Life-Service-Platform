package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class KnowledgeDocPageQueryDTO implements Serializable {

    private int page = 1;

    private int pageSize = 10;

    private String title;

    private String category;

    private Integer status;
}
