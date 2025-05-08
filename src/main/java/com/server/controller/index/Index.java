package com.server.controller.index;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping
@Tag(name = "测试页面",description = "用来测试一些功能")
public class Index {
    @GetMapping
    public long index(String a){
        return System.currentTimeMillis();
    }
}
