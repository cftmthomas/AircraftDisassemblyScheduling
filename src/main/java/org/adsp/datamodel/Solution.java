package org.adsp.datamodel;

import org.adsp.tools.json.JsonWriter;

public record Solution(Instance instance, Activity[] activities, Assignment[] assignments, int makespan, int cost) {
    public String toString(){
        return JsonWriter.objectToString(this);
    }
}
