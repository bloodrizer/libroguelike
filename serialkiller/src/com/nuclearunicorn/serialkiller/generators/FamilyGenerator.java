package com.nuclearunicorn.serialkiller.generators;

/**
 */
public class FamilyGenerator extends NPCGenerator{

    String surname = "";
    String name = "";
    NameGenerator namegen = new NameGenerator();

    public FamilyGenerator(){
        surname = namegen.generateSurname();
    }

    public String generateName(boolean isMale){
        name = namegen.generate(isMale);

        return name + " " + surname;
    }

}
