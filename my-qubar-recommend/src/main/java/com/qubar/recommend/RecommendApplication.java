package com.qubar.recommend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RecommendApplication {

    public static void main(String[] args) {

        SpringApplication.run(RecommendApplication.class, args);
        System.out.println("====================系统启动成功====================");
    }
}
