package com.server.stats;

import com.server.entity.video.VideoStats;
import com.server.service.stats.UserStatsService;
import com.server.service.stats.VideoStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class StatsTestImpl implements StatsTest{

    @Autowired private VideoStatsService videoStatsService;
    @Autowired private UserStatsService userStatsService;

    private static final int CONCURRENT_THREAD_MAX=1000;
    private static final int CONCURRENT_THREAD_MIN=10;

    private static final int[] videoIds=new int[]{1,2,3,4,5,6,7};
    private static final int[] userIds =new int[]{1,2,3,4,5,6,7};


    @Override
    @Test
    public void videoViewTest() {
        for(int i=0;i<CONCURRENT_THREAD_MAX;i++) {
            Thread t = new Thread(() -> {
                videoStatsService.CountView(1, 1);
            });
            t.start();
        }
//            if(i<300){
//                Thread t=new Thread(()->{
//                    videoStatsService.CountView(1,1);
//                });
//                t.start();
//            }
//            if(i>=300 && i<=600){
//                Thread t=new Thread(()->{
//                    videoStatsService.CountView(2,1);
//                });
//                t.start();
//            }
//            if(i>600){
//                Thread t=new Thread(()->{
//                    videoStatsService.CountView(3,1);
//                });
//                t.start();
//            }


        while (true){

        }
    }


    @Override
    @Test
    public void videoGetVideoStatsTest() throws InterruptedException {
        Thread t1=new Thread(()->{
            long start=System.currentTimeMillis();

            try {
                VideoStats videoStats= videoStatsService.getVideoStats(1);


                long end=System.currentTimeMillis();

                System.out.println("线程1测试完毕 用时 ："+(end-start)+" ->测试数据获得 :"+videoStats);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        Thread t2=new Thread(()->{
            long start=System.currentTimeMillis();

            try {
                VideoStats videoStats= videoStatsService.getVideoStats(1);


                long end=System.currentTimeMillis();

                System.out.println("线程2测试完毕 用时 ："+(end-start)+" ->测试数据获得 :"+videoStats);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        Thread t3=new Thread(()->{
            long start=System.currentTimeMillis();

            try {
                VideoStats videoStats= videoStatsService.getVideoStats(1);


                long end=System.currentTimeMillis();

                System.out.println("线程3测试完毕 用时 ："+(end-start)+" ->测试数据获得 :"+videoStats);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        t1.start();
        t2.start();
        t3.start();

        Thread.sleep(24*60*1000);

    }

    @Override
    @Test
    public void videoLikeTest() throws InterruptedException {

        for(int i=0;i<CONCURRENT_THREAD_MAX;i++){
            Thread t1 =new Thread(()->{
                videoStatsService.CountLike(1,1);
                videoStatsService.CountView(1,1);
                videoStatsService.CountView(2,1);
                videoStatsService.CountLike(2,1);
                videoStatsService.CountCoin(1,1);
                videoStatsService.CountCoin(2,1);
                videoStatsService.CountFavorite(1,1);
                videoStatsService.CountFavorite(2,1);
                videoStatsService.CountShare(1,1);
                videoStatsService.CountShare(2,1);
                videoStatsService.CountBarrage(1,1);
                videoStatsService.CountBarrage(2,1);
            });
            t1.start();
        }


        Thread.sleep(24*60*1000);
    }

    @Override
    public void videoCoinTest() {

    }

    @Override
    public void videoFavTest() {

    }

    @Override
    public void videoShareTest() {

    }

    @Override
    public void videoBarrageTest() {

    }

    @Override
    public void userGetStatsTest() {

    }

    @Override
    public void userVideoTest() {

    }

    @Override
    @Test
    public void userLikeTest() {
        for(int i=0;i<CONCURRENT_THREAD_MAX;i++){
            new Thread(()->{
                userStatsService.CountCoin(1,1);
                userStatsService.CountFollowing(1,1);
                userStatsService.CountLike(1,1);
                userStatsService.CountFollower(1,1);
                userStatsService.CountVideo(1,1);
                userStatsService.CountFavorite(1,1);
            }).start();

            new Thread(()->{
                userStatsService.CountCoin(2,1);
                userStatsService.CountFollowing(2,1);
                userStatsService.CountLike(2,1);
                userStatsService.CountFollower(2,1);
                userStatsService.CountVideo(2,1);
                userStatsService.CountFavorite(2,1);
            }).start();
        }

        while (1==1){

        }
    }

    @Override
    public void userFollowingTest() {

    }

    @Override
    public void userFollowerTest() {

    }

    @Override
    public void userCoinTest() {

    }

    @Override
    public void userFavTest() {

    }
}
