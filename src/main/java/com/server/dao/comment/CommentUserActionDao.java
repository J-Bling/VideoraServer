package com.server.dao.comment;
import com.server.dto.response.comment.CommentResponse;
import com.server.entity.comment.CommentUserActions;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CommentUserActionDao {

    void deleteBatchCascade(@Param("commentIds") List<Integer> commentIds);

    List<CommentUserActions> findActionList(@Param("userId") int userId, @Param("comments")List<CommentResponse> comments);

    @Select("select comment_id,user_id,action_type from comment_user_actions where user_id=#{userId} and comment_id=#{commentId}")
    CommentUserActions findAction(@Param("commentId") int commentId,@Param("userId") int userId);
}
