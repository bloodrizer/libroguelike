package com.nuclearunicorn.serialkiller.game.modes.in_game;

import com.nuclearunicorn.libroguelike.utils.Rng;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.EKeyPress;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.EventManager;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.combat.Combat;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
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
import com.nuclearunicorn.libroguelike.render.overlay.DebugOverlay;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import com.nuclearunicorn.libroguelike.utils.NLTimer;
import com.nuclearunicorn.libroguelike.utils.Timer;
import com.nuclearunicorn.libroguelike.vgui.effects.EffectsSystem;
import com.nuclearunicorn.serialkiller.game.ItemFactory;
import com.nuclearunicorn.serialkiller.game.Main;
import com.nuclearunicorn.serialkiller.game.MainApplet;
import com.nuclearunicorn.serialkiller.game.SkillerGame;
import com.nuclearunicorn.libroguelike.core.replay.Replay;
import com.nuclearunicorn.serialkiller.game.ai.TownAI;
import com.nuclearunicorn.serialkiller.game.ai.PlayerAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.GameTurn;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.HearingSensor;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.CrimeSensor;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.PainSensor;
import com.nuclearunicorn.serialkiller.game.sound.PlayerEars;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.game.bodysim.BodySimulation;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import com.nuclearunicorn.serialkiller.game.controllers.RLPlayerController;
import com.nuclearunicorn.serialkiller.game.debug.TownScenario;
import com.nuclearunicorn.serialkiller.game.debug.WorldProbe;
import com.nuclearunicorn.serialkiller.game.social.SocialController;
import com.nuclearunicorn.serialkiller.game.world.RLWorldModel;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLPlayer;
import com.nuclearunicorn.serialkiller.generators.NPCGenerator;
import com.nuclearunicorn.serialkiller.generators.layerGenerators.BasementGenerator;
import com.nuclearunicorn.serialkiller.generators.layerGenerators.TownChunkGenerator;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import com.nuclearunicorn.serialkiller.render.AsciiWorldView;
import com.nuclearunicorn.serialkiller.render.RenderConfig;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.newdawn.slick.Color;
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

    // FPS-style talk mode: 't' opens it, printable keys build the line, Enter speaks it to
    // nearby NPCs, Escape cancels. While active, movement/attack keys are suppressed.
    private boolean typeMode = false;
    private StringBuilder typeBuffer = new StringBuilder();
    /** How far out the replay dumps NPC brain state each turn. */
    private static final int NPC_DUMP_RADIUS = 12;

    @Override
    public void run() {
        overlay = new OverlaySystem();
        fx = new EffectsSystem();

        clientGameEnvironment = new GameEnvironment("jskiller-client-game-environment"){
            @Override
            public EventManager getEventManager(){
                return ClientEventManager.getEventManager();
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

        //the world is a plain square grid; the pseudo-isometry lives in the
        //renderer's projection, not in a rotated tileset
        WorldView.ISOMETRY_MODE = false;
        WorldView.ISOMETRY_ANGLE = 0.0f;
        WorldView.ISOMETRY_Y_SCALE = 1.0f;

        view = new AsciiWorldView(model);

        //tileset render affects camera positioning. Todo:fix this shit
        TilesetRenderer.TILE_SIZE = RenderConfig.CELL;

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
        HearingSensor.init();
        PainSensor.init();
        CrimeSensor.init();
        PlayerEars.init();       //the player is a listener too, on the same acoustic terms

        WorldProbe.atReady();   //-Ddebug.world=ready: is this town one connected place?

        //scenario verbs for replay-driven sessions - see TownScenario
        TownScenario.install();
        TownScenario.TurnPump.bind(this::makeTurn);

        //world is built and taking input - anchor replay playback here, not at frame 0
        Replay.markReady();
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
        // Shift is hold-to-wait, so every shifted character typed in talk mode used to run the
        // world for as long as the key was down. Composing a line is not a turn; sending it is.
        if (!typeMode && (Input.key_state_shft || playerSkipTurn)){
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

        if (typeMode){
            OverlaySystem.ttf.drawString(15,
                    com.nuclearunicorn.libroguelike.render.WindowRender.get_window_h() - 30,
                    "Say: " + typeBuffer + "_", org.newdawn.slick.Color.yellow);
        }

        DebugOverlay.frameTime = timer.popDiff();

    }

    /** -Dlrl.fov=N pins the player's sight radius, for testing what memory looks like. */
    private static final int FOV_OVERRIDE = Integer.getInteger("lrl.fov", 0);

    private void fovUpdate() {
        model.resetFov();
        int fovRadius = FOV_OVERRIDE > 0 ? FOV_OVERRIDE
                : ((RLCombat)Player.get_ent().get_combat()).getFovRadius();
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

        // Every mode stays subscribed for good, so ignore input unless this one is the mode
        // on screen — otherwise keys pressed on the loading screen hit a world that doesn't
        // exist yet, and ESC pressed in the menu is handled here as well as there.
        if (!isCurrent()){
            return;
        }

        // Talk mode swallows all keyboard input until the line is sent or cancelled.
        if (typeMode && event instanceof EKeyPress){
            handleTypeMode((EKeyPress) event);
            return;
        }

        //allow to press ESC even if player is dead
        if (event instanceof EKeyPress){
            if (((EKeyPress) event).key == Keyboard.KEY_ESCAPE){
                //consume it: the menu we are switching to listens for ESC as well, and this
                //very event is still being handed down the listener chain
                event.dispatch();

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
                case Keyboard.KEY_T:
                    typeMode = true;
                    typeBuffer.setLength(0);
                break;
                //render mode toggles, handy for comparing the layers
                case Keyboard.KEY_F1:
                    RenderConfig.PIXEL_SPRITES = !RenderConfig.PIXEL_SPRITES;
                    RLMessages.message("pixel sprites: " + RenderConfig.PIXEL_SPRITES, Color.yellow);
                break;
                case Keyboard.KEY_F2:
                    RenderConfig.SMOOTH_LIGHT = !RenderConfig.SMOOTH_LIGHT;
                    RLMessages.message("smooth light: " + RenderConfig.SMOOTH_LIGHT, Color.yellow);
                break;
                case Keyboard.KEY_F3:
                    RenderConfig.ASCII_OVER_SPRITES = !RenderConfig.ASCII_OVER_SPRITES;
                    RLMessages.message("ascii overlay: " + RenderConfig.ASCII_OVER_SPRITES, Color.yellow);
                break;
                case Keyboard.KEY_F4:
                    RenderConfig.REVEAL = !RenderConfig.REVEAL;
                    RLMessages.message("reveal map: " + RenderConfig.REVEAL, Color.yellow);
                break;
            }
        }

        if (isNextTurn){
            makeTurn();
        }
    }

    /** Edit the talk buffer. Enter sends the line, Escape cancels, Backspace deletes. */
    private void handleTypeMode(EKeyPress e){
        switch (e.key){
            case Keyboard.KEY_ESCAPE:
                typeMode = false;
                typeBuffer.setLength(0);
                break;
            case Keyboard.KEY_RETURN:
                if (typeBuffer.length() > 0){
                    speakToNearby(typeBuffer.toString());
                }
                typeMode = false;
                typeBuffer.setLength(0);
                break;
            case Keyboard.KEY_BACK:
                if (typeBuffer.length() > 0){
                    typeBuffer.setLength(typeBuffer.length() - 1);
                }
                break;
            default:
                if (e.chr != Keyboard.CHAR_NONE && e.chr >= ' '){
                    typeBuffer.append(e.chr);
                }
        }
    }

    /**
     * Say the line and let the world hear it. Delivery is HearingSensor's job now — the
     * player is just another speaker, which is also what makes NPCs hear each other.
     * Speaking costs a turn, so the NPC being addressed actually gets to think.
     */
    private void speakToNearby(String text){
        ((EntityActor) Player.get_ent()).say_message(text);
        makeTurn();
    }

    void makeTurn(){
        turnNumber++;
        GameTurn.advance();
        WorldProbe.tick();   //-Ddebug.census=<n>: what is the whole town doing?
        NLTimer timer = new NLTimer();
        timer.push();

        model.update();

        timer.pop("Turn # "+turnNumber);
        recordTurn();
        System.out.println(NpcController.pathfinderRequests + " astar calls on this turn ");
        System.out.println("Total pure astar calculation time: " + NpcController.totalAstarCalculationTime + "ms");
        NpcController.pathfinderRequests = 0;
        NpcController.totalAstarCalculationTime = 0;
    }

    /**
     * Dump the turn and every nearby NPC brain into the replay. Cheap when not recording
     * (Replay.observe is a no-op), and it is the record that makes a "the NPC ignored me"
     * report diagnosable after the fact instead of a thing to re-enact by hand.
     */
    private void recordTurn(){
        if (!Replay.isRecording()){
            return;
        }
        Entity playerEnt = Player.get_ent();
        if (playerEnt == null){
            return;
        }
        Replay.observe("turn", "turn", turnNumber,
                "px", playerEnt.origin.getX(), "py", playerEnt.origin.getY());

        Entity[] nearby = com.nuclearunicorn.libroguelike.utils.Fov.get_entity_in_radius(
                playerEnt.getEnvironment().getEntityManager(),
                playerEnt.origin, NPC_DUMP_RADIUS, playerEnt.getLayerId());
        for (Entity ent : nearby){
            // Every human, not just the LLM-brained ones, and always name the AI class.
            // Filtering to LLMAgentAI hid the actual bug: the NPC standing next to the
            // player had no AI at all, so an empty dump read as "no NPC nearby" rather
            // than "the NPC nearby has no brain to ignore you with".
            if (ent == playerEnt || !(ent instanceof EntityRLHuman)){
                continue;
            }
            Replay.observe("npc", "turn", turnNumber, "uid", ent.get_uid(), "name", ent.getName(),
                    "x", ent.origin.getX(), "y", ent.origin.getY(),
                    "dist", (int) Math.sqrt(
                            Math.pow(ent.origin.getX() - playerEnt.origin.getX(), 2)
                          + Math.pow(ent.origin.getY() - playerEnt.origin.getY(), 2)),
                    "ai", ent.getAI() == null ? "none" : ent.getAI().getClass().getSimpleName(),
                    "state", ent.getAI() instanceof TownAI
                            ? ((TownAI) ent.getAI()).debugState() : "-");
        }
    }

    public static void spawn_player(Point location){

        EntityRLPlayer playerEnt = new EntityRLPlayer();
        playerEnt.set_combat(new RLCombat());

        playerEnt.setName("Player");
        //the same object run() installs, reached the way everything else reaches it: the
        //private static was only ever set by run(), so nothing could build a town without one
        playerEnt.setEnvironment(ClientGameEnvironment.getEnvironment());
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

        playerEnt.getContainer().add_item(ItemFactory.produce("valium"));

        BaseItem food = ItemFactory.produceFood("generic food", 10);
        food.set_count(5);
        playerEnt.getContainer().add_item(food);

        /*for (BaseItem item: playerEnt.container.getItems()){
            System.out.println("Player's item: " + item + " , container:" + item.get_container());
        }*/

        NPCGenerator.generateNPCStats(Rng.derive(Rng.COMBAT), playerEnt);
        playerEnt.setName("Player");
    }
}
