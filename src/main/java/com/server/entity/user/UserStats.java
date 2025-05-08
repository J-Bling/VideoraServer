package com.server.entity.user;

import lombok.Data;

@Data
public class UserStats {
    protected Integer user_id;
    protected int video_count;//发布视频数
    protected int like_count;//获赞数
    protected int following_count;//关注数
    protected int follower_count;//粉丝数
    protected int coin_balance;//硬币剩余
    protected int favorite_count;

    public UserStats(){}
    public UserStats(Integer userId){
        this.user_id=userId;
        this.video_count=0;
        this.like_count=0;
        this.follower_count=0;
        this.following_count=0;
        this.coin_balance=0;
    }
    public UserStats(Integer user_id,String video_count,
                     Object like_count,Object following_count,
                     Object follower_count,Object coin_balance,
                     Object favorite_count){
        this.user_id=user_id;
        this.video_count=Integer.parseInt(video_count);
        this.like_count=Integer.parseInt(like_count.toString());
        this.following_count=Integer.parseInt(following_count.toString());
        this.follower_count=Integer.parseInt(follower_count.toString());
        this.coin_balance= Integer.parseInt(coin_balance.toString());
        this.favorite_count=Integer.parseInt(favorite_count.toString());
    }
}
