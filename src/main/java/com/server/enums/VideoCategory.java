package com.server.enums;

import com.server.exception.ApiException;

public enum VideoCategory {
    ANIMATION("动画", "动画、番剧、国创等内容"),
    MUSIC("音乐", "原创音乐、翻唱等"),
    DANCE("舞蹈", "宅舞、街舞、国风舞蹈等"),
    GAME("游戏", "游戏实况、攻略、电竞赛事等"),
    KNOWLEDGE("知识", "科普、人文历史、科技数码等"),
    TECHNOLOGY("科技", "编程、极客、技术教程等"),
    SPORTS("运动", "体育赛事、健身、户外运动等"),
    LIFE("生活", "日常、美食、旅行、搞笑等"),
    FOOD("美食", "美食制作、探店、吃播等"),
    FASHION("时尚", "美妆、穿搭、潮流等"),
    ENTERTAINMENT("娱乐", "综艺、明星、影视杂谈等"),
    MOVIE("影视", "电影、电视剧、影视剪辑等"),
    ORIGINAL("原创", "UP主原创视频"),
    DOCUMENTARY("纪录片", "人文自然、历史纪录等"),
    FAN_SUBTITLE("鬼畜", "鬼畜调教、音MAD等"),
    CAR("汽车", "汽车评测、改装、赛事等"),
    VLOG("VLOG", "个人生活记录、旅行日志等");

    private final String name;
    private final String description;

    public static String[] categoryName(){
        String[] names = new String[values().length];
        for(int i=0;i<values().length;i++){
            names[i]=values()[i].getName();
        }
        return names;
    }

    VideoCategory(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public static boolean isVailName(String name){
        for(VideoCategory category : values()){
            if(category.getName().equals(name)){
                return true;
            }
        }
        return false;
    }

    public String getDescription() {
        return description;
    }

    public static VideoCategory fromName(String name) {
        if (name == null) return null;
        for (VideoCategory category : values()) {
            if (category.name.replaceAll("\\s+", "")
                    .equalsIgnoreCase(name.replaceAll("\\s+", ""))) {
                return category;
            }
        }
        throw new ApiException(ErrorCode.BAD_REQUEST);
    }
}