package com.server.push.entity;
import lombok.Data;

@Data
public class Notification {
    private String message_id;
    private Integer user_id;
    private Integer target_id;
    private Integer tag_id;
    private String message;
    private Integer type;
    private Boolean is_read;
    private Long created;


    public Notification() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Notification notification;

        private Builder() {
            this.notification = new Notification();
        }

        public Builder messageId(String messageId) {
            notification.message_id = messageId;
            return this;
        }

        public Builder userId(Integer userId) {
            notification.user_id = userId;
            return this;
        }
        public Builder targetId(Integer targetId) {
            notification.target_id = targetId;
            return this;
        }

        public Builder message(String message) {
            notification.message = message;
            return this;
        }

        public Builder type(Integer type) {
            notification.type = type;
            return this;
        }

        public Builder isRead(Boolean isRead) {
            notification.is_read = isRead;
            return this;
        }

        public Builder created(Long created) {
            notification.created = created;
            return this;
        }

        public Builder tagId(Integer tag_id){
            notification.tag_id=tag_id;
            return this;
        }

        public Notification build() {
            if (notification.user_id == null) {
                throw new IllegalArgumentException("userId不能为空");
            }
            return notification;
        }
    }
}
