/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.events;

import com.nuclearunicorn.libroguelike.game.items.ItemContainer;

/**
 *
 * @author Administrator
 */
public class EContainerUpdate extends Event{
    public ItemContainer container;
    public EContainerUpdate(ItemContainer container){
        this.container = container;
    }
}
