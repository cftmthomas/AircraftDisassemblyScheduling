package org.adsp.tools.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.adsp.datamodel.*;

import java.io.File;
import java.io.IOException;

public class JsonReader {
    static ObjectMapper mapper = new JacksonMapper().getMapper();

    public static Instance readInstanceFile(String file){
        try{
            System.out.println("reading instance file " + file);
            Instance instance = mapper.readValue(new File(file), Instance.class);
//            JsonWriter.printInstance(instance);

            return instance;
        } catch (IOException e) {
            System.out.println("Error: unable to read instance!");
            e.printStackTrace();
            return new Instance("error", "","",0,0,0, new Resource[0], new Location[0], new Operation[0]);
        }
    }

    public static Solution readSolutionFile(String file) {
        try{
            System.out.println("reading solution file " + file);
            Solution solution = mapper.readValue(new File(file), Solution.class);
//            JsonWriter.printSolution(solution);

            return solution;
        } catch (IOException e) {
            System.out.println("Error: unable to read solution!");
            e.printStackTrace();
            Instance instance = new Instance("error", "","",0,0,0, new Resource[0], new Location[0], new Operation[0]);
            return new Solution(instance, new Activity[0], new Assignment[0], instance.maxTime(), 0);
        }
    }

    public static Log readLogFile(String file) {
        try{
            System.out.println("reading log file " + file);
            Log log = mapper.readValue(new File(file), Log.class);
//            JsonWriter.printLog(log);

            return log;
        } catch (IOException e) {
            System.out.println("Error: unable to read log!");
            e.printStackTrace();
            return new Log("error", 0, 0, new LogEntry[0]);
        }
    }
}
