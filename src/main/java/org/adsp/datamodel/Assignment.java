package org.adsp.datamodel;

import org.adsp.tools.json.JsonWriter;

public record Assignment(int resource, int operation, int requirement, int start, int end) {
    public Assignment(int resource, int operation, int start, int end){
        this(resource, operation, 0, start, end);
    }
    public String toString(){
        return JsonWriter.objectToString(this);
    }
}
