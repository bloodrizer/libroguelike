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
import com.nuclearunicorn.libroguelike.render.NPCRenderer;
import com.nuclearunicorn.libroguelike.utils.Timer;
import com.nuclearunicorn.libroguelike.utils.pathfinder.astar.AStarPathFinder;
import com.nuclearunicorn.libroguelike.utils.pathfinder.astar.Mover;
import com.nuclearunicorn.libroguelike.utils.pathfinder.astar.Path;
import org.lwjgl.util.Point;

/**
 *
 * @author Administrator
 */
public class NpcController extends BaseController implements Mover, IEventListener {

    public Point destination = null;
    public static final int SYNCH_CHUNK_SIZE = 5;
    
    public Path path = null;
    public Point step = null;


    int path_synch_counter = 0;
    public int NEXT_FRAME_DELAY = 100;
    public float MOVE_SPEED = 0.50f;

    static final boolean ALLOW_DIAGONAL_MOVEMENT = true;
    private AStarPathFinder finder;

    public NpcController(){
        ClientEventManager.eventManager.subscribe(this);

        finder = new AStarPathFinder(
            ClientGameEnvironment.getWorldLayer(Player.get_zindex()).tile_map,
            50,
            ALLOW_DIAGONAL_MOVEMENT);
    }

    public NpcController(GameEnvironment env){
        env.getEventManager().subscribe(this);
        
        finder = new AStarPathFinder(
            env.getWorldLayer(Player.get_zindex()).tile_map,
            50,
            ALLOW_DIAGONAL_MOVEMENT);
    }

    @Override
    public void think() {
        if (destination != null){

            owner.set_next_frame(NEXT_FRAME_DELAY);

            follow_path();
            return;
        }

        owner.set_next_frame(200000);
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

    public void calculate_path(int x, int y){

        Point target = new Point(x,y);
        target = ClientGameEnvironment.getWorldLayer(Player.get_zindex()).tile_map.world2local(target);

        Point source = new Point(owner.origin);
        source = ClientGameEnvironment.getWorldLayer(Player.get_zindex()).tile_map.world2local(source);

        //WorldModel.clearVisited();
        try{
            path = finder.findPath(this,
                source.getX(), source.getY(), target.getX(), target.getY());
        }
        catch(ArrayIndexOutOfBoundsException ex){
            /*
             * This is a temorary workaround for astar pathfind.
             * If entity tries to pathfind outside of the loaded data, it's probably should be removed
             */
            path = null;
        }
        
        step = null;
        /*
         * There is a bug in the path calculation
         * First element is player position, so it's incorrect
         */
        if (path != null && path.getLength()>=1){
            path.steps.remove(0);
        }
    }

    /*
     * THIS IS FNG BULLSHIT
     * MOVE THIS TO PLAYER CONTROLLER
     */
    public void change_tile(int x, int y){

        Point __step = null;

        //------------path synchronisation-----------
        /*if (path == null){
            throw new RuntimeException("trying to change tile for an null path object(?)");
        }*/

        //Path synchronisation is deprecated

        /*System.out.println("changing tile, path_synch_counter is: "+path_synch_counter+", length is: "+path.getLength());

        if (owner.isPlayerEnt() && path != null){
            if (path_synch_counter == 0){    //we are in the point of
                
                
                System.out.println("synch counter is 0, notifying");

                //we have some trajectory left
                if (path.getLength()>0){
                    //extract checkpoint step
                    if (path.getLength()>SYNCH_CHUNK_SIZE){
                        __step = path.getStep(SYNCH_CHUNK_SIZE);
                    }else{
                        __step = path.getStep(path.getLength()-1);
                    }

                    notify_path(__step);
                }
            }
            path_synch_counter++;
            if (path_synch_counter>=SYNCH_CHUNK_SIZE){
                path_synch_counter = 0;
            }
        }*/

        owner.dx = 0.0f;
        owner.dy = 0.0f;
        /// owner.origin.setLocation(x, y);
        owner.move_to(new Point(x,y));

        step = null;



        //--------------------------------------------
        //ABSOLUTELY REQUIRED OR WEIRED SHIT WILL OCCUR
        if (path!=null && path.getLength()>0){
            path.steps.remove(0);   //erase step
        }

    }

    private void notify_path(Point __dest){

        System.out.println("Sending path notification");

        __dest = ClientGameEnvironment.getWorldLayer(Player.get_zindex()).tile_map.local2world(__dest);


        EEntitySetPath dest_event = new EEntitySetPath(owner, __dest);
        dest_event.setManager(ClientGameEnvironment.getEnvironment().getEventManager());
        System.out.println("posting event");
        dest_event.post();
    }


    public void move_ent(int x, int y){

        WorldTile tile = ClientGameEnvironment.getWorldLayer(Player.get_zindex()).get_tile(x, y);
        if (tile == null){
            return;
        }

        //wachky-hacky safe switch
        if(tile.isBlocked()){
            step = null;
            path = null;
            destination = null;

            e_on_obstacle(x,y);
            return;
        }

        //owner.move_to(new Point(owner.origin.getX()-1, owner.origin.getY()));

        //displacement = 1.0f / (owner.get_renderer().ANIMATION_LENGTH-2)   //1 start frame + 1 end frame + iterated animation
        float dx = (float)(x-owner.origin.getX())*MOVE_SPEED * tile.get_speed_modifier();
        float dy = (float)(y-owner.origin.getY())*MOVE_SPEED * tile.get_speed_modifier();

        owner.dx += dx;
        owner.dy += dy;

        //todo: use single % counter to maisure if to change tile or not

        if (owner.dx > 1.0f || owner.dx < -1.0f || owner.dy > 1.0f || owner.dy < -1.0f){
            //change_tile(x,y);
            change_tile(
                    owner.x()+(int)owner.dx,
                    owner.y()+(int)owner.dy
            );
            return;
        }
    }


    public void follow_path(){
        Point __destination = new Point(this.destination);

        if (path!=null && path.getLength() > 0){


            if(step == null || step.equals(owner.origin)){  //this is safe hack, that hides the 'ent-lock' glitch
                step = path.getStep(0);
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
            owner.orientation = Entity.Orientation.ORIENT_W;
        }
        if(owner.origin.getX() < __destination.getX()){
            //move_ent(owner.origin.getX()+1, owner.origin.getY());
            dx = 1;
            owner.orientation = Entity.Orientation.ORIENT_E;
        }
        if(owner.origin.getY() > __destination.getY()){
            //move_ent(owner.origin.getX(), owner.origin.getY()-1);
            dy = -1;
            owner.orientation = Entity.Orientation.ORIENT_N;
        }
        if(owner.origin.getY() < __destination.getY()){
            //move_ent(owner.origin.getX(), owner.origin.getY()+1);
            dy = 1;
            owner.orientation = Entity.Orientation.ORIENT_S;
        }

        move_ent(owner.origin.getX()+dx, owner.origin.getY()+dy);
        


        if(owner.get_render() instanceof NPCRenderer && owner.origin.equals(destination)){
            this.destination = null;    //clean up destination
            step = null;                //this is required too
            path = null;
            ((NPCRenderer)owner.get_render()).set_frame(0);
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
       //throw new UnsupportedOperationException("Not supported yet.");
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
}
