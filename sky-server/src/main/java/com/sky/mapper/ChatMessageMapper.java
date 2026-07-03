package com.sky.mapper;

import com.sky.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    void insert(ChatMessage chatMessage);

    @Select("select * from chat_message where user_id = #{userId} and session_id = #{sessionId} order by create_time desc limit #{limit}")
    List<ChatMessage> listRecent(@Param("userId") Long userId,
                                 @Param("sessionId") String sessionId,
                                 @Param("limit") Integer limit);
}
