package com.server.stats;

public interface StatsTest {
    void videoViewTest();
    void videoGetVideoStatsTest() throws InterruptedException;
    void videoLikeTest() throws InterruptedException;
    void videoCoinTest();
    void videoFavTest();
    void videoShareTest();
    void videoBarrageTest();

    void userGetStatsTest();
    void userVideoTest();
    void userLikeTest();
    void userFollowingTest();
    void userFollowerTest();
    void userCoinTest();
    void userFavTest();
}
