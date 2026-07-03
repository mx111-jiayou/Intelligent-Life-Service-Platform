create table if not exists knowledge_doc
(
    id          bigint primary key auto_increment comment 'Primary key',
    title       varchar(128) not null comment 'Document title',
    category    varchar(64)  not null comment 'Document category',
    content     text         not null comment 'Document content',
    status      int          not null default 1 comment '1 enabled, 0 disabled',
    create_time datetime     not null comment 'Create time',
    update_time datetime     not null comment 'Update time',
    index idx_knowledge_doc_category (category),
    index idx_knowledge_doc_status (status),
    index idx_knowledge_doc_update_time (update_time)
) comment 'AI customer service knowledge document';
