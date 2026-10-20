package com.medibed.tracker;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedibedTrackerApplication {

    public static void main(String[] args) {
        // Shift times, appointment slots and "available now" are all Mumbai local time.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(MedibedTrackerApplication.class, args);
    }
}
