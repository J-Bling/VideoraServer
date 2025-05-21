package com.server.dao.message;

import com.server.message.entity.MessageLife;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MessageLifeDao {
    List<MessageLife> findAll();
    void batchInsert(@Param("lives")List<MessageLife> lives);
    void batchDelete();
}
