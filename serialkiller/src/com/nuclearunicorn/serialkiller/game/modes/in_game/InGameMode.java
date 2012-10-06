package com.nuclearunicorn.serialkiller.game.modes.in_game;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.EKeyPress;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.EventManager;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.combat.Combat;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.modes.AbstractGameMode;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.ui.IUserInterface;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldCluster;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.game.world.WorldViewCamera;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.libroguelike.render.TilesetRenderer;
import com.nuclearunicorn.libroguelike.render.layers.AbstractLayerRenderer;
import com.nuclearunicorn.libroguelike.render.overlay.DebugOverlay;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import com.nuclearunicorn.libroguelike.utils.NLTimer;
import com.nuclearunicorn.libroguelike.utils.Timer;
import com.nuclearunicorn.libroguelike.vgui.effects.EffectsSystem;
import com.nuclearunicorn.serialkiller.game.ItemFactory;
import com.nuclearunicorn.serialkiller.game.Main;
import com.nuclearunicorn.serialkiller.game.MainApplet;
import com.nuclearunicorn.serialkiller.game.SkillerGame;
import com.nuclearunicorn.serialkiller.game.ai.PlayerAI;
import com.nuclearunicorn.serialkiller.game.bodysim.BodySimulation;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import com.nuclearunicorn.serialkiller.game.controllers.RLPlayerController;
import com.nuclearunicorn.serialkiller.game.social.SocialController;
import com.nuclearunicorn.serialkiller.game.world.RLWorldModel;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLPlayer;
import com.nuclearunicorn.serialkiller.generators.NPCGenerator;
import com.nuclearunicorn.serialkiller.generators.layerGenerators.BasementGenerator;
import com.nuclearunicorn.serialkiller.generators.layerGenerators.TownChunkGenerator;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import com.nuclearunicorn.serialkiller.render.AsciiWorldView;
import com.nuclearunicorn.serialkiller.render.ConsoleRenderer;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Point;
import rlforj.los.IFovAlgorithm;
import rlforj.los.PrecisePermissive;

import java.util.ArrayList;
import java.util.Random;

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


    private static GameEnvironment clientGameEnvironment;
    private int turnNumber = 0;

    @Override
    public void run() {
        overlay = new OverlaySystem();
        fx = new EffectsSystem();

        clientGameEnvironment = new GameEnvironment("jskiller-client-game-environment"){
            @Override
            public EventManager getEventManager(){
                return ClientEventManager.eventManager;
            }
        };
        ClientGameEnvironment.setEnvironment(clientGameEnvironment);
        RLMessages.setEventManager(clientGameEnvironment.getEventManager());

        model = new RLWorldModel(5);
        clientGameEnvironment.setWorld(model);

        WorldCluster.CLUSTER_SIZE = 1;

        //ChunkGenerator townGenerator = new TownChunkGenerator();
        ArrayList<WorldLayer> layers = new ArrayList<WorldLayer>(model.getLayers());

        layers.get(0).registerGenerator(new TownChunkGenerator());
        for (int i = 1; i<5; i++){
            layers.get(i).registerGenerator(new BasementGenerator());
        }
        //layers.get(1).registerGenerator(new BasementGenerator());

        /*for(WorldLayer layer : model.getLayers()){
            layer.registerGenerator(townGenerator);
        }*/

        Timer.init();

        final AbstractLayerRenderer renderer = new ConsoleRenderer();
        WorldView.ISOMETRY_MODE = false;
        WorldView.ISOMETRY_ANGLE = 0.0f;
        WorldView.ISOMETRY_Y_SCALE = 1.0f;

        view  = new AsciiWorldView(model){
            public AbstractLayerRenderer getLayerRenderer(){
                return renderer;
            }
        };

        //tileset render affects camera positioning. Todo:fix this shit
        TilesetRenderer.TILE_SIZE = ConsoleRenderer.TILE_SIZE;

        WorldChunk.CHUNK_SIZE = 128;
        
        //spawn_player(new Point(0,0));

        
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

        //loading misc services
        SocialController.init();
    }

    @Override
    public void update() {

        DebugOverlay.renderTime = 0;

        NLTimer timer = new NLTimer();
        timer.push();

        super.update();

        get_ui().update();

        boolean playerSkipTurn = false;
        if (Player.get_ent() != null){
            Combat combat = Player.get_ent().get_combat();
            if (combat != null && !combat.is_alive()){
                playerSkipTurn = true;
            }
            BodySimulation bodysim = ((EntityRLPlayer)Player.get_ent()).getBodysim();
            if (bodysim != null && ( bodysim.isStunned() || bodysim.isFainted() ) ){
                playerSkipTurn = true;
            }
            PlayerAI ai = (PlayerAI) Player.get_ent().getAI();
            if (ai.isOutOfControl()){
                playerSkipTurn = true;
            }
        }
        if (Input.key_state_shft || playerSkipTurn){
            model.update();
        }

        fx.update();

        fovUpdate();

        DebugOverlay.updateTime = timer.popDiff();
        timer.push();

        view.render();
        fx.render();
        get_ui().render();


        //DebugPathfindingGraph.debugAdaptiveGraph(); //>:3
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

        //allow to press ESC even if player is dead
        if (event instanceof EKeyPress){
            if (((EKeyPress) event).key == Keyboard.KEY_ESCAPE){
                SkillerGame game;
                if (Main.game != null){
                    game = Main.game;
                }else{
                    game = MainApplet.game;
                }
                game.set_state("mainMenu");
                game.initStateUI();
                return;
            }
        }

        if (Player.get_ent() != null){
            Combat combat = Player.get_ent().get_combat();
            if (combat != null && !combat.is_alive()){
                return;
            }
            BodySimulation bodysim = ((EntityRLPlayer)Player.get_ent()).getBodysim();
            if (bodysim != null && bodysim.isStunned()){
                return;
            }
        }

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
                case Keyboard.KEY_LEFT: case Keyboard.KEY_A:
                    Player.move(Player.get_ent().x()-1,Player.get_ent().y());
                    isNextTurn = true;
                break;
                case Keyboard.KEY_RIGHT: case Keyboard.KEY_D:
                    Player.move(Player.get_ent().x()+1,Player.get_ent().y());
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
        System.out.println("Total pure astar calculation time: " + NpcController.totalAstarCalculationTime + "ms");
        NpcController.pathfinderRequests = 0;
        NpcController.totalAstarCalculationTime = 0;
    }

    public static void spawn_player(Point location){

        EntityRLPlayer playerEnt = new EntityRLPlayer();
        playerEnt.set_combat(new RLCombat());

        playerEnt.setName("Player");
        playerEnt.setEnvironment(clientGameEnvironment);
        playerEnt.setRenderer(new AsciiEntRenderer("@"));

        //TODO: extract player information from the event
        //clientGameEnvironment.getEntityManager().add(player_ent, Player.get_zindex());

        playerEnt.setLayerId(Player.get_zindex());
        playerEnt.spawn(location);

        WorldViewCamera.target.setLocation(location);

        playerEnt.set_controller(new RLPlayerController());
        playerEnt.set_ai(new PlayerAI());
        Player.set_ent(playerEnt);

        //---------------------------------------------------
        playerEnt.getContainer().add_item(ItemFactory.produce("hammer"));
        playerEnt.getContainer().add_item(ItemFactory.produce("knife"));
        playerEnt.getContainer().add_item(ItemFactory.produce("taser"));

        playerEnt.getContainer().add_item(ItemFactory.produce("suppressive pills"));

        BaseItem food = ItemFactory.produceFood("generic food", 10);
        food.set_count(5);
        playerEnt.getContainer().add_item(food);

        /*for (BaseItem item: playerEnt.container.getItems()){
            System.out.println("Player's item: " + item + " , container:" + item.get_container());
        }*/

        NPCGenerator.generateNPCStats(new Random(), playerEnt);
        playerEnt.setName("Player");
    }
}
