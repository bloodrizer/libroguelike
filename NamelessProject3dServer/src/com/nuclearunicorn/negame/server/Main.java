package com.nuclearunicorn.negame.server;

import com.nuclearunicorn.negame.server.core.NEServerCore;

/**
 * Created by IntelliJ IDEA.
 * User: bloodrizer
 * Date: 25.06.12
 * Time: 12:04
 * To change this template use File | Settings | File Templates.
 */
public class Main {
    public static void main(String[] args) {
        NEServerCore serverCore = new NEServerCore();

        serverCore.run();
        serverCore.destroy();
    }
}
