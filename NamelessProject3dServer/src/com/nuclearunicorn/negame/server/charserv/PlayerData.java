/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.negame.server.charserv;

/**
 *
 * @author Bloodrizer
 */

/*
 * This shit holds basic player information
 *
 */

public class PlayerData {
    public String login;
    public String password;

    public PlayerData(String login, String password){
        this.login = login;
        this.password = password;
    }
}
