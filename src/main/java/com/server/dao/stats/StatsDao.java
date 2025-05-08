package com.server.dao.stats;

import com.server.entity.cache.stats.StatsUpdateTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StatsDao {
    // 批量更新用户统计
    void batchUpdateUserStats(@Param("column") String column,
                              @Param("tasks") List<StatsUpdateTask> tasks);

    // 批量更新视频统计
    void batchUpdateVideoStats(@Param("column") String column,
                               @Param("tasks") List<StatsUpdateTask> tasks);
}
