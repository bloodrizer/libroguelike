package com.nuclearunicorn.serialkiller.generators;

import com.nuclearunicorn.libroguelike.utils.Resources;
import com.nuclearunicorn.libroguelike.utils.Rng;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 */
public class NameGenerator {

    //.class.getResourceAsStream(FONT_PATH)

    Random namesRandom = Rng.derive(Rng.NAMES);

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
        for (String strLine : Resources.lines("/resources/namegen/surnames.csv")) {
            String[] line = strLine.replace("\"","").split(",");
            surnames.add(line[0]);
        }
    }

    private static void parseNames(List<String> names, String sex){
        for (String strLine : Resources.lines("/resources/namegen/"+sex+".txt")) {
            String[] line = strLine.split(" ");
            names.add(line[0]);
        }
    }
    
    public String generate( boolean isMale ){
        String name = generateName(isMale);
        String surname = generateSurname();
        
        return name + " " + surname;
    }

    public String generateName( boolean isMale ) {
        String name = "";
        if (isMale){
            name = male.get(namesRandom.nextInt(male.size()));
        }else{
            name = female.get(namesRandom.nextInt(female.size()));
        }

        return name;
    }

    public String generateSurname() {
        return surnames.get(namesRandom.nextInt(surnames.size()));
    }
}
