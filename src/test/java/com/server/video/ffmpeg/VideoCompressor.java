package com.server.video.ffmpeg;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

// 压缩等级配置（可根据实际需求扩展）
enum CompressionLevel {

    LOW(0.5, 0.6),   // 分辨率降50%，码率60%
    MEDIUM(0.3, 0.4),// 分辨率降70%，码率40%
    HIGH(0.2, 0.3); // 分辨率降80%，码率30%

    final double resolutionScale;
    final double bitrateScale;

    CompressionLevel(double resScale, double bitScale) {
        this.resolutionScale = resScale;
        this.bitrateScale = bitScale;
    }
}

public class VideoCompressor {
    private static final Logger logger = LoggerFactory.getLogger(VideoCompressor.class);
    /**
     * 视频压缩入口方法
     * @param inputPath 输入视频路径
     * @param outputPath 输出视频路径
     * @param level 压缩等级 (LOW/MEDIUM/HIGH)
     * @return 压缩是否成功
     */
    public boolean compressVideo(String inputPath, String outputPath, CompressionLevel level) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputPath)) {
            grabber.start();

            // 动态计算目标参数
            int targetWidth = (int)(grabber.getImageWidth() * level.resolutionScale);
            int targetHeight = (int)(grabber.getImageHeight() * level.resolutionScale);
            int targetBitrate = (int)(grabber.getVideoBitrate() * level.bitrateScale);

            // 对齐到偶数分辨率（H264要求）
            targetWidth = targetWidth % 2 == 0 ? targetWidth : targetWidth - 1;
            targetHeight = targetHeight % 2 == 0 ? targetHeight : targetHeight - 1;

            return transcode(grabber, outputPath, targetWidth, targetHeight, targetBitrate);
        } catch (Exception e) {
            logger.error("视频压缩失败: {}", inputPath, e);
            return false;
        }
    }

    private boolean transcode(FFmpegFrameGrabber grabber, String outputPath,
                              int width, int height, int bitrate) {
        try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputPath, width, height)) {
            // 视频基础配置
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFormat("mp4");
            recorder.setFrameRate(grabber.getFrameRate());
            recorder.setVideoBitrate(bitrate);

            // 低配服务器优化配置
            recorder.setVideoOption("preset", "ultrafast");  // 编码速度优先
            recorder.setVideoOption("tune", "fastdecode");   // 快速解码优化
            recorder.setVideoOption("threads", "1");         // 限制CPU线程数

            // 音频压缩（直接转码为低码率AAC）
            if (grabber.getAudioChannels() > 0) {
                recorder.setAudioChannels(1);
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                recorder.setAudioBitrate(64000); // 64kbps
            }

            recorder.start();

            // 帧处理（带跳过逻辑减轻负载）
            int skipRatio = 2; // 每2帧保留1帧
            double originalFPS = grabber.getFrameRate();
            recorder.setFrameRate(originalFPS / skipRatio); // 输出帧率减半

            Frame frame;
            long frameCount = 0;
            while ((frame = grabber.grab()) != null) {
                if (frameCount++ % skipRatio == 0) {
                    recorder.record(frame);
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("转码过程异常", e);
            new File(outputPath).delete();
            return false;
        }
    }
}
class a{
    public static void main(String[] args) {
        String VIDEO_BASE_URL = "/Users/j/Desktop/video/";
        String inputFile = VIDEO_BASE_URL + "4968451-hd_1280_720_60fps.mp4";
        String output1 = VIDEO_BASE_URL + "output_low.mp4";
        String output2 = VIDEO_BASE_URL + "output_medium.mp4";
        String output3 = VIDEO_BASE_URL + "output_high.mp4";
        long start1=System.currentTimeMillis();
        new VideoCompressor().compressVideo(inputFile,output1,CompressionLevel.LOW);
        System.out.println("完成LOW "+(System.currentTimeMillis()-start1));
        start1=System.currentTimeMillis();
        new VideoCompressor().compressVideo(inputFile,output2,CompressionLevel.MEDIUM);
        System.out.println("完成MID "+(System.currentTimeMillis()-start1));
        start1=System.currentTimeMillis();
        new VideoCompressor().compressVideo(inputFile,output3,CompressionLevel.HIGH);
        System.out.println("完成HIG "+(System.currentTimeMillis()-start1));
    }
}