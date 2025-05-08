package com.server.dao.record;

import com.server.entity.cache.record.RecordUpdate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RecordDao {
    void batchUpdateRecord(@Param("records") List<RecordUpdate> recordUpdates, @Param("table") String table);
    void batchUpdateRelation(@Param("records") List<RecordUpdate> recordUpdates);

    @Select("select user_id from user_relation where target_id=#{authorId} and relation_type=1")
    List<Integer> findAllFanIdsByAuthorId(@Param("authorId") Integer authorId);
}
