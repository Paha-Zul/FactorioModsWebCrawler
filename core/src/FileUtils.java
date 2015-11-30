import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by Paha on 11/12/2015.
 */
public class FileUtils {
    public static ObjectMapper mapper;

    static{
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static File findFileWithPartName(File dir, String fileName){
        File[] list = dir.listFiles();
        for(File file : list){
            if(file.getName().contains(fileName))
                return file;
        }

        return null;
    }

    public static <T> T getJsonObject(File file, Class<T> classType){
        try {
            return mapper.readValue(file, classType);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static <T> T getJsonObjectAsMap(File file, Class<T> classType){
        try {
            return mapper.readValue(file, new TypeReference<Map<String, String>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void writeObjectToJson(File file, Object object){
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, object);
           // mapper.writeValue(file, object);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes all files excluding .json files in a directory.
     * @param dir The directory to delete files in.
     */
    public static void deleteAllFiles(File dir){
        File[] fileList = dir.listFiles();

        for(File file : fileList){
            if(file.getName().endsWith(".json")) continue;
            file.setWritable(true); //We can't delete if read only, so try and make writeable.
            if(!file.delete())      //Delete the file.
                System.out.println("Something went wrong, the file "+file.getName()+" couldn't be deleted.");
        }
    }
}
