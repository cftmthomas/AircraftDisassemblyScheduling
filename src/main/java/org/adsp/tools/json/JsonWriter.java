package org.adsp.tools.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.adsp.datamodel.Instance;
import org.adsp.datamodel.Log;
import org.adsp.datamodel.Solution;

import java.io.File;
import java.io.IOException;

public class JsonWriter {
    static ObjectMapper mapper = new JacksonMapper().getMapper();

    public static void writeInstanceToFile(Instance instance, String path){
        try{
            File out = new File(path);
            File file = out.isDirectory() ? new File(out.getAbsolutePath() + "/" + instance.name() + ".json") : out;
            file.getParentFile().mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, instance);
        } catch (IOException e) {
            System.out.println("Unable to write to specified file!");
            e.printStackTrace();
        }
    }

    public static void printInstance(Instance instance){
        try{
            String prettyInstance = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(instance);
            System.out.println(prettyInstance);
        } catch (JsonProcessingException e) {
            System.out.println("Error: Unable to print instance\n" + e);
        }
    }

    public static void writeSolutionToFile(Solution solution, String path){
        try{
            File out = new File(path);
            File file = out.isDirectory() ? new File(out.getAbsolutePath() + "/" + solution.instance().name() + ".json") : out;
            file.getParentFile().mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, solution);
        } catch (IOException e) {
            System.out.println("Unable to write to specified file!");
            e.printStackTrace();
        }
    }

    public static void printSolution(Solution solution){
        try{
            String prettySolution = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(solution);
            System.out.println(prettySolution);
        } catch (JsonProcessingException e) {
            System.out.println("Error: Unable to print solution\n" + e);
        }
    }

    public static String objectToString(Object object){
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            System.out.println("Error: Unable to map object to string\n" + e);
            return "";
        }
    }

    public static void writeLogToFile(Log log, String path) {
        try{
            File out = new File(path);
            File file = out.isDirectory() ? new File(out.getAbsolutePath() + "/" + log.instance() + ".json") : out;
            file.getParentFile().mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, log);
        } catch (IOException e) {
            System.out.println("Unable to write to specified file!");
            e.printStackTrace();
        }
    }

    public static void printLog(Log log){
        try{
            String prettyLog = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(log);
            System.out.println(prettyLog);
        } catch (JsonProcessingException e) {
            System.out.println("Error: Unable to print log\n" + e);
        }
    }
}
