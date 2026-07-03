create table if not exists chat_message
(
    id          bigint primary key auto_increment comment 'Primary key',
    user_id     bigint       null comment 'User id',
    session_id  varchar(64)  not null comment 'Chat session id',
    role        varchar(32)  not null comment 'Message role: user or assistant',
    content     text         not null comment 'Message content',
    create_time datetime     not null comment 'Create time',
    index idx_chat_message_user_session (user_id, session_id),
    index idx_chat_message_create_time (create_time)
) comment 'AI customer service chat message';
