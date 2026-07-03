create table if not exists sql_optimization_result
(
    id             bigint primary key auto_increment comment 'Primary key',
    sql_signature  varchar(64)  not null comment 'SQL signature',
    original_sql    text         not null comment 'Original SQL',
    optimized_sql   text         null comment 'Optimized SQL',
    suggestion      text         not null comment 'Optimization suggestion',
    score           int          null comment 'Optimization score',
    model_name      varchar(64)  null comment 'AI model name',
    create_time     datetime     not null comment 'Create time',
    update_time     datetime     not null comment 'Update time',
    unique key uk_sql_optimization_signature (sql_signature),
    index idx_sql_optimization_update_time (update_time)
) comment 'SQL optimization result cache';
