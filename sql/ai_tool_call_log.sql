create table if not exists ai_tool_call_log
(
    id          bigint primary key auto_increment comment 'Primary key',
    user_id     bigint       null comment 'User id',
    session_id  varchar(64)  not null comment 'Chat session id',
    tool_name   varchar(64)  not null comment 'Tool name',
    tool_params text         null comment 'Tool input params',
    tool_result text         null comment 'Tool output result',
    success     int          not null default 1 comment '1 success, 0 failed',
    create_time datetime     not null comment 'Create time',
    index idx_ai_tool_user_session (user_id, session_id),
    index idx_ai_tool_name (tool_name),
    index idx_ai_tool_create_time (create_time)
) comment 'AI function calling tool log';
