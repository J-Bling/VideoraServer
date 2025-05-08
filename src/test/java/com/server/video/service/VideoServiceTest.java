package com.server.video.service;
import com.server.dto.request.auth.AuthRequest;
import com.server.dto.request.video.VideoUploadRequest;
import com.server.dto.response.video.VideoDataResponse;
import com.server.entity.video.Video;
import com.server.entity.video.VideoClip;
import com.server.enums.VideoCategory;
import com.server.service.userservice.UserDataService;
import com.server.service.userservice.UserService;
import com.server.service.videoservice.VideoEditService;
import com.server.service.videoservice.VideoService;
import com.server.util.JwtUtil;
import com.server.util.PasswordUtil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.util.List;


@SpringBootTest
public class VideoServiceTest {
    @Autowired private VideoService videoService;
    @Autowired private VideoEditService videoEditService;
    @Autowired private UserDataService userDataService;
    @Autowired private UserService userService;


    private final Logger logger= LoggerFactory.getLogger(VideoServiceTest.class);

    @Test
    public void createUserTest() throws InterruptedException {
        new Thread(()->{
            long start=System.currentTimeMillis();
            try {
                String nickname = PasswordUtil.generateSalt();
                userService.createUser(new AuthRequest(nickname, null, "1@2.com", "12345678"));
            }catch (Exception e){
                logger.error("发生错误 : {}",e.getMessage(),e);
            }

            System.out.println("用时"+(System.currentTimeMillis()-start));
        }).start();

        new Thread(()->{
            long start=System.currentTimeMillis();
            try {
                String nickname = PasswordUtil.generateSalt();
                userService.createUser(new AuthRequest(nickname, "13232112651", null, "12345678"));
            }catch (Exception e){
                logger.error("发生错误 : {}",e.getMessage(),e);
            }

            System.out.println("用时"+(System.currentTimeMillis()-start));
        }).start();


        Thread.sleep(24*60*60*1000);
    }

    @Test
    public void loginTest() throws InterruptedException {
        new Thread(()->{
            long start=System.currentTimeMillis();
            try {
                System.out.println(userService.loginByPass(new AuthRequest("13232112651",null,"12345678")));
            }catch (Exception e){
                logger.error("发生错误 : {}",e.getMessage(),e);
            }

            System.out.println("用时"+(System.currentTimeMillis()-start));
        }).start();

        new Thread(()->{
            long start=System.currentTimeMillis();
            try {
                System.out.println(userService.loginByPass(new AuthRequest(null,"1@2.com","12345678")));
            }catch (Exception e){
                logger.error("发生错误 : {}",e.getMessage(),e);
            }

            System.out.println("用时"+(System.currentTimeMillis()-start));
        }).start();

        new Thread(()->{
            long start=System.currentTimeMillis();
            try {
                System.out.println(userService.loginByPass(new AuthRequest("123123123",null,"12345678")));
            }catch (Exception e){
                logger.error("发生错误 : {}",e.getMessage(),e);
            }

            System.out.println("3 用时"+(System.currentTimeMillis()-start));
        }).start();

        new Thread(()->{
            long start=System.currentTimeMillis();
            try {
                System.out.println(userService.loginByPass(new AuthRequest(null,"1@3.com","12345678")));
            }catch (Exception e){
                logger.error("发生错误 : {}",e.getMessage(),e);
            }

            System.out.println("4 用时"+(System.currentTimeMillis()-start));
        }).start();

        Thread.sleep(24*60*60*1000);
    }

    @Test
    public void jwtTest(){
        String sub=JwtUtil.validateAndGetToken
                ("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDAwMjAiLCJleHAiOjE3NDUzMTQ2MjMsImlhdCI6MTc0NTA1NTQyM30.BZfBI-BfVSr_OZGMdMcnCNCLGRvSmVKIJr0wNz2xOY4");
        System.out.println(sub);
    }

    private void createVideo(String videoUrl,String imageUrl){
        try (FFmpegFrameGrabber grabber =
                     new FFmpegFrameGrabber(videoUrl)) {
            grabber.start();
            File file = new File(imageUrl);
            File file1=new File(videoUrl);
            double size = file1.length() / (1024.0 * 1024.0);
            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();
            long time= grabber.getLengthInTime();
            int duration = (int)(time  / 1000000);
            double rate = grabber.getVideoFrameRate();
            double clips_count = (double) duration /20;
            int count = (int) Math.ceil(clips_count);

            VideoUploadRequest request = new VideoUploadRequest("testTitle", "description",
                    VideoCategory.ANIMATION.getName(), size, duration, width, height, "mp4", count);

            Video video=request.toEntity();
            video.setAuthor(100020);
            try(FileInputStream inputStream=new FileInputStream(file)) {
                Integer videoId = videoEditService.createVideoInit(video, inputStream,file.getName());
                System.out.println(videoId);
            }

        } catch (Exception e) {
            logger.error("发送错误 ",e);
        }
    }

    @Test
    public void createVideoTest() throws InterruptedException {
        new Thread(()->{
            createVideo("/Users/j/Desktop/video/4968451-hd_1280_720_60fps.mp4",
                    "/Users/j/Desktop/img/OIP-C.7GLMYPqMlt2LgkbPsOnDIAAAAA.jpeg");
        }).start();


        Thread.sleep(24*60*60*1000);
    }

    private void uploadVideoClip(String filename,Integer videoId){
        long start =System.currentTimeMillis();
        try {
            File file = new File(filename);
            if (!file.exists()) return;
            FileInputStream inputStream =new FileInputStream(file);
            FFmpegFrameGrabber grabber =new FFmpegFrameGrabber(file);
            grabber.start();
            double duration = (double) grabber.getLengthInTime()/1000000;
            grabber.close();
            videoEditService.saveVideoClip(new VideoClip(videoId,1,duration),inputStream,filename);
            inputStream.close();
        } catch (Exception e) {
            logger.error("视频分割失败：{}", e.getMessage(), e);
        }finally {
            System.out.println("用时 : "+(System.currentTimeMillis()-start));
        }

    }

    @Test
    public void createVideoClipsTest() throws InterruptedException {
        new Thread(()->{
            uploadVideoClip("/Users/j/Desktop/video/4968451-hd_1280_720_60fps.mp4",9);
        }).start();
        new Thread(()->{
            uploadVideoClip("/Users/j/Desktop/video/20613565-hd_1280_720_24fps.mp4",8);
        }).start();
        Thread.sleep(24*60*60*1000);
    }

    @Test
    public void videoRecommendationsByRandomTest() throws InterruptedException {
        Thread t1= new Thread(()->{
            try {
                long start = System.currentTimeMillis();
                List<VideoDataResponse> data= videoService.videoRecommendationsByRandom(0);
                System.out.println(data);
                System.out.println("用时 : "+(System.currentTimeMillis()-start));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Thread t2= new Thread(()->{
            try {
                long start = System.currentTimeMillis();
                System.out.println(videoService.videoRecommendationsByRandom(0));
                System.out.println("用时 : "+(System.currentTimeMillis()-start));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        t1.start();
        t2.start();

        Thread.sleep(24*60*60*1000);
    }

    @Test
    public void videoRecommendationsByCategoryTest() throws InterruptedException {
        Thread t1= new Thread(()->{
            try {
                long start = System.currentTimeMillis();
                List<VideoDataResponse> data= videoService.videoRecommendationsByCategory(VideoCategory.ANIMATION,0);
                System.out.println(data);
                System.out.println("用时 : "+(System.currentTimeMillis()-start));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Thread t2= new Thread(()->{
            try {
                long start = System.currentTimeMillis();
                System.out.println(videoService.videoRecommendationsByCategory(VideoCategory.ANIMATION,0));
                System.out.println("用时 : "+(System.currentTimeMillis()-start));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        t1.start();
        t2.start();

        Thread.sleep(24*60*60*1000);
    }

    @Test
    public void getVideoClipUrlTest() throws InterruptedException {
        new Thread(()->{
            long start = System.currentTimeMillis();
            System.out.println(videoService.getVideoClipUrl(9,0,true));
            System.out.println("用时 : "+(System.currentTimeMillis()-start));
        }).start();

        new Thread(()->{
            long start = System.currentTimeMillis();
            System.out.println(videoService.getVideoClipUrl(8,0,true));
            System.out.println("用时 : "+(System.currentTimeMillis()-start));
        }).start();

        new Thread(()->{
            long start = System.currentTimeMillis();
            System.out.println(videoService.getVideoClipUrl(8,0,false));
            System.out.println("用时 : "+(System.currentTimeMillis()-start));
        }).start();

        new Thread(()->{
            long start = System.currentTimeMillis();
            System.out.println(videoService.getVideoClipUrl(9,0,false));
            System.out.println("用时 : "+(System.currentTimeMillis()-start));
        }).start();

        Thread.sleep(24*60*60*1000);
    }

    @Test
    public void getVideoResponseDataTest() throws InterruptedException {
        new Thread(()->{
            try {
                long start = System.currentTimeMillis();
                System.out.println(videoService.getVideoResponseData(8,100020));
                System.out.println("用时 : "+(System.currentTimeMillis()-start));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        new Thread(()->{
            try {
                long start = System.currentTimeMillis();
                System.out.println(videoService.getVideoResponseData(9,100020));
                System.out.println("用时 : "+(System.currentTimeMillis()-start));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        new Thread(()->{
            try {
                long start = System.currentTimeMillis();
                System.out.println(videoService.getVideoResponseData(8,100019));
                System.out.println("用时 : "+(System.currentTimeMillis()-start));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        new Thread(()->{
            try {
                long start = System.currentTimeMillis();
                System.out.println(videoService.getVideoResponseData(9,100019));
                System.out.println("用时 : "+(System.currentTimeMillis()-start));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        Thread.sleep(24*60*60*1000);
    }

    @Test
    public void vailJwt() throws InterruptedException {
        String data=JwtUtil.validateAndGetToken("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDAwMjAiLCJleHAiOjE3NDU2NzUxMTksImlhdCI6MTc0NTQxNTkxOX0.X8iVTdczASND5GSHVNR-qhr4-PcIJxRJCVSgHde6_cg");
        System.out.println(data);
        System.out.println("ok");
        Thread.sleep(24*60*60*1000);
    }

    @Test
    public void createJwt() throws InterruptedException {
        String token=JwtUtil.generateToken(100019,System.currentTimeMillis()+1000000);
        System.out.println(token);
        String result=JwtUtil.validateAndGetToken(token);
        System.out.println(result);
        Thread.sleep(24*60*60*1000);
    }
}
