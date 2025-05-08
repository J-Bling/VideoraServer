package com.server.video.ffmpeg;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

class VideoFormat_ {
    public final int w;
    public final int h;
    public final int bitrate; // kbps
    public final int frameRate;

    public VideoFormat_(int w, int h, int bitrate, int frameRate) {
        this.w = w;
        this.h = h;
        this.bitrate = bitrate;
        this.frameRate = frameRate;
    }
}

public class TranscodingTest {

    private final Logger logger = LoggerFactory.getLogger(TranscodingTest.class);
    private static final String VIDEO_BASE_URL = "/Users/j/Desktop/video/";
    private static final VideoFormat_ LOW_RATE = new VideoFormat_(854, 480, 800, 30);  // 480p
    private static final VideoFormat_ HEIGHT_RATE = new VideoFormat_(1920, 1080, 2000, 30); // 1080p

    private long transcodeVideo(String inputPath, String outputPath, boolean isHighQuality) {
        long start = System.currentTimeMillis();
        File file = new File(inputPath);
        if (!file.exists()) {
            logger.error("输入文件不存在: {}", inputPath);
            return 0;
        }

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputPath)) {
            grabber.start();

            VideoFormat_ format = isHighQuality ? HEIGHT_RATE : LOW_RATE;

            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputPath, format.w, format.h)) {
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.setFormat("mp4");
                recorder.setFrameRate(format.frameRate);

                // 动态码率控制：取原视频码率与预设码率的较小值
                int originalBitrate = grabber.getVideoBitrate(); // 单位：bps
                int targetBitrate = Math.min(originalBitrate, format.bitrate * 1000);
                recorder.setVideoBitrate(targetBitrate);

                // 提高编码效率参数
                recorder.setVideoOption("preset", "slow"); // 编码速度换压缩率
                recorder.setVideoOption("profile", "main"); // 兼容性平衡
                recorder.setVideoOption("tune", "film");   // 根据内容类型优化

                recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
                recorder.setVideoQuality(0); // 避免与码率控制冲突

                // 音频压缩配置
                if (grabber.getAudioChannels() > 0) {
                    recorder.setAudioChannels(1); // 单声道节省空间
                    recorder.setSampleRate(44100); // 统一采样率
                    recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                    recorder.setAudioBitrate(96000); // 96kbps
                }

                recorder.start();

                Frame frame;
                while ((frame = grabber.grab()) != null) {
                    if (frame.image != null || frame.samples != null) {
                        recorder.record(frame);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("视频转码失败: {} -> {}", inputPath, outputPath, e);
            new File(outputPath).delete();
            return -1;
        }

        return System.currentTimeMillis() - start;
    }



    @Test
    public void test(){
        String inputFile = VIDEO_BASE_URL + "4968451-hd_1280_720_60fps.mp4";
        String outputHD = VIDEO_BASE_URL + "output_hd.mp4";
        String outputSD = VIDEO_BASE_URL + "output_sd.mp4";

        try {
            System.out.println("高清转码用时: " + transcodeVideo(inputFile, outputHD, true) + "ms");
            System.out.println("标清转码用时: " + transcodeVideo(inputFile, outputSD, false) + "ms");
        } catch (Exception e) {
            logger.error("转码测试失败", e);
        }
    }

}
