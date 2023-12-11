module org.cpvisu {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires eu.hansolo.tilesfx;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires org.apache.commons.io;

    requires cpoptimizer;

    exports org.adsp.datamodel;
    exports org.adsp.tools;
    opens org.adsp.datamodel to com.fasterxml.jackson.databind;
    opens org.adsp.tools to com.fasterxml.jackson.databind;

    exports org.adsp.visu.runnable;
    opens org.adsp.visu.runnable to javafx.fxml;
}