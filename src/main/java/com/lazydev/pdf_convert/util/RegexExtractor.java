package com.lazydev.pdf_convert.util;

import com.lazydev.pdf_convert.model.PDFData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexExtractor {
    private static final Logger logger = LoggerFactory.getLogger(RegexExtractor.class);

    // Store patterns as constants for better maintenance
    private static final Map<String, Pattern> PATTERNS = new HashMap<>();

    static {
        PATTERNS.put("businessName", Pattern.compile("Tên kinh doanh \\(.*\\):\\s*(.+)"));
        PATTERNS.put("address", Pattern.compile("Địa chỉ lắp máy:\\s*(.+)"));
        PATTERNS.put("serialNumber", Pattern.compile("Số S/N của máy EDC:\\s*(\\S+)"));
        PATTERNS.put("posDevice", Pattern.compile("Loại máy:\\s*(.+)"));
        PATTERNS.put("groupName", Pattern.compile("Tên pháp lý \\(Theo giấy phép kinh doanh\\):(?:.*-\\s*(\\S+)|\\s*(.+))"));
        PATTERNS.put("notes", Pattern.compile("Ghi chú:\\s*(.+)"));
        PATTERNS.put("merchantId", Pattern.compile("MID\\s+VND\\s+([\\d\\s\\n]+)"));
        PATTERNS.put("terminalId", Pattern.compile("TID\\s+VND\\s+([\\d\\s\\n]+)"));
        PATTERNS.put("terminalIdVtop", Pattern.compile("TID V-TOP\\s+([\\d\\s\\n]+)"));
    }

    public static String extractSpecificData(String text) {
        if (text == null || text.trim().isEmpty()) {
            logger.warn("Input text is null or empty");
            return "";
        }

        PDFData data = new PDFData();
        try {
            extractBusinessData(text, data);
            extractDeviceData(text, data);
            extractIdentificationData(text, data);

            logger.debug("Successfully extracted data for business: {}", data.getBusinessName());
            return data.toString();
        } catch (Exception e) {
            logger.error("Error extracting data from text", e);
            return "";
        }
    }

    private static void extractBusinessData(String text, PDFData data) {
        // Business name
        extractPattern("businessName", text).ifPresent(data::setBusinessName);

        // Address
        extractPattern("address", text).ifPresent(data::setAddress);

        // Group name
        Matcher groupMatcher = PATTERNS.get("groupName").matcher(text);
        if (groupMatcher.find()) {
            String groupName = groupMatcher.group(1) != null ?
                    groupMatcher.group(1).trim() :
                    groupMatcher.group(2).trim();
            data.setGroupName(groupName);
        }

        // Notes
        extractPattern("notes", text).ifPresent(notes -> {
            if (notes.startsWith("Ngày")) {
                data.setNotes("null");
            } else {
                data.setNotes(notes.trim());
            }
        });
    }

    private static void extractDeviceData(String text, PDFData data) {
        // Serial number
        extractPattern("serialNumber", text).ifPresent(serialNumber ->
                data.setSerialNumber("F" + serialNumber));

        // POS device
        extractPattern("posDevice", text).ifPresent(device ->
                data.setPosDevice(device.split("[^a-zA-Z0-9 ]+")[0].trim())
        );
    }

    private static void extractIdentificationData(String text, PDFData data) {
        // Merchant ID
        extractPattern("merchantId", text)
                .ifPresent(mid -> data.setMerchantId(mid.replace(" ", "").trim()));

        // Terminal ID and Terminal ID 00
        extractPattern("terminalId", text).ifPresent(tid -> {
            String cleanTid = tid.replace(" ", "").trim();
            data.setTerminalId(cleanTid);

            // Generate Terminal ID 00 if applicable
            if (cleanTid.length() >= 4 && cleanTid.substring(2, 4).equals("39")) {
                String tid00 = cleanTid.substring(0, 2) + "00" + cleanTid.substring(4);
                data.setTerminalId00(tid00);
            }
        });

        // Terminal ID V-TOP and POS V-TOP
        extractPattern("terminalIdVtop", text).ifPresent(tidVtop -> {
            String cleanTidVtop = tidVtop.replace(" ", "").trim();
            data.setTerminalVtopId(cleanTidVtop);
            data.setPosVtop("POS_" + cleanTidVtop);
        });
    }

    private static java.util.Optional<String> extractPattern(String patternKey, String text) {
        Pattern pattern = PATTERNS.get(patternKey);
        if (pattern == null) {
            logger.error("Pattern not found for key: {}", patternKey);
            return java.util.Optional.empty();
        }

        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return java.util.Optional.of(matcher.group(1).trim());
        }
        return java.util.Optional.empty();
    }
}