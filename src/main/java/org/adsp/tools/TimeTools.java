package org.adsp.tools;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeTools {
    public static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    public static final DateTimeFormatter dtfc = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String getCurrentDateTime(){
       return dtf.format(LocalDateTime.now());
    }

    public static String getCurrentDateTimeCompact(){
        return dtfc.format(LocalDateTime.now());
    }
}
