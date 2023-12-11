package org.adsp.datamodel;

import org.adsp.tools.TimeWindow;
import org.adsp.tools.json.JsonWriter;

public record Resource(
        int id,
        String name,
        String category,
        TimeWindow[] unavailable,
        int cost
) {
    public String toString(){
        return JsonWriter.objectToString(this);
    }
}
