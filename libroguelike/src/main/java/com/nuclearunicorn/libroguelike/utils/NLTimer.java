/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NLTimer {
    final static Logger logger = LoggerFactory.getLogger(NLTimer.class);

    public long time = 0;

    public void push(){
        time = System.nanoTime();
    }

    public float popDiff(){
        return (float)( System.nanoTime() - time ) / (1000*1000);
    }

    public void pop(){
        float time_diff = (float)( System.nanoTime() - time ) / (1000*1000);
        logger.debug( "NLTimer : {}  ms elasped", Float.toString(time_diff));
    }

    public void pop(String message){
        float time_diff = (float)( System.nanoTime() - time ) / (1000*1000);
        logger.debug( "NLTimer : {} ms elasped. ( {} )", Float.toString(time_diff), message );
    }
}
