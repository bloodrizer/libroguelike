/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.core;

/**
 * placeholder for the console
 */
public class Console {
    public static void execute_cmd(String cmd){
        System.out.println("Console:"+cmd);

        String[] cmd_arr = cmd.split(" ");
        if (cmd_arr[0].equals("/give") && cmd_arr.length == 3){
            String type = cmd_arr[1];
            int count = Integer.parseInt(cmd_arr[2]);

            //Player.get_ent().container.add_item(BaseItem.produce(type, count));
        }
    }
}

//todo: some method to register shit
