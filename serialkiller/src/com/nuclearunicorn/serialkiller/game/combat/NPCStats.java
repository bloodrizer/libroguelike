package com.nuclearunicorn.serialkiller.game.combat;


import java.util.Random;

public class NPCStats {
    
    Random rand;
    
    public int str;
    public int per;
    public int end;
    public int chr;
    public int intl;
    public int agi;
    public int luk;

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
