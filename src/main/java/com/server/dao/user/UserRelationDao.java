package com.server.dao.user;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


public interface UserRelationDao {

    @Select("select relation_type from user_relation where user_id=#{userId} and target_id=#{targetId}")
    Boolean findRelationType(@Param("userId") Integer userId,@Param("targetId") Integer targetId);
}
