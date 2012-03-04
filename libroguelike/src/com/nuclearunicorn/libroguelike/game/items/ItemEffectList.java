/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.items;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author Administrator
 */
public class ItemEffectList {
     public static java.util.Map<String,List<ItemEffect>> effects =
            Collections.synchronizedMap(new java.util.HashMap<String,List<ItemEffect>>(64));
}
