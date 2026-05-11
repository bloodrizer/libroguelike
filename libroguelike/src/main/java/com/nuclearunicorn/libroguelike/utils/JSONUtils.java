package com.nuclearunicorn.libroguelike.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Bloodrizer
 */
public class JSONUtils {

    private GsonBuilder gsonBuilder;
    private Gson gson;

    public JSONUtils() {
        gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();
    }

    List<String> getLines(String msg){
        List<String> lines = new ArrayList<String>();

        return lines;
    }
    
    public Map<String, String> getJson(String filename){
        InputStream jsonStream = JSONUtils.class.getResourceAsStream(filename);

        java.util.Scanner s = new java.util.Scanner(jsonStream, "UTF-8").useDelimiter("\\A");
        String json = s.hasNext() ? s.next() : "";

        Map<String, String> jsonMap = gson.fromJson(json, new TypeToken<Map<String, String>>(){}.getType());

        return jsonMap;
    }
    
    public String getMessage(String filename){
        Map<String, String> json = getJson("/resources/messages/" + filename);
        return json.get("msg");
    }
}
