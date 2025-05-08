package com.server.notification;

import com.server.push.dto.request.NotificationForComment;
import com.server.push.dto.request.NotificationForVideoResponse;
import com.server.push.dto.response.HistoryNotificationResponse;
import com.server.push.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class NotificationServiceTest {
    @Autowired private NotificationService notificationService;

    private final ThreadPoolExecutor poolExecutor =
            new ThreadPoolExecutor(5,10,2, TimeUnit.SECONDS,new ArrayBlockingQueue<>(5));


    @Test
    public void test() throws InterruptedException {

        poolExecutor.execute(()->{
            long start = System.currentTimeMillis();
            try {
                List<NotificationForVideoResponse>  responses=
                        notificationService.getLikeVideoNotification(100020,0);
//                for(NotificationForVideoResponse response : responses){
//                    System.out.println(response.getVideoDataResponse());
//                    for(HistoryNotificationResponse<Integer> historyNotificationResponses : response.getNotificationWithUser()){
//                        System.out.println(historyNotificationResponses);
//                    }
//                    System.out.println("\n\n");
//                }
                System.out.println("likeVideo: "+responses.get(0).getNotificationWithUser().size());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }catch (Exception e){
                System.out.println(e.getMessage());
            }
            finally {
                System.out.println(Thread.currentThread().getName()+"用时 :"+(System.currentTimeMillis()-start));
            }
        });

//        poolExecutor.execute(()->{
//            long start = System.currentTimeMillis();
//            try {
//                List<NotificationForVideoResponse>  responses=
//                        notificationService.getLikeVideoNotification(100020,60);
//                for(NotificationForVideoResponse response : responses){
//                    System.out.println(response.getVideoDataResponse());
//                    for(HistoryNotificationResponse<Integer> historyNotificationResponses : response.getNotificationWithUser()){
//                        System.out.println(historyNotificationResponses);
//                    }
//                    System.out.println("\n\n");
//                }
//                System.out.println(responses.get(0).getNotificationWithUser().size());
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }catch (Exception e){
//                System.out.println(e.getMessage());
//            }finally {
//                System.out.println(Thread.currentThread().getName()+"用时 :"+(System.currentTimeMillis()-start));
//            }
//        });


//        poolExecutor.execute(()->{
//            long start = System.currentTimeMillis();
//            try {
//                List<NotificationForVideoResponse>  responses=
//                        notificationService.getLikeVideoNotification(100020,0);
//                for(NotificationForVideoResponse response : responses){
//                    System.out.println(response.getVideoDataResponse());
//                    for(HistoryNotificationResponse<Integer> historyNotificationResponses : response.getNotificationWithUser()){
//                        System.out.println(historyNotificationResponses);
//                    }
//                    System.out.println("\n\n");
//                }
//                System.out.println(responses.get(0).getNotificationWithUser().size());
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }catch (Exception e){
//                System.out.println(e.getMessage());
//            }finally {
//                System.out.println(Thread.currentThread().getName()+"用时 :"+(System.currentTimeMillis()-start));
//            }
//        });

        poolExecutor.execute(()->{
            long start = System.currentTimeMillis();
            try {

                List<NotificationForComment>  responses=
                        notificationService.getLikeCommentNotification(100020,30);
//                for(NotificationForVideoResponse response : responses){
//                    System.out.println(response.getVideoDataResponse());
//                    for(HistoryNotificationResponse<Integer> historyNotificationResponses : response.getNotificationWithUser()){
//                        System.out.println(historyNotificationResponses);
//                    }
//                    System.out.println("\n\n");
//                }
                System.out.println("likeComment: "+responses.size());
                System.out.println("likeComment: "+responses.get(0).getNotificationWithUser().size());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }catch (Exception e){
                System.out.println(e.getMessage());
            }finally {
                System.out.println(Thread.currentThread().getName()+"用时 :"+(System.currentTimeMillis()-start));
            }
        });


        poolExecutor.execute(()->{
            long start = System.currentTimeMillis();
            try {


                List<HistoryNotificationResponse<Integer>>  responses=
                        notificationService.getFollowNotification(100020,0);
                System.out.println("follow : "+responses.size());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }catch (Exception e){
                System.out.println(e.getMessage());
            }finally {
                System.out.println(Thread.currentThread().getName()+"用时 :"+(System.currentTimeMillis()-start));
            }
        });

        poolExecutor.execute(()->{
            long start = System.currentTimeMillis();
            try {
                List<NotificationForComment>  responses=
                        notificationService.getReplyCommentNotification(100020,0);
                System.out.println("reply : "+responses.size());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }catch (Exception e){
                System.out.println(e.getMessage());
            }finally {
                System.out.println(Thread.currentThread().getName()+"用时 :"+(System.currentTimeMillis()-start));
            }
        });


        poolExecutor.shutdown();
        Thread.sleep(24*60*60*1000);
    }
}
