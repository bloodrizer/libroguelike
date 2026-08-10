/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ent.controller;

import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.EEntityChangeChunk;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.events.network.EEntitySetPath;
import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.utils.NLTimer;
import com.nuclearunicorn.libroguelike.utils.Timer;
import com.nuclearunicorn.libroguelike.utils.pathfinder.astar.Mover;
import com.nuclearunicorn.libroguelike.utils.pathfinder.astar.implementation.AStarPathFinder;
import org.lwjgl.util.Point;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Administrator
 */
public class NpcController extends BaseController implements Mover, IEventListener {

    public Point destination = null;
    public List<Point> adaptivePathPool = new ArrayList<Point>(32);
    
    public static final int SYNCH_CHUNK_SIZE = 5;
    
    public List<Point> path = null;
    public Point step = null;


    //consecutive turns spent waiting for someone to step out of the way
    private int blocked_turns = 0;
    private static final int BLOCKED_PATIENCE = 3;

    //set while a caller is probing for a route it can do without, to keep the log honest
    protected boolean quietAstar = false;

    int path_synch_counter = 0;
    public int NEXT_FRAME_DELAY = 100;
    public float MOVE_SPEED = 0.50f;

    static final boolean ALLOW_DIAGONAL_MOVEMENT = false;

    //static int MAX_SEARCH_DISTANCE = 80;    //50 is fast, but not accurate, 80 is sorta-ok, 120+ is hell slow
    static int MAX_SEARCH_DISTANCE = 120;

    private AStarPathFinder finder;
    
    public static int pathfinderRequests = 0;
    public static long totalAstarCalculationTime = 0;

    public NpcController(){
        ClientEventManager.subscribe(this);

        finder = new AStarPathFinder( 
            ClientGameEnvironment.getWorldLayer(Player.get_zindex()).tile_map,
            MAX_SEARCH_DISTANCE,
            ALLOW_DIAGONAL_MOVEMENT);
    }

    public NpcController(GameEnvironment env){
        env.getEventManager().subscribe(this);
        
        finder = new AStarPathFinder(
            env.getWorldLayer(Player.get_zindex()).tile_map,
            50,
            ALLOW_DIAGONAL_MOVEMENT);
    }
    
    public boolean hasPath(){
        return (path != null && !path.isEmpty() && destination != null);
    }

    @Override
    public void think() {

    }

    public void set_destination(Point destination){

        path_synch_counter = 0; //reset synchronisation counter

        owner.next_frame = Timer.get_time();

        this.destination = destination;
        
        //path.calculate(destination)
        calculate_path(
                destination.getX(),
                destination.getY()
        );

    }

    /*
        Don't you ever dare to call this method, suckers. Use set_destionation() instead
     */
    private void calculate_path(int x, int y){

        path = getAstarPath(owner.origin.getX(), owner.origin.getY(), x, y);

        step = null;
        /*
         * There is a bug in the path calculation
         * First element is player position, so it's incorrect
         */
        if (path != null && path.size()>=1){
            path.remove(0);
        }
    }
    
    public List<Point> getAstarPath(int sx, int sy, int tx, int ty){
        
        Point source = new Point(sx, sy);
        source = ClientGameEnvironment.getWorldLayer(Player.get_zindex()).tile_map.world2local(source);

        Point target = new Point(tx, ty);
        target = ClientGameEnvironment.getWorldLayer(Player.get_zindex()).tile_map.world2local(target);

        return getAstarPath(source, target);
    }
    
    public List<Point> getAstarPath(Point source, Point target){
        //System.out.println("path calculation requested by entity #"+this.owner.get_uid());
        pathfinderRequests++;


        NLTimer astarTimer = new NLTimer();
        astarTimer.push();

        List<Point> astarPath = null;

        try {
            astarPath = finder.findPath(this,
                    source.getX(), source.getY(), target.getX(), target.getY());
        } catch(ArrayIndexOutOfBoundsException ex) {
            /*
            * This is a temorary workaround for astar pathfind.
            * If entity tries to pathfind outside of the loaded data, it's probably should be removed
            */
            ex.printStackTrace();
        }

        //A route longer than MAX_SEARCH_DISTANCE is a normal outcome, not a fault - it is why
        //the milestone pathfinder exists. Only say so when nothing is going to retry.
        if (astarPath == null && !quietAstar){
            System.err.println("Astar pathfinder failed from @"+source+" to @"+target
                    +" for ent '" + owner.get_uid() + "', took " + astarTimer.popDiff() + " ms");
        }

        NpcController.totalAstarCalculationTime += astarTimer.popDiff();
        
        return astarPath;
    }

    /*
     * THIS IS FNG BULLSHIT
     * MOVE THIS TO PLAYER CONTROLLER
     */
    public void change_tile(int x, int y){

        Point __step = null;

        owner.dx = 0.0f;
        owner.dy = 0.0f;
        /// owner.origin.setLocation(x, y);
        owner.move_to(new Point(x,y));

        step = null;



        //--------------------------------------------
        //ABSOLUTELY REQUIRED OR WEIRED SHIT WILL OCCUR
        if (path!=null && path.size()>0){
            path.remove(0);   //erase step
        }

    }

    protected void notify_path(Point __dest){

        System.out.println("Sending path notification");

        __dest = ClientGameEnvironment.getWorldLayer(Player.get_zindex()).tile_map.local2world(__dest);


        EEntitySetPath dest_event = new EEntitySetPath(owner, __dest);
        dest_event.setManager(ClientGameEnvironment.getEnvironment().getEventManager());

        dest_event.post();
    }


    public void move_ent(int x, int y){

        //WorldTile tile = ClientGameEnvironment.getWorldLayer(Player.get_zindex()).get_tile(x, y);
        WorldTile tile = owner.getLayer().get_tile(x, y);
        if (tile == null){
            return;
        }

        //wachky-hacky safe switch
        if(tile.isBlocked()){

            if (owner.tile == tile){
                step = null;
                if (path != null && path.size()>0){
                    path.remove(0);
                }
                return;
            }

            step = null;

            /*
             * Someone standing in the way is a queue, not a wall - they walk on. Throwing the
             * route away over it meant one neighbour in a doorway sent everybody behind him
             * looking for another way in, every single turn. Wait a few turns first; only a
             * genuine obstacle (or a queue that never clears) is worth re-routing for.
             */
            Entity blocker = tile.get_obstacle();
            if (blocker != null && blocker.controller != null){
                e_on_obstacle(x,y);     //still the melee trigger
                if (++blocked_turns < BLOCKED_PATIENCE){
                    return;             //give them a moment to walk on
                }
                blocked_turns = 0;
                if (squeezePast(blocker)){
                    return;
                }
                path = null;
                destination = null;
                return;
            }

            blocked_turns = 0;
            path = null;
            destination = null;

            e_on_obstacle(x,y);
            return;
        }

        blocked_turns = 0;

        //actually change tile

        //erase last visited adaptive path node
        //so in case of collision we will re-calculate path
        //using astar to the next node instead of resetting whole path
        if (adaptivePathPool.contains(step)){
            adaptivePathPool.remove(step);
        }

        //TODO: implement slow turn-based movement
        //TODO: implement policy settings
        change_tile(
                x,
                y
        );
    }


    /**
     * Someone has been standing in our way for {@link #BLOCKED_PATIENCE} turns. Whether we may
     * edge round them is a game rule, not an engine one — the default is no.
     */
    protected boolean squeezePast(Entity blocker){
        return false;
    }

    /**
     * Trade places with the entity in front of us. Two people meeting in a one-tile hallway is
     * otherwise a permanent standoff: each is the other's obstacle, neither route is wrong, and
     * no amount of re-planning helps because A* is right both times.
     *
     * <p>The blocker is lifted out of the way for the length of our own step, because
     * {@code EEntityMove} refuses to move onto an occupied tile — and once we have vacated,
     * theirs is free for them.
     */
    protected boolean swapWith(Entity blocker){
        Point mine = new Point(owner.origin);
        Point theirs = new Point(blocker.origin);

        boolean wasBlocking = blocker.is_blocking();
        blocker.set_blocking(false);
        owner.move_to(theirs);
        blocker.set_blocking(wasBlocking);

        if (!owner.origin.equals(theirs)){
            return false;   //the move was refused for some other reason; nothing has changed
        }
        blocker.move_to(mine);
        blocker.dx = 0.0f;
        blocker.dy = 0.0f;

        owner.dx = 0.0f;
        owner.dy = 0.0f;
        step = null;
        if (path != null && path.size()>0){
            path.remove(0);   //we are standing on it now
        }
        //their route started from the tile they no longer occupy
        if (blocker.controller instanceof NpcController){
            NpcController theirCtrl = (NpcController) blocker.controller;
            theirCtrl.step = null;
            theirCtrl.path = null;
            theirCtrl.blocked_turns = 0;
        }
        return true;
    }

    public void follow_path(){
        //safe switch
        if (destination == null){
            return;
        }

        Point __destination = new Point(this.destination);

        if (path!=null && path.size() > 0){


            if(step == null || step.equals(owner.origin)){  //this is safe hack, that hides the 'ent-lock' glitch
                step = path.get(0);
            }

            Point location = new Point(step.getX(),step.getY());
            location = ClientGameEnvironment.getWorldLayer(Player.get_zindex()).tile_map.local2world(location);

            

            __destination.setLocation(location.getX(), location.getY());
        }


        int dx = 0;
        int dy = 0;
        if(owner.origin.getX() > __destination.getX()){
            //(owner.origin.getX()-1, owner.origin.getY());
            dx = -1;
            //owner.orientation = Entity.Orientation.ORIENT_W;
        }
        if(owner.origin.getX() < __destination.getX()){
            //move_ent(owner.origin.getX()+1, owner.origin.getY());
            dx = 1;
            //owner.orientation = Entity.Orientation.ORIENT_E;
        }
        if(owner.origin.getY() > __destination.getY()){
            //move_ent(owner.origin.getX(), owner.origin.getY()-1);
            dy = -1;
            //owner.orientation = Entity.Orientation.ORIENT_N;
        }
        if(owner.origin.getY() < __destination.getY()){
            //move_ent(owner.origin.getX(), owner.origin.getY()+1);
            dy = 1;
            //owner.orientation = Entity.Orientation.ORIENT_S;
        }

        move_ent(owner.origin.getX()+dx, owner.origin.getY()+dy);
        


        if(/*owner.get_render() instanceof NPCRenderer &&*/ owner.origin.equals(destination)){      //strrrange point
            this.destination = null;    //clean up destination
            step = null;                //this is required too
            path = null;
            //((NPCRenderer)owner.get_render()).set_frame(0);
        }
    }

    /*
     * This is extremely important!!!!
     * When player crosses chunk border,
     * WorldModel.WorldModelTileMap coord anchor changes
     *
     * That means, ALL PREVIOUS PATH COORDINATES ARE INVALID!
     * We should recalculate them right away
     */

    public void e_on_event(Event event) {
        if (event instanceof EEntityChangeChunk){
            if (((EEntityChangeChunk)event).ent.isPlayerEnt() && destination!=null){
                calculate_path(destination.getX(),destination.getY());
            }
        }
    }

    public void e_on_event_rollback(Event event) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    public void e_on_obstacle(int x, int y) {
        //throw new UnsupportedOperationException("Not yet implemented");
    }


    //------------------------------------------------------------------------------------------------------------------

    public int distanceToTarget(Point target){

        int dx = target.getX() - owner.x();
        int dy = target.getY() - owner.y();
        return (int)Math.sqrt(dx * dx + dy * dy);
    }
    
    private Point getTargetDestinationVector(Point target){

        //vector from this object to the target, and distance
        int dx = target.getX() - owner.x();
        int dy = target.getY() - owner.y();
        double distance = Math.sqrt(dx * dx + dy * dy);

        //normalize it to length 1 (preserving direction), then round it and
        //convert to integer so the movement is restricted to the map grid
        
        dx = (int)(Math.round(dx/distance));
        dy = (int)(Math.round(dy/distance));
        
        return new Point(dx,dy);
    }

    public void escapeTarget(Entity target) {
        Point vec = getTargetDestinationVector(target.origin);

        //this.set_destination(new Point(owner.x() - vec.getX(), owner.y() - vec.getY()));

        move_ent(owner.x() - vec.getX(), owner.y());
        move_ent(owner.x(), owner.y()- vec.getY());
        //this.follow_path();
    }

    public void chaseTarget(Entity target) {
        Point vec = getTargetDestinationVector(target.origin);

        this.move_ent(owner.x() + vec.getX(), owner.y() + vec.getY());
    }

    public void clearPath() {
        path = null;
    }


}
