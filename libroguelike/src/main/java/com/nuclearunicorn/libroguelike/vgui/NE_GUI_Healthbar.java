/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.render.AreaRenderer;

/**
 *
 * @author Administrator
 */
public class NE_GUI_Healthbar extends NE_GUI_Element{

    int val = 20;
    int max_val = 100;

    static AreaRenderer bar_sprite = new AreaRenderer(){
        {
            texture_name = "/ui/window_ui_modern.png";
            set_size(512,512);
        }
    };
    public NE_GUI_Healthbar(){
        w = 180;
        h = 19;
    }


    public void update(){
        if (Player.get_ent() != null && Player.get_ent().get_combat() != null){
            max_val = Player.get_ent().get_combat().get_max_hp();
            val = Player.get_ent().get_combat().get_hp();
        }
    }


    @Override
    public void render(){
        super.render();

        update();

        bar_sprite.set_rect(133, 0, 8, 19); //left part
        bar_sprite.render(get_x(), get_y() , 8 , h);

        int bar_w = w - 18;

        if (bar_w > 0){

            float percent = ( (float)val / max_val );
            
            bar_sprite.set_rect(151, 0, 3, 19); //active health
            bar_sprite.render(get_x() + 8, get_y(),
                    (int)(bar_w*percent), h);

            bar_sprite.set_rect(155, 0, 2, 19); //inactive health
            bar_sprite.render(get_x() + 8 + (int)(bar_w*percent), get_y(),
                    (int)(bar_w*(1-percent)), h);

            
        }

        bar_sprite.set_rect(143, 0, 8, 19); //right part
        bar_sprite.render(get_x() + w - 11, get_y(), 8, h);


    }
}
