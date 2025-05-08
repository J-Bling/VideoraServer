package com.server.comment;

import com.server.dao.comment.CommentDao;
import com.server.dto.request.comment.CommentRequest;
import com.server.dto.request.comment.CommentUserActionRequest;
import com.server.dto.response.comment.CommentResponse;
import com.server.service.commentservice.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


class Consumer implements Runnable{
    protected  int id;
    protected  CommentService  commentService;

    public Consumer(CommentService commentService,int id){
        this.commentService=commentService;
        this.id=id;
    }
    public Consumer(){}

    private void createReplyComment(){
        CommentRequest commentRequest= new CommentRequest();
        commentRequest.setRoot_id(1);
        commentRequest.setParent_id(1);
        commentRequest.setContent("test reply");
        commentRequest.setVideo_id(1);
        this.commentService.createComment(commentRequest,1);
    }

    private List<CommentResponse> findPublicVideoComments(){
        int offset=0,limit=30,videoId=1;//
        return this.commentService.getPublicVideoComments(videoId,offset,limit);
    }

    private List<CommentResponse> findPublicReplyComments(){
        return this.commentService.getPublicReplyComments(1,1,0,30);
    }

    private List<CommentResponse> findReplyComments(){
        return this.commentService.getReplyComments(1,1,1,0,30,true);
    }

    private List<CommentResponse> findVideoComments(){
        return this.commentService.getVideoComments(1,1,0,30,true);
    }

    private List<CommentResponse> getPublicCommentsByVideo(){
        return this.commentService.getPublicVideoComments(1,0,30);
    }

    @Override
    public void run(){
        long start=System.currentTimeMillis();
        List<CommentResponse> commentResponses =this.commentService.getVideoComments(1,1,0,30,true);
        long end=System.currentTimeMillis();
        System.out.println("id "+id + "用时 " +(end-start) +"  comments : " +commentResponses);
    }
}

class Consumer2 extends Consumer{

    public Consumer2(CommentService commentService, int id) {
        super(commentService, id);
    }
    public List<CommentResponse> getCommentReply(){
        return this.commentService.getReplyComments(1,1,1,30,30,true);
    }

    public List<CommentResponse> getCommentRoot(){
        return this.commentService.getVideoComments(1,1,30,30,true);
    }

    private List<CommentResponse> getCommentByVideoIdWithNew(){
        return this.commentService.getVideoComments(1,1,0,30,false);
    }
    private List<CommentResponse> getCommentByReply(){
        return this.commentService.getReplyComments(1,1,1,0,30,false);
    }

    private List<CommentResponse> getPublicCommentsByVideo(){
        return this.commentService.getPublicVideoComments(1,0,30);
    }

    @Override
    public void run(){
        long start=System.currentTimeMillis();
        List<CommentResponse> commentResponses =this.commentService.getVideoComments(1,2,2,30,true);
        long end=System.currentTimeMillis();
        System.out.println("id "+id + "用时 " +(end-start) +"  comments : " +commentResponses);
    }
}

class Consumer3 extends Consumer{
    protected  String content;
    private CommentUserActionRequest request;
    public Consumer3(CommentService commentService, int id,String content) {
        super(commentService,id);
        this.content=content;
    }
    public Consumer3(CommentService commentService,CommentUserActionRequest request){
        super(commentService,1);
        this.request=request;
    }

    private void Like(){
        if(request==null) return;
        this.commentService.handleAction(request);
    }

    private void unLike(){
        if(request==null || commentService==null) return;
        this.commentService.handleAction(request);
    }

    private void cancelAction(){
        if(request==null || commentService==null) return;
        this.commentService.handleAction(request);
    }


    @Override
    public void run(){
        long start =System.currentTimeMillis();
        try{
            unLike();
        }catch (Exception e){
            System.out.print("异常"+e);
        }
        long end =System.currentTimeMillis();
        System.out.println("线程执行完毕,用时  " + (end-start) ) ;
    }
}

class deleteConsumer extends Consumer{
    private final CommentService commentService;
    private int commentId;
    private int videoId;
    private int rootId;
    private int parentId;
    private int userId;

    public deleteConsumer(CommentService commentService,int commentId,int videoId,int rootId,int parentId ,int userId){
        this.commentService=commentService;
        this.commentId=commentId;
        this.videoId=videoId;
        this.rootId=rootId;
        this.parentId=parentId;
        this.userId=userId;
    }


    @Override
    public void run(){
        long start =System.currentTimeMillis();
        try{
            this.commentService.deleteComment(commentId,videoId,rootId,parentId,userId);
        }catch (Exception e){
            System.out.print("异常"+e);
        }
        long end =System.currentTimeMillis();
        System.out.println("线程执行完毕,用时  " + (end-start) ) ;

    }
}



@SpringBootTest
public class CommentServerTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentDao commentDao;

    @Test
    public void likeTest()throws InterruptedException{
        try{
            Thread t1=new Thread(new Consumer3(commentService,new CommentUserActionRequest(1,0,3,2,true)));
            t1.start();
            Thread t2 =new Thread(new Consumer3(commentService,new CommentUserActionRequest(1,0,3,2,true)));
            t2.start();
            new Thread(new Consumer3(commentService,new CommentUserActionRequest(1,0,3,2,true))).start();
            new Thread(new Consumer3(commentService,new CommentUserActionRequest(1,0,3,2,true))).start();

        }catch (Exception e){
            return;
        }
        Thread.sleep(24*60*60*1000);
    }

    @Test
    public void unLike() throws InterruptedException{
        try{
            Thread t1=new Thread(new Consumer3(commentService,new CommentUserActionRequest(1,0,3,2,null)));
            Thread t2 =new Thread(new Consumer3(commentService,new CommentUserActionRequest(1,0,3,2,null)));
            t1.start();
            t2.start();
            new Thread(new Consumer3(commentService,new CommentUserActionRequest(1,0,3,2,null))).start();
            new Thread(new Consumer3(commentService,new CommentUserActionRequest(1,0,3,2,null))).start();
        }catch (Exception e){
            return;
        }
        Thread.sleep(24*60*60*1000);
    }


    @Test
    public void findCommentTest() throws InterruptedException {
        long start=System.currentTimeMillis();
        try {
            Thread t1= new Thread(new Consumer(commentService,1));
            Thread t2 =new Thread(new Consumer2(commentService,2));

            t1.start();t1.join();
            t2.start();

        }catch (Exception e){
            System.out.println("debug error reason is "+ e);
        }
        long end=System.currentTimeMillis();
        System.out.println("用时 "+ (end-start));
        Thread.sleep(24*60*60*1000);
    }



    @Test
    public void createComment() throws InterruptedException {
        try{
            for(int i=0;i<300;i++){
                Thread t= new Thread(new Consumer3(commentService,i,"tester concurrent"+i));
//                t.start();
            }
        }catch (Exception e){
            return;
        }

        Thread.sleep(24*60*60*1000);
    }

    @Test
    public void deleteTest() throws InterruptedException{
        long start=System.currentTimeMillis();
        try {
            for(int i=0;i<100;i++){
                Thread t=new Thread(new deleteConsumer(commentService,230+i ,1,1,1,1));
                t.start();
                t.join();
            }
        }catch (Exception e){
            System.out.println("debug error reason is "+ e);
        }
        long end=System.currentTimeMillis();
        System.out.println("用时 "+ (end-start));
        Thread.sleep(24*60*60*1000);
    }
}
