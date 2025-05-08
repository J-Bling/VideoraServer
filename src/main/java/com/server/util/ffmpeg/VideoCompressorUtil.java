package com.server.util.ffmpeg;

import com.server.enums.PresetProfile;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class VideoCompressorUtil {
    private static final Logger logger = LoggerFactory.getLogger(VideoCompressorUtil.class);
    static {
        avutil.av_log_set_level(avutil.AV_LOG_QUIET); // 完全静默
    }

    public boolean smartCompress(String inputPath, String outputPath, PresetProfile profile) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputPath)) {
            grabber.start();

            int targetWidth = alignEven((int)(grabber.getImageWidth() * profile.resolutionScale));
            int targetHeight = alignEven((int)(grabber.getImageHeight() * profile.resolutionScale));

            int originalBitrate = grabber.getVideoBitrate(); // 单位：bps
            int targetBitrate = (int) Math.min(
                    originalBitrate * profile.bitrateScale,
                    calculateRecommendedBitrate(targetWidth, targetHeight, grabber.getFrameRate())
            );

            return transcodeWithBiliStrategy(grabber, outputPath,
                    targetWidth, targetHeight, targetBitrate, profile);
        } catch (Exception e) {
            logger.error("智能压缩失败: {}", inputPath, e);
            return false;
        }
    }

    private boolean transcodeWithBiliStrategy(FFmpegFrameGrabber grabber, String outputPath,
                                              int width, int height, int bitrate, PresetProfile profile) {
        try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputPath, width, height)) {
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFormat("mp4");
            recorder.setFrameRate(grabber.getFrameRate());
            recorder.setVideoBitrate(bitrate);

            recorder.setVideoOption("preset", profile.encodingPreset);
            recorder.setVideoOption("crf", String.valueOf(profile.crfValue));
            recorder.setVideoOption("movflags", "+faststart");
            recorder.setVideoOption("keyint", String.valueOf(2 * (int)grabber.getFrameRate()));
            recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);

            if (grabber.getAudioChannels() > 0) {
                recorder.setAudioChannels(Math.min(2, grabber.getAudioChannels()));
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                recorder.setAudioBitrate(Math.min(128000, grabber.getAudioBitrate()));
                recorder.setSampleRate(44100);
            }

            recorder.start();

            AtomicInteger frameCounter = new AtomicInteger(0);
            ExecutorService executor = Executors.newSingleThreadExecutor();

            Future<?> task = executor.submit(() -> {
                Frame frame;
                try {
                    while ((frame = grabber.grab()) != null) {
                        recorder.record(frame);
                        frameCounter.incrementAndGet();
                    }
                } catch (Exception e) {
                    logger.error("帧处理异常", e);
                }
            });

            while (!task.isDone()) {
                double load = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
                if (load > 1.8) {
                    Thread.sleep(500);
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("转码过程异常", e);
        }
        return false;
    }

    private int alignEven(int value) {
        return value % 2 == 0 ? value : value - 1;
    }

    private int calculateRecommendedBitrate(int width, int height, double fps) {
        double pixelCount = width * height;
        double fpsFactor = Math.sqrt(fps / 30.0);
        return (int) (pixelCount * 0.07 * fpsFactor); // 经验公式
    }
}
