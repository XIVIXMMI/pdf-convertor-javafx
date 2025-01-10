package com.lazydev.pdf_convert.service;

import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConversionService {

    private final ProgressBar progressBar;
    private final Label progressLable;
    private final List<File> filesToConvert;
    private boolean isCancelled;

    public ConversionService(ProgressBar progressBar, Label progressLable){
        this.progressBar = progressBar;
        this.progressLable = progressLable;
        this.filesToConvert = new ArrayList<>();
        this.isCancelled = false;
    }

    public void addFiles(List<File> files){
        filesToConvert.addAll(files);
        updateProgressLabel("Added "+ files.size() + " files.");
    }

    public void addFolder(File folder){
        File[] files = folder.listFiles(((dir, name) -> name.endsWith(".pdf")));
        if(files != null){
            addFiles(List.of(files));
        }
    }

    public void startConversion() {
        Task<Void> conversionTask = new Task<>() {
            @Override
            protected Void call() {
                for(int i=0; i <filesToConvert.size(); i++){
                    if (isCancelled) break;
                    File file = filesToConvert.get(i);
                    convertFile(file);
                    updateProgress(i + 1, filesToConvert.size());
                }
                return null;
            }
        };
        progressBar.progressProperty().bind(conversionTask.progressProperty());
        new Thread(conversionTask).start();
    }

    public void cancelConversion() {
        isCancelled = true;
        updateProgressLabel("Conversion cancelled.");
    }

    private void convertFile(File file) {
        // Mock conversion logic
        try {
            Thread.sleep(1000); // Simulate time-consuming task
        } catch (InterruptedException ignored) {
        }
    }

    private void updateProgressLabel(String s) {
    }
}
