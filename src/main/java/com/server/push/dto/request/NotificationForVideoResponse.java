package com.server.push.dto.request;


import com.server.dto.response.video.VideoDataResponse;
import com.server.push.dto.response.HistoryNotificationResponse;
import java.util.List;

public class NotificationForVideoResponse {
    protected VideoDataResponse videoDataResponse;
    protected List<HistoryNotificationResponse<Integer>> notificationWithUser;

    public NotificationForVideoResponse(){}

    public NotificationForVideoResponse(VideoDataResponse videoDataResponse){
        this.videoDataResponse=videoDataResponse;
    }

    public VideoDataResponse getVideoDataResponse() {
        return videoDataResponse;
    }

    public List<HistoryNotificationResponse<Integer>> getNotificationWithUser() {
        return notificationWithUser;
    }

    public void setVideoDataResponse(VideoDataResponse videoDataResponse) {
        this.videoDataResponse = videoDataResponse;
    }

    public void setNotificationWithUser(List<HistoryNotificationResponse<Integer>> notificationWithUser) {
        this.notificationWithUser = notificationWithUser;
    }
}
