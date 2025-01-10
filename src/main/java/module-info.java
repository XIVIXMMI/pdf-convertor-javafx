module com.lazydev.pdf_convert {
    requires javafx.controls;
    requires javafx.fxml;
    //requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires static lombok;
    requires org.slf4j;
    requires org.apache.poi.ooxml;
    requires org.apache.pdfbox;
    requires java.desktop;
    //requires eu.hansolo.tilesfx;

    opens com.lazydev.pdf_convert to javafx.fxml;
    exports com.lazydev.pdf_convert;
    exports com.lazydev.pdf_convert.controller;
    opens com.lazydev.pdf_convert.controller to javafx.fxml;
}