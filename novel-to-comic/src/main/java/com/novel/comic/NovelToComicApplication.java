package com.novel.comic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NovelToComicApplication {
    public static void main(String[] args) {
        SpringApplication.run(NovelToComicApplication.class, args);
    }
}