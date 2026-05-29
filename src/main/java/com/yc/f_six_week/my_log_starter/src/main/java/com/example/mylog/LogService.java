package com.yc.f_six_week.my_log_starter.src.main.java.com.example.mylog;

public class LogService {
    private String prefix;

    public LogService(String prefix) {
        this.prefix = prefix;
    }

    public void log(String message) {
        System.out.println("[" + prefix + "] " + message);
    }
}