create table if not exists slow_sql_log
(
    id             bigint primary key auto_increment comment 'Primary key',
    mapper_id      varchar(255) not null comment 'MyBatis mapper statement id',
    sql_text       text         not null comment 'Original normalized SQL text',
    normalized_sql text         not null comment 'SQL after literal normalization',
    sql_signature  varchar(64)  not null comment 'SHA-256 signature of normalized SQL',
    cost_time      bigint       not null comment 'SQL execution cost in milliseconds',
    threshold_time bigint       not null comment 'Slow SQL threshold in milliseconds',
    create_time    datetime     not null comment 'Log creation time',
    index idx_slow_sql_signature (sql_signature),
    index idx_slow_sql_create_time (create_time),
    index idx_slow_sql_mapper (mapper_id)
) comment 'Slow SQL execution log';
