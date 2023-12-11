package org.adsp.datamodel;

import org.adsp.tools.json.JsonWriter;

public record Location(int id, String name, String zone, int capacity) {
    public boolean isForward(){
        return zone.equals("FWD");
    }

    public boolean isAft(){
        return zone.equals("AFT");
    }

    public boolean isRight(){
        return zone.equals("RH");
    }

    public boolean isLeft(){
        return zone.equals("LH");
    }

    public String toString(){
        return JsonWriter.objectToString(this);
    }
}
