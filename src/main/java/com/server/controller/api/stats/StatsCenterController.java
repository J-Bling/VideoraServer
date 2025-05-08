package com.server.controller.api.stats;

import com.server.service.stats.UserStatsService;
import com.server.service.stats.VideoStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats/center")
@Tag(name="数据统计中心")
public class StatsCenterController {

    @Autowired private UserStatsService userStatsService;
    @Autowired private VideoStatsService videoStatsService;


    @GetMapping("/")
    @Operation(summary = "test")
    public long index(){
        return System.currentTimeMillis();
    }

    
}
