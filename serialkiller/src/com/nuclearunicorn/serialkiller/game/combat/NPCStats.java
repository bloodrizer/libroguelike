package com.nuclearunicorn.serialkiller.game.combat;


import java.util.Random;

public class NPCStats {
    
    Random rand;
    
    int str;
    int per;
    int end;
    int chr;
    int intl;
    int agi;
    int luk;

    //TODO : seed initialization
    public NPCStats(){
        rand = new Random();

        str = rand.nextInt(7)+3;
        per = rand.nextInt(7)+3;
        end = rand.nextInt(7)+3;
        chr = rand.nextInt(7)+3;
        intl = rand.nextInt(7)+3;
        agi = rand.nextInt(7)+3;
        luk = rand.nextInt(7)+3;
    }
}
