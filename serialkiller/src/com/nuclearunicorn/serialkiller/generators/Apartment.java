package com.nuclearunicorn.serialkiller.generators;

import com.nuclearunicorn.libroguelike.game.ent.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 18.03.12
 * Time: 14:37
 * To change this template use File | Settings | File Templates.
 */
public class Apartment extends Block {
    public Apartment(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    public List<Block> rooms = new ArrayList<Block>(10);
    public List<Entity> beds = new ArrayList<Entity>(3);

    public Apartment(Block district) {
        super(district.getX(), district.getY(), district.getW(), district.getH());
    }
}
