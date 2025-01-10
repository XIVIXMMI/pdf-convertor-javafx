// ExcelService.java
package com.lazydev.pdf_convert.service;

import com.lazydev.pdf_convert.model.PDFData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ExcelService {
    private static final Logger logger = LoggerFactory.getLogger(ExcelService.class);

    private static final String[] HEADERS = {
                "Tên File","Tên kinh doanh", "Địa chỉ", "Số serial", "Loại máy",
                "Mã máy", "Ghi chú", "MID", "TID", "TID 00",
                "TID V-TOP", "POS V-TOP"
    };

    public void convertTxtToExcel(File txtFile) throws Exception {
        if (!txtFile.exists() || !txtFile.getName().endsWith(".txt")) {
            throw new IllegalArgumentException("Invalid text file");
        }
        List<DataEntry> dataList = readDataFromTxt(txtFile);
        String excelFilePath = txtFile.getParent() + File.separator +
                txtFile.getName().replace(".txt", ".xlsx");
        createExcelFile(dataList, excelFilePath);
    }

    private record DataEntry(PDFData data, String fileName) { }

    private List<DataEntry> readDataFromTxt(File txtFile) throws Exception {
        List<DataEntry> dataList = new ArrayList<>();
        PDFData currentData = null;
        String currentFileName = null;

        try(BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(txtFile), StandardCharsets.UTF_8)
        )){
            String line;
            while ((line = reader.readLine()) != null) {
                if(line.startsWith("File: ")) {
                    if(currentData != null ){
                        dataList.add( new DataEntry(currentData, currentFileName));
                    }
                    currentData = new PDFData();
                    currentFileName = line.substring(6).trim();
                    continue;
                }
                if(currentData != null){
                    processDataLine(line,currentData);
                }
            }
            if(currentData != null){
                dataList.add(new DataEntry(currentData,currentFileName));
            }
        }
        return dataList;
    }

    private void processDataLine(String line, PDFData data) {
        if (line.startsWith("Tên kinh doanh: ")) {
            data.setBusinessName(line.substring("Tên kinh doanh: ".length()));
        } else if (line.startsWith("Địa chỉ: ")) {
            data.setAddress(line.substring("Địa chỉ: ".length()));
        } else if (line.startsWith("Số serial: ")) {
            data.setSerialNumber(line.substring("Số serial: ".length()));
        } else if (line.startsWith("Loại máy: ")) {
            data.setPosDevice(line.substring("Loại máy: ".length()));
        } else if (line.startsWith("Mã máy: ")) {
            data.setGroupName(line.substring("Mã máy: ".length()));
        } else if (line.startsWith("Ghi chú: ")) {
            data.setNotes(line.substring("Ghi chú: ".length()));
        } else if (line.startsWith("MID: ")) {
            data.setMerchantId(line.substring("MID: ".length()));
        } else if (line.startsWith("TID: ")) {
            data.setTerminalId(line.substring("TID: ".length()));
        } else if (line.startsWith("TID 00: ")) {
            data.setTerminalId00(line.substring("TID 00: ".length()));
        }else if (line.startsWith("TID V-TOP: ")) {
            data.setTerminalVtopId(line.substring("TID V-TOP: ".length()));
        } else if (line.startsWith("POS_V-TOP: ")) {
            data.setPosVtop(line.substring("POS V-TOP: ".length()));
        }
    }

    private void createExcelFile(List<DataEntry> dataList, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("POS Data");

            // Create header row
            Row headerRow = sheet.createRow(0);
            createHeaders(headerRow);

            // Create data rows
            int rowNum = 1;
            for (DataEntry entry : dataList) {
                Row row = sheet.createRow(rowNum++);
                fillDataRow(row, entry.data(), entry.fileName());
            }

            // Auto-size columns
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to file
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }
        }
    }

    private void createHeaders(Row headerRow) {
        CellStyle headerStyle = headerRow.getSheet().getWorkbook().createCellStyle();
        Font headerFont = headerRow.getSheet().getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void fillDataRow(Row row, PDFData data, String fileName) {
        int columnIndex = 0;
        row.createCell(columnIndex++).setCellValue(fileName);
        row.createCell(columnIndex++).setCellValue(data.getBusinessName());
        row.createCell(columnIndex++).setCellValue(data.getAddress());
        row.createCell(columnIndex++).setCellValue(data.getSerialNumber());
        row.createCell(columnIndex++).setCellValue(data.getPosDevice());
        row.createCell(columnIndex++).setCellValue(data.getGroupName());
        row.createCell(columnIndex++).setCellValue(data.getNotes());
        row.createCell(columnIndex++).setCellValue(data.getMerchantId());
        row.createCell(columnIndex++).setCellValue(data.getTerminalId());
        row.createCell(columnIndex++).setCellValue(data.getTerminalId00());
        row.createCell(columnIndex++).setCellValue(data.getTerminalVtopId());
        row.createCell(columnIndex).setCellValue(data.getPosVtop());
    }

}