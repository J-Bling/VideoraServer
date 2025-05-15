package com.server.comment.dao;

import com.server.comment.dto.response.CommentResponse;
import com.server.comment.entity.Comment;
import com.server.comment.entity.CommentUserActions;
import com.server.comment.server.CommentServiceImpl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.lang.Nullable;
import java.util.List;


@Mapper
public interface CommentDao {
    Comment findComment(@Param("commentId") String commentId,@Param("userId") Integer userId);
    void deleteComment(@Param("commentId") String commentId);
    CommentUserActions findAction(@Param("commentId") String commentId,@Param("userId") Integer userId);
    Comment findCommentStats(@Param("commentId") String commentId);

    List<CommentResponse> findCommentByVideoId(@Param("videoId") Integer videoID,
                                               @Nullable @Param("userId") Integer userId ,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit
    );

    List<CommentResponse> findCommentByRootId(@Param("videoId") Integer videoId,
                                              @Param("rootId") String rootId,
                                              @Nullable @Param("userId") Integer userId,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit
    );

    List<CommentResponse> findCommentByHot(@Param("videoId") Integer videoId,
                                           @Nullable @Param("userId") Integer userId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit
    );


    void batchInsertAction(@Param("actions") List<CommentUserActions> actions);
    void batchDeleteAction(@Param("actions") List<CommentUserActions> actions);

    void batchInsertComment(@Param("comments") List<Comment> comments);

    void batchUpdate(@Param("tasks")List<CommentServiceImpl.CommentUpdate> tasks);
}
