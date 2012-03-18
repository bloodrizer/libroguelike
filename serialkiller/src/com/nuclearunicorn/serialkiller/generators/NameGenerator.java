package com.nuclearunicorn.serialkiller.generators;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 */
public class NameGenerator {

    //.class.getResourceAsStream(FONT_PATH)

    Random namesRandom = new Random();

    static List<String> male = new ArrayList<String>(1024);
    static List<String> female = new ArrayList<String>(1024);
    static List<String> surnames = new ArrayList<String>(1024);

    static {
        parseNames(male, "male");
        parseNames(female, "female");

        parseSurnames();
    }
    
    public NameGenerator(){

    }

    private static void parseSurnames() {
        InputStream is = NameGenerator.class.getResourceAsStream("/resources/namegen/surnames.csv");

        DataInputStream in = new DataInputStream(is);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String strLine;

        try {
            while ((strLine = br.readLine()) != null)   {
                String[] line = strLine.replace("\"","").split(",");
                surnames.add(line[0]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void parseNames(List<String> names, String sex){
        InputStream is = NameGenerator.class.getResourceAsStream("/resources/namegen/"+sex+".txt");

        DataInputStream in = new DataInputStream(is);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String strLine;

        try {
            while ((strLine = br.readLine()) != null)   {
                String[] line = strLine.split(" ");
                names.add(line[0]);
            }

            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public String generate( boolean isMale ){
        String name = "";
        if (isMale){
            name = male.get(namesRandom.nextInt(male.size()));
        }else{
            name = female.get(namesRandom.nextInt(female.size()));
        }
        String surname = surnames.get(namesRandom.nextInt(surnames.size()));
        
        return name + " " + surname;
    }
}
