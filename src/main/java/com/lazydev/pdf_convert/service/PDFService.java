package com.lazydev.pdf_convert.service;

import com.lazydev.pdf_convert.exception.PDFProcessingException;
import com.lazydev.pdf_convert.util.RegexExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PDFService {
    private static final Logger logger = LoggerFactory.getLogger(PDFService.class);
    private static final String PDF_EXTENSION = ".pdf";
    private static final String TXT_EXTENSION = ".txt";

    public String convertAllPDFs(File directory) throws PDFProcessingException {
        validateDirectory(directory);

        File[] pdfFiles = Optional.ofNullable(directory.listFiles((dir, name) -> name.toLowerCase().endsWith(PDF_EXTENSION)))
                .orElse(new File[0]);

        if (pdfFiles.length == 0) {
            logger.warn("No PDF files found in directory: {}", directory.getPath());
            return "Không tìm thấy file PDF nào trong thư mục!";
        }

        String combinedFileName = directory.getName() + TXT_EXTENSION;
        File combinedFile = new File(directory, combinedFileName);
        StringBuilder overallStatus = new StringBuilder("Đang xử lý...\n");

        try (FileWriter writer = new FileWriter(combinedFile, StandardCharsets.UTF_8)) {
            processFiles(pdfFiles, writer, overallStatus);
            logger.info("Successfully processed {} files in directory: {}", pdfFiles.length, directory.getPath());
        } catch (IOException e) {
            logger.error("Error creating combined file: {}", combinedFile.getPath(), e);
            throw new PDFProcessingException("Đã xảy ra lỗi khi tạo file tổng hợp: " + e.getMessage());
        }

        return overallStatus.toString();
    }

    private void validateDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            throw new PDFProcessingException("Thư mục không tồn tại!");
        }
        if (!directory.isDirectory()) {
            throw new PDFProcessingException("Đường dẫn không phải là thư mục!");
        }
    }

    private void processFiles(File[] pdfFiles, FileWriter writer, StringBuilder overallStatus) {
        Arrays.sort(pdfFiles); // Sort files for consistent processing order

        for (File file : pdfFiles) {
            try (PDDocument document = PDDocument.load(file)) {
                processIndividualFile(file, document, writer, overallStatus);
            } catch (IOException e) {
                logger.error("Error processing file: {}", file.getName(), e);
                overallStatus.append(file.getName()).append(": Lỗi khi xử lý! Chi tiết: ")
                        .append(e.getMessage()).append("\n");
            }
        }
    }

    private void processIndividualFile(File file, PDDocument document, FileWriter writer, StringBuilder overallStatus)
            throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        String filteredData = RegexExtractor.extractSpecificData(text);

        if (filteredData.isEmpty()) {
            logger.warn("No data found in file: {}", file.getName());
            overallStatus.append(file.getName()).append(": không tìm thấy dữ liệu cần thiết!\n");
            return;
        }

        writeToFile(writer, file.getName(), filteredData);
        overallStatus.append(file.getName()).append(": Chuyển đổi thành công!\n");
        logger.info("Successfully processed file: {}", file.getName());
    }

    private void writeToFile(FileWriter writer, String fileName, String data) throws IOException{
        writer.write("File: " + fileName + "\n");
        writer.write(data);
        writer.write("\n\n");
        writer.write("----------------------------------------------------------------\n");
        writer.flush();
    }
}