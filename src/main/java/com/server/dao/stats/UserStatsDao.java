package com.server.dao.stats;
import com.server.entity.user.UserStats;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserStatsDao {

    UserStats findUserStats(@Param("userId") int userId);

    @Select("select coin_balance from user_stats where user_id = #{userId}")
    Integer findCoinByUserId(@Param("userId") Integer userId);

    @Insert("INSERT IGNORE INTO user_stats (user_id) VALUES (#{userId})")
    void insertUserStats(@Param("userId") int userId);

//    @Update("update user_stats set video_count=video_count-1 where user_id=#{id}")
//    void increaseVideoCountById(@Param("id") Integer id);
//
//    @Update("update user_stats set video_count=video_count-1 where user_id=#{id} AND video_count > 0")
//    void decreaseVideoCountById(@Param("id") Integer id);
}
