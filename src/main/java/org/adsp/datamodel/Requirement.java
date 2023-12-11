package org.adsp.datamodel;

import org.adsp.tools.json.JsonWriter;

public record Requirement(String[] category, int quantity) {
    public String toString(){
        return JsonWriter.objectToString(this);
    }
}
