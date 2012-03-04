package com.nuclearunicorn.serialkiller.game.modes.in_game;

import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.EKeyPress;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.EventManager;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityPlayer;
import com.nuclearunicorn.libroguelike.game.ent.controller.PlayerController;
import com.nuclearunicorn.libroguelike.game.modes.AbstractGameMode;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.ui.IUserInterface;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldModel;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.game.world.WorldViewCamera;
import com.nuclearunicorn.libroguelike.game.world.generators.ChunkGenerator;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.libroguelike.render.TilesetRenderer;
import com.nuclearunicorn.libroguelike.render.layers.LayerRenderer;
import com.nuclearunicorn.libroguelike.render.overlay.DebugOverlay;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import com.nuclearunicorn.libroguelike.utils.NLTimer;
import com.nuclearunicorn.libroguelike.utils.Timer;
import com.nuclearunicorn.libroguelike.vgui.effects.EffectsSystem;
import com.nuclearunicorn.serialkiller.generators.TownChunkGenerator;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import com.nuclearunicorn.serialkiller.render.AsciiWorldView;
import com.nuclearunicorn.serialkiller.render.ConsoleRenderer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Point;

/**
 * Main game mode
 */
public class InGameMode extends AbstractGameMode implements IEventListener {

    private OverlaySystem overlay;
    IUserInterface wgt = null;

    private WorldView view;
    private WorldModel model;
    private EffectsSystem fx;


    private GameEnvironment clientGameEnvironment;
    
    @Override
    public void run() {
        overlay = new OverlaySystem();
        fx = new EffectsSystem();

        clientGameEnvironment = new GameEnvironment(){
            @Override
            public EventManager getEventManager(){
                return ClientEventManager.eventManager;
            }
        };
        ClientGameEnvironment.setEnvironment(clientGameEnvironment);

        model = new WorldModel(1);
        clientGameEnvironment.setWorld(model);


        ChunkGenerator townGenerator = new TownChunkGenerator();
        for(WorldLayer layer : model.getLayers()){
            layer.registerGenerator(townGenerator);
        }

        Timer.init();

        final LayerRenderer renderer = new ConsoleRenderer();
        WorldView.ISOMETRY_MODE = false;
        WorldView.ISOMETRY_ANGLE = 0.0f;
        WorldView.ISOMETRY_Y_SCALE = 1.0f;

        view  = new AsciiWorldView(model){
            public LayerRenderer getLayerRenderer(){
                return renderer;
            }
        };

        //tileset render affects camera positioning. Todo:fix this shit
        TilesetRenderer.TILE_SIZE = ConsoleRenderer.TILE_SIZE;

        WorldChunk.CHUNK_SIZE = 64;

        //finally, spawn player
        spawn_player(new Point(5, 5));
    }

    @Override
    public void update() {

        DebugOverlay.renderTime = 0;

        NLTimer timer = new NLTimer();
        timer.push();

        super.update();

        get_ui().update();
        model.update();
        fx.update();

        DebugOverlay.updateTime = timer.popDiff();
        timer.push();

        view.render();
        fx.render();
        get_ui().render();
        overlay.render();

        DebugOverlay.frameTime = timer.popDiff();

    }

    @Override
    public IUserInterface get_ui(){
        if (wgt!=null){
            return wgt;
        }
        wgt = new InGameUI();

        return wgt;
    }

    @Override
    public void e_on_event(Event event) {
        if (event instanceof EKeyPress){
            switch(((EKeyPress) event).key){
                case Keyboard.KEY_UP:
                    Player.move(Player.get_ent().x(),Player.get_ent().y()-1);
                break;
                case Keyboard.KEY_DOWN:
                    Player.move(Player.get_ent().x(),Player.get_ent().y()+1);
                break;
                case Keyboard.KEY_RIGHT:
                    Player.move(Player.get_ent().x()+1,Player.get_ent().y());
                    break;
                case Keyboard.KEY_LEFT:
                    Player.move(Player.get_ent().x()-1,Player.get_ent().y());
                    break;
            }
        }
    }

    void spawn_player(Point location){

        Entity playerEnt = new EntityPlayer();

        playerEnt.setName("Player");
        playerEnt.setEnvironment(clientGameEnvironment);
        playerEnt.setRenderer(new AsciiEntRenderer("@"));

        //TODO: extract player information from the event
        //clientGameEnvironment.getEntityManager().add(player_ent, Player.get_zindex());

        playerEnt.setLayerId(Player.get_zindex());
        playerEnt.spawn(12345, location);

        WorldViewCamera.target.setLocation(location);

        playerEnt.set_controller(new PlayerController());
        Player.set_ent(playerEnt);
    }
}
