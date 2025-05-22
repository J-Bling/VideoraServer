package com.server.dao.notification;

import com.server.dto.response.video.VideoDataResponse;
import com.server.push.dto.response.HistoryNotificationResponse;
import com.server.push.entity.Notification;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.lang.Nullable;

import java.util.List;

public interface NotificationDao {

    @Select("select * from notification where user_id=#{userId} and is_read=0 order by created desc ")
    List<Notification> findUnreadMessagesByUserId(@Param("userId") Integer userId);

    @Select("select * from notification where user_id=#{userId} and is_read=0 and type=#{type} order by created desc")
    List<Notification> findUnreadNotificationByType(@Param("userId") Integer userId,@Param("type") int type);

    @Select("select * from notification where user_id=#{userId} and type=#{type} order by created desc limit #{offset},#{limit} ")
    List<Notification> findHistoryNotificationByType(@Param("userId") int userId,@Param("type") int type,@Param("offset") int offset,@Param("limit") int limit);

    List<HistoryNotificationResponse<VideoDataResponse>> findHistoryVideoDynamic(@Param("userId") Integer userId,
                                                                                @Nullable @Param("authorId") Integer authorId,
                                                                                @Param("type") Integer type,
                                                                                @Param("createdLast") long createdLast,
                                                                                @Param("limit") int limit);

    void batchInsert(@Param("notifications") List<Notification> notifications);
    void batchUpdateStatus(@Param("ids") List<String> ids);

    @Delete("delete from notification where user_id=#{userId} and type=#{type}")
    void deleteNotification(@Param("userId") Integer userId,@Param("type") int type);

    @Delete("delete from notification where user_id=#{userId} and target_id=#{targetId} and type=4 or type=5")
    void deleteNotificationForLetter(@Param("userId") int userId,@Param("targetId") int targetId);
}
