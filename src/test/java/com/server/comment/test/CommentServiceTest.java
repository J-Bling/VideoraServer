package com.server.comment.test;

import com.server.comment.dto.request.CreateCommentRequest;
import com.server.comment.dto.response.CommentResponse;
import com.server.comment.server.CommentService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class CommentServiceTest {

    @Autowired private CommentService commentService;

    Integer authorId2 =100019;
    Integer authorId = 100020;
    Integer videoId1 = 10;
    Integer videoId2 =15;

    Logger logger = LoggerFactory.getLogger(CommentServiceTest.class);


    long sleepTime = 24*60*60*1000;

    @Test
    public void createForVideo() throws InterruptedException {
        CreateCommentRequest request1 = new CreateCommentRequest("test1",videoId1);
        CreateCommentRequest request2 = new CreateCommentRequest("test2",videoId2);

        request1.setRoot_id("5c992288d86b4d53b6a3e5a0a19d5e8e");
        request1.setParent_id("0c132ae228a14550b833c036f2953b05");

        request2.setParent_id("31ddeb2196134a189a3461cc33ef2308");
        request2.setRoot_id("5c992288d86b4d53b6a3e5a0a19d5e8e");

        commentService.createComment(request1,authorId);
        commentService.createComment(request2,authorId);
        Thread.sleep(sleepTime);
    }

    @Test
    public void concurrentCreate() throws InterruptedException {
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(10,150,10, TimeUnit.SECONDS,new ArrayBlockingQueue<>(100));

//        for(int i=0;i<100;i++){
//            poolExecutor.execute(()->{
//                CreateCommentRequest request1 = new CreateCommentRequest("test1",videoId1);
//                CreateCommentRequest request2 = new CreateCommentRequest("test2",videoId2);
//                request1.setRoot_id("0fe3efd7dd064d06999d96a34bd1058c");
//                request1.setParent_id("0fe3efd7dd064d06999d96a34bd1058c");
//                request2.setRoot_id("0e955a7bc4704edea4eabb1e009146bd");
//                request2.setParent_id("0e955a7bc4704edea4eabb1e009146bd");
//
//                commentService.createComment(request1,authorId2);
//                commentService.createComment(request2,authorId2);
//            });
//        }


        for(int i=0;i<100;i++){
            poolExecutor.execute(()->{
                long start =System.currentTimeMillis();
                CreateCommentRequest request2 = new CreateCommentRequest("test2",videoId2);

                request2.setParent_id("fa161c8ee7fd4199adca8597e9c2a6a2");
                request2.setRoot_id("0e955a7bc4704edea4eabb1e009146bd");

                commentService.createComment(request2,authorId);
                logger.info("{}: run :{}", Thread.currentThread().getName(), System.currentTimeMillis() - start);
            });
        }

        poolExecutor.shutdown();

        Thread.sleep(sleepTime);
    }


    String commentId1 = "0e955a7bc4704edea4eabb1e009146bd";
    String commentId2 = "5c992288d86b4d53b6a3e5a0a19d5e8e";

    @Test
    public void like() throws InterruptedException {
        new Thread(()->{
            long start = System.currentTimeMillis();
            commentService.like(commentId1,authorId,videoId2);
            logger.info("1 : {}",System.currentTimeMillis()-start);
        }).start();
//        new Thread(()->{
//            long start = System.currentTimeMillis();
//            commentService.like(commentId2,authorId2,videoId1);
//            logger.info("2 : {}",System.currentTimeMillis()-start);
//        }).start();


        Thread.sleep(sleepTime);
    }

    @Test
    public void concurrentLike() throws InterruptedException {
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(10,150,10, TimeUnit.SECONDS,new ArrayBlockingQueue<>(100));


        for(int i = 0 ;i<50 ;i++){
            poolExecutor.execute(()->{
                long start = System.currentTimeMillis();
                boolean status = commentService.like(commentId1,authorId,videoId2);
                logger.info("1 : {} time :{}",status,System.currentTimeMillis()-start);
            });
        }

        for(int i = 0 ;i<50 ;i++){
            poolExecutor.execute(()->{
                long start = System.currentTimeMillis();
                boolean status = commentService.like(commentId2,authorId2,videoId1);
                logger.info("2 : {} time :{}",status,System.currentTimeMillis()-start);
            });
        }

        poolExecutor.shutdown();

        Thread.sleep(sleepTime);
    }


    @Test
    public void dislike() throws InterruptedException {
        new Thread(()->{
            long start = System.currentTimeMillis();
            boolean status = commentService.disLike(commentId1,authorId,videoId2);
            logger.info("1 : {} time :{}",status,System.currentTimeMillis()-start);
        }).start();

        new Thread(()->{
            long start = System.currentTimeMillis();
            boolean status = commentService.disLike(commentId2,authorId2,videoId1);
            logger.info("2 : {} time :{}",status,System.currentTimeMillis()-start);
        }).start();

        Thread.sleep(sleepTime);
    }

    @Test
    public void concurrentDislike() throws InterruptedException {
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(10,150,10, TimeUnit.SECONDS,new ArrayBlockingQueue<>(100));

        for(int i = 0 ;i<50 ;i++){
            poolExecutor.execute(()->{
                long start = System.currentTimeMillis();
                boolean status = commentService.disLike(commentId1,authorId,videoId2);
                logger.info("1 : {} time :{}",status,System.currentTimeMillis()-start);
            });
        }

        for(int i = 0 ;i<50 ;i++){
            poolExecutor.execute(()->{
                long start = System.currentTimeMillis();
                boolean status = commentService.disLike(commentId2,authorId2,videoId1);
                logger.info("2 : {} time :{}",status,System.currentTimeMillis()-start);
            });
        }

        poolExecutor.shutdown();

        Thread.sleep(sleepTime);

    }


    @Test
    public void getCommentsByVideoId() throws InterruptedException {

        List<CommentResponse> responses1= commentService.getCommentsByVideoId(videoId1,0,null);
        List<CommentResponse> responses2= commentService.getCommentsByVideoId(videoId1,0,authorId);
        List<CommentResponse> responses3= commentService.getCommentsByVideoId(videoId1,0,authorId2);

        List<CommentResponse> responses4 =commentService.getCommentsByVideoId(videoId1,responses1.size(),null);
        List<CommentResponse> responses5 =commentService.getCommentsByVideoId(videoId1,responses2.size(),authorId);
        List<CommentResponse> responses6 =commentService.getCommentsByVideoId(videoId1,responses3.size(),authorId2);

        Thread.sleep(sleepTime);
    }

    @Test
    public void currentGetCommentsByVideoId() throws InterruptedException {
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(10,150,10, TimeUnit.SECONDS,new ArrayBlockingQueue<>(100));


        for(int i = 0;i<50;i++){
            int finalI = i;
            poolExecutor.execute(()->{
                int offset = 0;
                if(finalI>=10) offset = 10;
                if (finalI>=20) offset=20;
                if(finalI>=30) offset=30;
                try {
                    long start = System.currentTimeMillis();

                    List<CommentResponse> commentResponseList = commentService.getCommentsByVideoId(videoId1,offset,null);
                    logger.info("size : {} , time : {}, find : {}",offset,System.currentTimeMillis()-start,commentResponseList.size());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        for(int i = 0;i<50;i++){
            int finalI = i;
            poolExecutor.execute(()->{
                int offset = 0;
                if(finalI>=10) offset = 10;
                if (finalI>=20) offset=20;
                if(finalI>=30) offset=30;
                try {
                    long start = System.currentTimeMillis();
                    List<CommentResponse> commentResponseList = commentService.getCommentsByVideoId(videoId1,offset,authorId);
                    logger.info("size : {} , time : {}, find : {}",offset,System.currentTimeMillis()-start,commentResponseList.size());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        poolExecutor.shutdown();
        Thread.sleep(sleepTime);
    }


    String  rootId1 ="0e955a7bc4704edea4eabb1e009146bd";
    String rootId2 = "0fe3efd7dd064d06999d96a34bd1058c";




    @Test
    public void concurrentGetReplyByVideoId() throws InterruptedException {
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(10,50,10, TimeUnit.SECONDS,new ArrayBlockingQueue<>(100));
        for(int i = 0;i<81;i++){
            int finalI = i;
            poolExecutor.execute(()->{
                int offset = 0;
                if(finalI>=10) offset = 10;
                if (finalI>=20) offset=20;
                if(finalI>=30) offset=30;
                if(finalI>=40) offset=40;
                if(finalI == 50) offset=50;
                if(finalI == 60) offset=60;
                if(finalI == 70) offset=70;
                if(finalI == 80) offset=80;
                try {
                    long start = System.currentTimeMillis();

                    List<CommentResponse> commentResponseList = commentService.getReplyByVideoId(videoId2,commentId1,null,offset);
                    logger.info("1  size : {} , time : {}, find : {}",offset,System.currentTimeMillis()-start,commentResponseList!=null ? commentResponseList.size() : null);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        poolExecutor.shutdown();

        Thread.sleep(sleepTime);
    }


    @Test
    public void gets() throws InterruptedException {
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(10,50,10, TimeUnit.SECONDS,new ArrayBlockingQueue<>(200));
        for(int i = 0;i<50;i++){
            int finalI = i;
            poolExecutor.execute(()->{
                int offset = 0;
                if(finalI>=10) offset = 10;
                if (finalI>=20) offset=20;
                if(finalI>=30) offset=30;
                try {
                    long start = System.currentTimeMillis();

                    List<CommentResponse> commentResponseList = commentService.getCommentByVideoWithScore(videoId1,null,offset);
                    logger.info("1 size : {} , time : {}, find : {}",offset,System.currentTimeMillis()-start,commentResponseList!=null ? commentResponseList.size() : null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        for(int i = 0;i<150;i++){
            int finalI = i;
            poolExecutor.execute(()->{
                int offset = 0;
                if(finalI>=10) offset = 10;
                if (finalI>=20) offset=20;
                if(finalI>=30) offset=30;
                if(finalI>=40) offset=40;
                if(finalI>=50) offset =50;
                try {
                    long start = System.currentTimeMillis();

                    List<CommentResponse> commentResponseList = commentService.getCommentByVideoWithScore(videoId2,authorId,offset);
                    logger.info("2 size : {} , time : {}, find : {}",offset,System.currentTimeMillis()-start,commentResponseList!=null ? commentResponseList.size() : null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        poolExecutor.shutdown();

        Thread.sleep(sleepTime);
    }
}
