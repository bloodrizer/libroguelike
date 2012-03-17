package com.nuclearunicorn.serialkiller.game.modes.in_game;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.EKeyPress;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.EventManager;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.game.ent.controller.PlayerController;
import com.nuclearunicorn.libroguelike.game.modes.AbstractGameMode;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.ui.IUserInterface;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldCluster;
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
import com.nuclearunicorn.serialkiller.game.ItemFactory;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import com.nuclearunicorn.serialkiller.game.world.RLWorldModel;
import com.nuclearunicorn.serialkiller.game.world.entities.EntRLPlayer;
import com.nuclearunicorn.serialkiller.generators.TownChunkGenerator;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import com.nuclearunicorn.serialkiller.render.AsciiWorldView;
import com.nuclearunicorn.serialkiller.render.ConsoleRenderer;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Point;
import rlforj.los.IFovAlgorithm;
import rlforj.los.PrecisePermissive;

/**
 * Main game mode
 */
public class InGameMode extends AbstractGameMode implements IEventListener {

    private OverlaySystem overlay;
    IUserInterface wgt = null;

    private WorldView view;
    private RLWorldModel model;
    private EffectsSystem fx;

    //IFovAlgorithm fov = new ShadowCasting();
    IFovAlgorithm fov = new PrecisePermissive();


    private GameEnvironment clientGameEnvironment;
    private int turnNumber = 0;

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
        RLMessages.setEventManager(clientGameEnvironment.getEventManager());

        model = new RLWorldModel(1);
        clientGameEnvironment.setWorld(model);

        WorldCluster.CLUSTER_SIZE = 1;


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

        WorldChunk.CHUNK_SIZE = 128;
        
        spawn_player(new Point(0,0));

        
        //hack begin

        /**
         * This ugly hack fixes player position.
         * First, we create player entity, as it requeres local environment
         * Then, we force game to make player turn in order to detect chunks and pre-build them
         * As an effect of chunk building, generator sets player location inside of generated safehouses
         * Then, we finally able to use pre-generated player and place it into the safehouse
         */
        model.update();

        if (RLWorldModel.playerSafeHouseLocation != null){
            Player.get_ent().move_to(RLWorldModel.playerSafeHouseLocation);
        }
        //hack end
    }

    @Override
    public void update() {

        DebugOverlay.renderTime = 0;

        NLTimer timer = new NLTimer();
        timer.push();

        super.update();

        get_ui().update();

        //debug-only timeskip
        if (Input.key_state_shft){
            model.update();
        }

        fx.update();

        fovUpdate();

        DebugOverlay.updateTime = timer.popDiff();
        timer.push();

        view.render();
        fx.render();
        get_ui().render();

        DebugOverlay.debugPathfinding();    //heavy, but very useful
        overlay.render();

        DebugOverlay.frameTime = timer.popDiff();

    }

    private void fovUpdate() {
        model.resetFov();
        int fovRadius = ((RLCombat)Player.get_ent().get_combat()).getFovRadius();
        fov.visitFieldOfView(model, Player.get_ent().x(), Player.get_ent().y(), fovRadius);
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
        boolean isNextTurn = false;

        if (event instanceof EKeyPress){
            switch(((EKeyPress) event).key){
                case Keyboard.KEY_UP: case Keyboard.KEY_W:
                    Player.move(Player.get_ent().x(),Player.get_ent().y()-1);
                    isNextTurn = true;
                break;
                case Keyboard.KEY_DOWN: case Keyboard.KEY_S:
                    Player.move(Player.get_ent().x(),Player.get_ent().y()+1);
                    isNextTurn = true;
                break;
                case Keyboard.KEY_RIGHT: case Keyboard.KEY_A:
                    Player.move(Player.get_ent().x()+1,Player.get_ent().y());
                    isNextTurn = true;
                break;
                case Keyboard.KEY_LEFT: case Keyboard.KEY_D:
                    Player.move(Player.get_ent().x()-1,Player.get_ent().y());
                    isNextTurn = true;
                break;
                case Keyboard.KEY_SPACE:
                    isNextTurn = true;
                break;
            }
        }

        if (isNextTurn){
            makeTurn();
        }
    }

    void makeTurn(){
        turnNumber++;
        NLTimer timer = new NLTimer();
        timer.push();

        model.update();

        timer.pop("Turn # "+turnNumber);
        System.out.println(NpcController.pathfinderRequests + " astar calls on this turn ");
        NpcController.pathfinderRequests = 0;
    }

    void spawn_player(Point location){

        EntRLPlayer playerEnt = new EntRLPlayer();
        playerEnt.set_combat(new RLCombat());

        playerEnt.setName("Player");
        playerEnt.setEnvironment(clientGameEnvironment);
        playerEnt.setRenderer(new AsciiEntRenderer("@"));

        //TODO: extract player information from the event
        //clientGameEnvironment.getEntityManager().add(player_ent, Player.get_zindex());

        playerEnt.setLayerId(Player.get_zindex());
        playerEnt.spawn(location);

        WorldViewCamera.target.setLocation(location);

        playerEnt.set_controller(new PlayerController());
        Player.set_ent(playerEnt);

        //---------------------------------------------------
        //TODO: use some kind of item factory
        playerEnt.container.add_item(ItemFactory.produce("hammer"));
        playerEnt.container.add_item(ItemFactory.produce("knife"));
    }
}
