package com.server.dao.message;

import com.server.message.entity.Message;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MessageDao {
    List<Message> findMessageForCache(@Param("room") String room,@Param("offset") int offset,@Param("limit") int limit);
    List<Message> findMessageByLastCreated(@Param("room") String room,@Param("lastCreated") long lastCreated,@Param("limit") int limit);

    void batchInsertMessage(@Param("messages") List<Message> messages);

    @Delete("delete from message where created > #{created}")
    void cleanMessage(@Param("created") long created);
}
