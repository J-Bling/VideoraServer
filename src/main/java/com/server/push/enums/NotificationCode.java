package com.server.push.enums;

public enum NotificationCode {
    SYSTEM(0,"系统通知"),
    LIKED_FOR_VIDEO(1,"点赞你的视频"),
    LIKED_FOR_COMMENT(2,"点赞你的评论"),
    REPLY_FOR_COMMENT(3,"回复你的评论"),
    FOLLOWED_FOR(4,"关注了你"),
    PRIVATE_LETTER_FOR(5,"私信了你"),
    DYNAMIC_PUBLISH_FOR(6,"你订阅的频道有新的动态"),
    SERVER_BULLETIN(7,"服务器公告");
    private final Integer code;
    private final String description;

    NotificationCode(Integer code,String description){
        this.code=code;this.description=description;
    }

    public static NotificationCode fromCode(int code){
        for(NotificationCode notificationCode : values()){
            if(notificationCode.code==code) return notificationCode;
        }
        throw new RuntimeException("参数错误");
    }

    public static Integer[] getCodes(){
        Integer[] codes= new Integer[values().length];
        int i =0;
        for(NotificationCode notificationCode : values()){
            codes[i++]= notificationCode.code;
        }
        return codes;
    }

    public static boolean isVailCode(Integer code){
        for(NotificationCode notificationCode : values()){
            if(notificationCode.code.equals(code)) return true;
        }
        return false;
    }

    public String getDescription() {
        return description;
    }

    public Integer getCode() {
        return code;
    }
}
