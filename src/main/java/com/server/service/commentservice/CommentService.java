package com.server.service.commentservice;

import com.server.dto.request.comment.CommentRequest;
import com.server.dto.request.comment.CommentUserActionRequest;
import com.server.dto.response.comment.CommentResponse;

import java.util.List;

/**
 * 废弃接口
 */
public interface CommentService {

    List<CommentResponse> getVideoComments(int videoId,int userId,int offset,int limit,boolean isHot);
    List<CommentResponse> getReplyComments(int rootId,int parentId,int userId,int offset,int limit,boolean isHot);
    List<CommentResponse> getPublicVideoComments(int videoId,int offset,int limit);
    List<CommentResponse> getPublicReplyComments(int rootId,int parentId,int offset,int limit);

    CommentResponse getPublicCommentByRedis(Integer commentId);
    List<CommentResponse> getPublicCommentsByRedis(Integer[] commentIds);

    void handleAction(CommentUserActionRequest commentUserActionRequest);
    void createComment(CommentRequest commentRequest,int userId);
    void deleteComment(int commentId,int videoId,int rootId,int parentId,int userId);
}
