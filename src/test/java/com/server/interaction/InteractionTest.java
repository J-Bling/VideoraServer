package com.server.interaction;

import com.server.entity.user.UserRelation;
import com.server.service.interaction.InteractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class InteractionTest {

    @Autowired private InteractionService interactionService;

    @Test
    public void handleUsersRelationsTest(){
        for(int i=0;i<10;i++){
            new Thread(()->{
                try {
                    interactionService.handleUserRelation(new UserRelation(1,2,null));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }


        while (1==1){

        }
    }

    @Test
    public void handleLikeForVideoTest(){
        for(int i=0;i<10;i++){
            new Thread(()->{
                try {
                    interactionService.handleLikeForVideo(1,1,2,0);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            new Thread(()->{
                try {
                    interactionService.handleLikeForVideo(1,2,2,0);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            new Thread(()->{
                try {
                    interactionService.handleLikeForVideo(2,1,2,1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            new Thread(()->{
                try {
                    interactionService.handleLikeForVideo(2,2,2,1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }


        while (1==1){

        }
    }

    @Test
    public void handleFavFoeVideoTest(){
        for(int i=0;i<10;i++){
            new Thread(()->{
                try {
                    interactionService.handleFavFoeVideo(1,1,1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            new Thread(()->{
                try {
                    interactionService.handleFavFoeVideo(1,2,1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            new Thread(()->{
                try {
                    interactionService.handleFavFoeVideo(2,1,0);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            new Thread(()->{
                try {
                    interactionService.handleFavFoeVideo(2,2,0);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }


        while (1==1){

        }
    }

    @Test
    public void handleCoinForVideoTest(){
        Thread t1=new Thread(()->{
            try {
                interactionService.handleCoinForVideo(1,1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Thread t2=new Thread(()->{
            try {
                interactionService.handleCoinForVideo(1,2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Thread t3=new Thread(()->{
            try {
                interactionService.handleCoinForVideo(2,1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Thread t4=new Thread(()->{
            try {
                interactionService.handleCoinForVideo(2,2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        new Thread(()->{
            try {
                interactionService.handleCoinForVideo(2,3);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        new Thread(()->{
            try {
                interactionService.handleCoinForVideo(1,3);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();


        t1.start();
        t2.start();
        t3.start();
        t4.start();

        while (true){

        }
    }
}
