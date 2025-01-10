package com.lazydev.pdf_convert.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PDFData {

    private String groupName;
    private String businessName;
    private String address;
    private String serialNumber;
    private String posDevice;
    private String notes;
    private String merchantId;
    private String terminalId;
    private String terminalId00;
    private String terminalVtopId;
    private String posVtop;

    @Override
    public String toString(){
        return  "Tên kinh doanh: " + businessName + "\n" +
                "Địa chỉ: " + address + "\n" +
                "Số serial: " + serialNumber + "\n" +
                "Loại máy: " + posDevice + "\n" +
                "Mã máy: " + groupName + "\n" +
                "Ghi chú: " + notes + "\n" +
                "MID: " + merchantId + "\n" +
                "TID: " + terminalId + "\n" +
                "TID 00: " + terminalId00 + "\n" +
                "TID V-TOP: " + terminalVtopId + "\n" +
                "POS_V-TOP: " + posVtop + "\n";
    }


}
