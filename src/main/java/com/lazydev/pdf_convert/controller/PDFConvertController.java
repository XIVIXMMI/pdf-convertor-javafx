package com.lazydev.pdf_convert.controller;

import com.lazydev.pdf_convert.service.ExcelService;
import com.lazydev.pdf_convert.service.PDFService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PDFConvertController {

    @FXML private TextField pdfPathField;
    @FXML private VBox dropArea;
    @FXML private TextArea statusTextArea;
    @FXML private ProgressBar progressBar;
    @FXML private Button cancelButton;
    @FXML private Button convertButton;
    @FXML private Label progressLabel;
    @FXML private Label timerLabel;

    private File[] selectedFolders;
    private volatile boolean isCancelled;
    private final ExecutorService executorService;
    private final PDFService pdfService;
    private final StringProperty progressMessage = new SimpleStringProperty();
    private final SimpleBooleanProperty converting = new SimpleBooleanProperty(false);
    private Task<Void> currentTask;
    private final ExcelService excelService;

    private final SimpleDoubleProperty progress = new SimpleDoubleProperty(0);
    private long startTime;
    private Timeline timer;
    private final SimpleLongProperty elapsedTimeInSeconds = new SimpleLongProperty(0);

    // Progress Bar
    private final DoubleProperty totalProgress = new SimpleDoubleProperty(0);

    public PDFConvertController() {
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger();
                @Override
                public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("PDFConverter-" + counter.incrementAndGet());
                        thread.setDaemon(true);
                    return thread;
            }
        });
        this.pdfService = new PDFService();
        this.excelService = new ExcelService();
    }

    private enum ConversionType {
        PDF_TO_TXT,
        TXT_TO_EXCEL,
        BOTH
    }

    // Controller
    @FXML
    public void initialize() {

        setupDragAndDrop();
        setupButtons();
        setupTimer();

        progressBar.progressProperty().bind(totalProgress);

        convertButton.disableProperty().bind(converting);
        cancelButton.disableProperty().bind(converting.not());
    }

    private void setupButtons() {
        convertButton.setOnAction(event -> convertData());
        cancelButton.setOnAction(event -> cancelOperation());
    }

    @FXML
    private void convertData() {
        if (selectedFolders == null || selectedFolders.length == 0) {
            showError("Hãy chọn thư mục trước!");
            return;
        }

        // Reset progress
        Platform.runLater(() -> {
            totalProgress.set(0);
            progressLabel.setText("0%");
        });

        startConversion(ConversionType.BOTH);
    }

    private void startConversion(ConversionType type) {
        isCancelled = false;
        currentTask = createConversionTask(type);
        configureTaskBindings(currentTask);

        converting.set(true);
        Platform.runLater(() -> {
            progressBar.getStyleClass().removeAll("complete", "reset");
            progressBar.getStyleClass().add("running");
        });

        new Thread(currentTask).start();
    }

    private Task<Void> createConversionTask(ConversionType type) {
        return new Task<>() {
            @Override
            protected Void call() {
                try {
                    startTime = System.currentTimeMillis();
                    Platform.runLater(() ->{
                        startTime = System.currentTimeMillis();
                        progress.set(0);
                        timer.play();
                    });

                    AtomicInteger completedTasks = new AtomicInteger();
                    int totalTasks = selectedFolders.length;
                    CountDownLatch latch = new CountDownLatch(totalTasks);

                    // initial progress label
                    updateProgressLabel(0, totalTasks);

                    for (File folder : selectedFolders) {
                        if (isCancelled) {
                            updateMessage("Tác vụ đã bị hủy.");
                            break;
                        }

                        processFolderWithType(folder, completedTasks, totalTasks, type, latch);
                    }
                    latch.await();
                    return null;
                }catch ( InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                } finally {
                    Platform.runLater(() -> {
                        converting.set(false);
                        timer.stop();
                        // Lưu tổng thời gian xử lý
                        long totalTime = (System.currentTimeMillis() - startTime) / 1000;
                        setStatus(String.format("Hoàn thành! Tổng thời gian xử lý: %02d:%02d:%02d",
                                totalTime / 3600, (totalTime % 3600) / 60, totalTime % 60), "green");
                    });
                }
            }
        };
    }

    private void setupTimer() {
        timer = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    elapsedTimeInSeconds.set((System.currentTimeMillis() - startTime) / 1000);
                })
        );
        timer.setCycleCount(Timeline.INDEFINITE);

        // Bind timer label to time property
        timerLabel.textProperty().bind(
                Bindings.createStringBinding(() ->
                                String.format("Thời gian xử lý: %02d:%02d:%02d",
                                        elapsedTimeInSeconds.get() / 3600,
                                        (elapsedTimeInSeconds.get() % 3600) / 60,
                                        elapsedTimeInSeconds.get() % 60),
                        elapsedTimeInSeconds
                )
        );
    }

    private void processFolderWithType(File folder, AtomicInteger completedTasks,
                                        int totalTasks, ConversionType type, CountDownLatch latch) {
        if (!folder.isDirectory()) {
            updateTaskMessage("Thư mục không hợp lệ: " + folder.getName());
            latch.countDown();
            return;
        }

        Task<Void> folderTask = new Task<>() {
            @Override
            protected Void call() {
                try {
                    // Calculate progress per folder
                    double progressPerFolder = 1.0 / totalTasks;

                    // PDF conversion phase (50% of folder progress)
                    if (type == ConversionType.PDF_TO_TXT || type == ConversionType.BOTH) {
                        String pdfResult = pdfService.convertAllPDFs(folder);
                        updateTaskStatus(pdfResult, folder.getName());

                        // Update progress for PDF phase
                        Platform.runLater(() -> {
                            double currentProgress = totalProgress.get() + (progressPerFolder * 0.5);
                            totalProgress.set(currentProgress);
                            progressLabel.setText(String.format("%.1f%%", currentProgress * 100));
                        });
                    }

                    // Excel conversion phase (remaining 50% of folder progress)
                    if (type == ConversionType.TXT_TO_EXCEL || type == ConversionType.BOTH) {
                        File txtFile = new File(folder, folder.getName() + ".txt");
                        if (txtFile.exists()) {
                            excelService.convertTxtToExcel(txtFile);
                            updateTaskStatus("Chuyển đổi Excel thành công: " + folder.getName(),
                                    folder.getName());

                            // Update progress for Excel phase
                            Platform.runLater(() -> {
                                double currentProgress = totalProgress.get() + (progressPerFolder * 0.5);
                                totalProgress.set(currentProgress);
                                progressLabel.setText(String.format("%.1f%%", currentProgress * 100));
                            });
                        }
                    }

                } catch (Exception e) {
                    updateTaskStatus("Lỗi xử lý: " + e.getMessage(), folder.getName());
                } finally {
                    int completed = completedTasks.incrementAndGet();
                    updateProgressLabel(completed, totalTasks);

                    Platform.runLater(() -> {
                        double finalProgress = (double) completed / totalTasks;
                        progress.set(finalProgress);
                        updateProgressDisplay(finalProgress);
                    });

                    latch.countDown();
                }
                return null;
            }
        };

        executorService.submit(folderTask);
    }

    // reset progressBar
    private void resetProgressBar() {
        Platform.runLater(() -> {
            progressBar.getStyleClass().removeAll("complete", "running");
            progressBar.getStyleClass().add("reset");
            totalProgress.set(0);
            progressLabel.setText("0%");
        });
    }

    private void updateTaskStatus(String message, String fileName) {
        Platform.runLater(() -> {
            setStatus(fileName + ": " + message, "black");
            progressMessage.set(message);
        });
    }

    // Method to update the progress label text
    public void updateProgressLabel(int processed, int total) {
        progressMessage.set(processed + "/" + total + " thư mục đã xử lý");
    }

    private void updateProgressDisplay(double progress) {
        int percentage = (int) (progress * 100);
        progressLabel.setText(percentage + "%");

        // Khi tiến trình hoàn tất, làm nổi bật thanh tiến trình
        if (progress >= 1.0) {
            progressBar.getStyleClass().removeAll("running", "reset");
            progressBar.getStyleClass().add("complete");
            setStatus("Quá trình chuyển đổi hoàn tất!", "green");
        }
    }

    private void updateTaskMessage(String message) {
        Platform.runLater(() -> progressMessage.set(message));
    }

    private void configureTaskBindings(Task<Void> task) {
        // The binding remains the same
        progressBar.progressProperty().bind(task.progressProperty());
        // Add a listener to update the progress label
        task.progressProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                double progress = newVal.doubleValue();
                int percentage = (int) (progress * 100);
                progressLabel.setText(percentage + "% completed");
            });
        });

        task.messageProperty().addListener((obs, oldMessage, newMessage) ->
                setStatus(newMessage, "black"));
    }

    private void setupDragAndDrop() {
        dropArea.setOnDragOver(this::handleDragOver);
        dropArea.setOnDragDropped(this::handleDragDropped);

        // Add visual feedback
        dropArea.setOnDragEntered(e -> dropArea.setStyle("-fx-border-color: green;"));
        dropArea.setOnDragExited(e -> dropArea.setStyle(""));
    }

    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    private void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles()) {
            selectedFolders = db.getFiles().stream()
                    .filter(File::isDirectory)
                    .toArray(File[]::new);

            if (selectedFolders.length > 0) {
                pdfPathField.setText(String.join(", ",
                        db.getFiles().stream()
                                .map(File::getAbsolutePath)
                                .toArray(String[]::new)));
                success = true;
                setStatus("Đã chọn " + selectedFolders.length + " thư mục", "black");
                resetProgressBar();
            } else {
                setStatus("Vui lòng chọn thư mục hợp lệ", "red");
            }
        }

        event.setDropCompleted(success);
        event.consume();
    }


    private void cancelOperation() {
        isCancelled = true;
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        executorService.shutdownNow();

        Platform.runLater(() -> {
            resetProgressBar();
            totalProgress.set(0);
            progressLabel.setText("Đã hủy tác vụ");
            progressBar.setStyle(""); // Reset màu thanh tiến trình
            timer.stop();
            elapsedTimeInSeconds.set(0);
            timerLabel.setText("Thời gian xử lý: 00:00:00");
            convertButton.setDisable(false);
            cancelButton.setDisable(true);
            setStatus("Đã hủy quá trình chuyển đổi.", "red");
        });
    }

    @FXML
    private void openDirectoryChooser() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Chọn thư mục chứa PDF");
        File selectedDirectory = directoryChooser.showDialog(dropArea.getScene().getWindow());

        if (selectedDirectory != null) {
            selectedFolders = new File[]{selectedDirectory};
            pdfPathField.setText(selectedDirectory.getAbsolutePath());
            setStatus("Đã chọn thư mục: " + selectedDirectory.getName(), "black");
        }
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @FXML
    private void exitApplication() {
        shutdown(); // Clean up resources
        Platform.exit();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setStatus(String message, String color) {
        Platform.runLater(() -> {
            statusTextArea.setStyle("-fx-text-fill: " + color + ";");
            statusTextArea.appendText(message + "\n");
            statusTextArea.setScrollTop(Double.MAX_VALUE); // Auto-scroll to bottom
        });
    }

}
