/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.utils.ClockSignal;
import com.nuclearunicorn.libroguelike.vgui.effects.EffectsSystem;
import com.nuclearunicorn.libroguelike.vgui.effects.FXTooltip;

/**
 *
 * @author dpopov
 */
public class TooltipSystem extends EffectsSystem {
    
    static final long TOOLTIP_HOVER_TIME = 2000;    //number of ms to hold mouse over control before showing tooltip
    
    NE_GUI_System gui = null;
    NE_GUI_Element focused_element = null;
    long hover_time = 0;
    static FXTooltip fx_tooltip = null;
    
    ClockSignal signal = new ClockSignal();

    void set_gui(NE_GUI_System gui) {
        this.gui = gui;
    }
    
    public static void set_tooltip(FXTooltip fx_tooltip){
        TooltipSystem.fx_tooltip = fx_tooltip;
    }

    
    @Override
    public void update() {

        super.update(); //enforce gc on expired tooltips
        
        NE_GUI_Element elem = gui.get_gui_element(Input.get_mx(), Input.get_my());

        if(focused_element == null){
            focused_element = elem;
        }

        if (elem != null && elem != focused_element){
            focused_element = elem;
            tooltip_cancel();
        }else{
            if (signal.get_signal(TOOLTIP_HOVER_TIME)) {
                tooltip_show(elem);
            }
        }
    }

    private void tooltip_cancel() {

        //System.out.println("elem out of focus, canceling");

        signal.reset();
        //hide tooltip if presents
        if (fx_tooltip != null){

            //mark tooltip as dispoable and erase pointer for a new tooltip instance

            fx_tooltip.disable();
            fx_tooltip = null;
        }
    }

    private void tooltip_show(NE_GUI_Element elem) {

        //throw new UnsupportedOperationException("Not yet implemented");
        /*if (fx_tooltip == null){

            System.out.println("showing tooltip on elem"+elem);

            //show it there
            FXTooltip tooltip = new FXTooltip(elem);
            root.add(tooltip);
            TooltipSystem.set_tooltip(tooltip);
        }*/
    }

    /*public void e_on_event(Event event){
        if (event instanceof ETooltipShow){
            FXTooltip tooltip = new FXTooltip(
                ((ETooltipShow)event).element
            );
            root.add(tooltip);
            TooltipSystem.set_tooltip(tooltip);
        }
    }*/
    
}
