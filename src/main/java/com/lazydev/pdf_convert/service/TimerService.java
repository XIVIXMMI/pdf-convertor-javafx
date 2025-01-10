package com.lazydev.pdf_convert.service;

import javafx.application.Platform;
import javafx.scene.control.Label;

import java.util.Timer;
import java.util.TimerTask;

public class TimerService {

    private final Label timerLabel;
    private Timer timer;
    private int claspedSeconds; // Đếm số giây đã trôi qua

    public TimerService(Label timerLabel) {
        this.timerLabel = timerLabel;
        this.claspedSeconds = 0;
    }

    public void timerStart() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                claspedSeconds++;
                updateTimerLabel();
            }
        },0,1000); // cứ mỗi 1000ms (1s) run method sẽ được thực thi
    }

    public void stopTimer() {
        if(timer != null){
            timer.cancel();
        }
    }

    public void resetTimer() {
        stopTimer();
        claspedSeconds = 0;
        updateTimerLabel();
    }

    private void updateTimerLabel() {
        Platform.runLater(() -> timerLabel.setText("Elapsed:"+ claspedSeconds +"s"));
    }

}
