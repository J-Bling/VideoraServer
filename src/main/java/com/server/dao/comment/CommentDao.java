package com.server.dao.comment;
import com.server.dto.cache.StatsUpdateTask;
import com.server.dto.response.comment.CommentResponse;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CommentDao {

    //只查找第一级的comment
    List<CommentResponse> findVideoCommentsByVideoIdOnNew(@Param("videoId") int videoId,
                                                     @Param("userId") int userId,
                                                     @Param("offset") int offset,
                                                     @Param("limit") int limit);
    List<CommentResponse> findVideoCommentsByVideoIdOnHot(@Param("videoId") int videoId,
                                                          @Param("userId") int userId,
                                                          @Param("offset") int offset,
                                                          @Param("limit") int limit);
    List<CommentResponse> findReplyCommentsByParentIdOnNew(@Param("rootId") int rootId,
                                                      @Param("parentId") int parentId,
                                                      @Param("userId") int userId,
                                                      @Param("offset") int offset,
                                                      @Param("limit") int limit);
    List<CommentResponse> findReplyCommentsByParentIdOnHot(@Param("rootId") int rootId,
                                                           @Param("parentId") int parentId,
                                                           @Param("userId") int userId,
                                                           @Param("offset") int offset,
                                                           @Param("limit") int limit);

    List<CommentResponse> findPublicVideoComments(@Param("videoId") int videoId,@Param("offset") int offset,
                                                  @Param("limit") int limit);
    List<CommentResponse> findPublicReplyComments(@Param("rootId") int rootId,@Param("parentId") int parentId,
                                                  @Param("offset") int offset,@Param("limit") int limit);

    @Select("select id,video_id,root_id,parent_id,reply_count,like_count,dislike_count,content,created from comment where id=#{commentId}")
    CommentResponse findPublicComment(@Param("commentId") Integer commentId);

    List<CommentResponse> findPublicComments(@Param("commentIds") Integer commentIds);

    @Delete("delete from comment where id=#{commentId} and user_id=#{userId}")
    int deleteComment(@Param("commentId") int commentId,@Param("userId") int userId);
    void updateBatchComments(@Param("column") String column, @Param("tasks")List<StatsUpdateTask> tasks);
}
