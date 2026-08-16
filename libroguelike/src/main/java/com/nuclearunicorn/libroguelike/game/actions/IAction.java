/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.actions;

/**
 *
 * @author Administrator
 */
public interface IAction<T> {
    public void execute();
    public void set_owner(T owner);
    public T get_owner();
    public void set_name(String name);
    public String get_name();
}
