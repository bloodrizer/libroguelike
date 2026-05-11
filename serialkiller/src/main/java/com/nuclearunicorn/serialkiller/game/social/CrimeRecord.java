package com.nuclearunicorn.serialkiller.game.social;

/**

 */
public class CrimeRecord {
    public final CrimeType crimeType;
    public int count = 1;

    public CrimeRecord(CrimeType crimeType){
        this.crimeType = crimeType;
    }

    public void incCount(){
        count++;
    }
}
