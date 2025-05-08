package com.server.video.ffmpeg;

enum PresetProfile {
    MOBILE(0.5, 0.6, "veryfast", 25),  // 移动端适配
    BALANCE(0.7, 0.8, "fast", 23),     // 平衡模式
    LOW_POWER(0.3, 0.4, "ultrafast", 28); // 低功耗模式

    final double resolutionScale;
    final double bitrateScale;
    final String encodingPreset;
    final int crfValue;

    PresetProfile(double resScale, double bitScale, String preset, int crf) {
        this.resolutionScale = resScale;
        this.bitrateScale = bitScale;
        this.encodingPreset = preset;
        this.crfValue = crf;
    }
}
