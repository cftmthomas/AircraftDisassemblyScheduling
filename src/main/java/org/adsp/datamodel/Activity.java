package org.adsp.datamodel;

import org.adsp.tools.json.JsonWriter;

public record Activity(int operation, int start, int end) {
    public String toString(){
        return JsonWriter.objectToString(this);
    }
}
