package org.adsp.datamodel;

import org.adsp.tools.json.JsonWriter;

public record Operation(
        int id,
        String name,
        String card,
        int duration,
        int location,
        int occupancy,
        int mass,
        Requirement[] resources,
        int[] precedences
) {
    public String toString(){
        return JsonWriter.objectToString(this);
    }
}
