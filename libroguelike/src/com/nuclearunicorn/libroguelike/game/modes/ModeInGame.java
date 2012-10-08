/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.modes;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.EventManager;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.events.network.EPlayerSpawn;
import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityPlayer;
import com.nuclearunicorn.libroguelike.game.ent.controller.PlayerController;
import com.nuclearunicorn.libroguelike.game.ent.monsters.EntMonster;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.ui.GameUI;
import com.nuclearunicorn.libroguelike.game.ui.IUserInterface;
import com.nuclearunicorn.libroguelike.game.world.WorldModel;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.game.world.WorldViewCamera;
import com.nuclearunicorn.libroguelike.render.TilesetRenderer;
import com.nuclearunicorn.libroguelike.render.overlay.DebugOverlay;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import com.nuclearunicorn.libroguelike.utils.Timer;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_System;
import com.nuclearunicorn.libroguelike.vgui.effects.EffectsSystem;
import org.lwjgl.util.Point;

/**
 *
 * @author Administrator
 */
public class ModeInGame implements IGameMode, IEventListener {

    //private Tileset tileset = null;

    private TilesetRenderer bg_tileset;
    private WorldView view;
    private WorldModel model;
    private NE_GUI_System gui;
    private EffectsSystem fx;


    private GameEnvironment clientGameEnvironment;


    private OverlaySystem overlay;
    
    public ModeInGame(){
        ClientEventManager.subscribe(this);
    }

    public void run(){
        bg_tileset = new TilesetRenderer();
        view  = new WorldView(null);
        //model = new WorldModel();

        clientGameEnvironment = new GameEnvironment("libroguelike-client-game-environment"){
            @Override
            public EventManager getEventManager(){
                return ClientEventManager.getEventManager();
            }
        };
        ClientGameEnvironment.setEnvironment(clientGameEnvironment);

        model = clientGameEnvironment.getWorld();
                //ClientWorld.getWorld();

        overlay = new OverlaySystem();

        fx = new EffectsSystem();

        //gui = new NE_GUI_System();

        Timer.init();   //very-very critical

        //synchronize with server
        //init world

        
    }

    void spawn_player(EPlayerSpawn event){
        
        Point location = event.origin;
        
        Entity player_ent = new EntityPlayer();
        player_ent.setName(Player.characterInfo.name);
        
        player_ent.setEnvironment(clientGameEnvironment);
        //player_ent.setName(event.charInfo.name);
        
        //TODO: extract player information from the event
        
        clientGameEnvironment.getEntityManager().add(player_ent, Player.get_zindex());
        player_ent.spawn(location);

        WorldViewCamera.target.setLocation(location);

        player_ent.set_controller(new PlayerController());
        //player_ent.set_combat(new BasicCombat());

        Player.set_ent(player_ent);
    }




    public void update(){

        Input.update();
        ClientEventManager.update();
        model.update();
        fx.update();

        view.render();
        fx.render();

        get_ui().update();
        get_ui().render();

        DebugOverlay.debugPathfinding();

        overlay.render();
    }

    IUserInterface ui = null;
    public IUserInterface get_ui(){
        //return new GameUI();
        if (ui == null){
            ui = new GameUI();
        }
        return ui;
    }

    //--------------------------------------------------------------------------
    public void e_on_event(Event event){

       if (event instanceof EPlayerSpawn){
           spawn_player(((EPlayerSpawn)event));
       }
       else if(event instanceof EMouseClick){
           e_on_mouse_click(((EMouseClick)event));
       }
       else
       {
           //System.out.println("Unknown message registered:" + event.classname());   //debug
       }
    }

    public void e_on_mouse_click( EMouseClick event){
        Point tile_origin = view.getTileCoord(event.origin);

        //System.out.println(tile_origin);
        if (event.type == Input.MouseInputType.LCLICK) {
            WorldTile tile = ClientGameEnvironment.getWorldLayer(Player.get_zindex()).get_tile(tile_origin.getX(), tile_origin.getY());
            
            if(tile == null){
                return;
            }

            Entity ent = tile.get_actor();
            if (ent != null && ent instanceof EntMonster){
                Player.attack(ent);
            }else{
                Player.move(tile_origin);
            }
        }
        //todo: use Player.player_ent.controller.set_target(tile_origin);
    }
    //--------------------------------------------------------------------------
    public void e_on_event_rollback(Event event){
        
    }

    private Object EPlayerLogon(Event event) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
