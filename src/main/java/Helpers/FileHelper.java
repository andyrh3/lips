package Helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

public class FileHelper {

    public static void writeObjectToJSONFile(Object data, File file){
        try {
            FileWriter fw = new FileWriter(file);
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .setDateFormat("dd MMM yyyy")
                    .setPrettyPrinting()
                    .create();
            String json = gson.toJson(data);
            fw.write(json);
            fw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeObjectToFile(Object object, File file){
        try {
            FileOutputStream f = new FileOutputStream(file);
            ObjectOutputStream o = new ObjectOutputStream(f);
            o.writeObject(object);
            o.close();
            f.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            System.out.println("Error initializing stream");
        }
    }

    public static Object readObjectFromFile(File file){
        try {
            FileInputStream fi = new FileInputStream(file);
            ObjectInputStream oi = new ObjectInputStream(fi);
            Object object = oi.readObject();
            oi.close();
            fi.close();
            return object;

        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            System.out.println("Error initializing stream");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}
