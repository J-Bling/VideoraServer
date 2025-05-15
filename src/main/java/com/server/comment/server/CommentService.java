package com.server.comment.server;

import com.server.comment.dto.request.CreateCommentRequest;
import com.server.comment.dto.response.CommentResponse;
import com.server.comment.entity.Comment;

import java.util.List;

public interface CommentService {
    Comment getCommentOnCache(String commentId);
    String createComment(CreateCommentRequest request, int authorId);
    List<CommentResponse> getCommentsByVideoId(Integer videoId,int offset,Integer userId) throws InterruptedException;
    List<CommentResponse> getCommentByVideoWithScore(Integer videoId,Integer userId,int offset);
    List<CommentResponse> getReplyByVideoId(Integer videoId,String rootId,Integer userId,int offset) throws InterruptedException;
    boolean like(String commentId,Integer userId,Integer videoId);
    boolean disLike(String commentId,Integer userId,Integer videoId);
    void deleteComment(String commentId,Integer userId,Integer videoId);
}
