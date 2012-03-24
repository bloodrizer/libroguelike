package com.nuclearunicorn.serialkiller.game.ai;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.game.ai.BasicMobAI;
import com.nuclearunicorn.libroguelike.game.ai.IAIAction;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldTimer;
import com.nuclearunicorn.serialkiller.game.events.NPCWitnessCrimeEvent;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.RLWorldChunk;
import com.nuclearunicorn.serialkiller.game.world.entities.EntDoor;
import com.nuclearunicorn.serialkiller.game.world.entities.EntFurniture;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.generators.Apartment;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;

import java.util.ArrayList;
import java.util.List;

/*
    PEDESTRIAN AI - patroling from node to node like a policeman
*/
public class PedestrianAI extends BasicMobAI {

    public static final String AI_STATE_PATROLLING = "ai_state_PATROLLING";
    public static final String AI_STATE_ESCAPING = "ai_state_ESCAPING";
    public static final String AI_STATE_SLEEPING = "ai_state_SLEEPING";
    public static final String AI_STATE_TIRED = "ai_state_TIRED";

    List<EntityActor> knowCriminals = new ArrayList<EntityActor>();
    EntityActor nearestEnemy = null;



    public PedestrianAI(){
        super();

        registerState(AI_STATE_PATROLLING, new IAIAction() {
            @Override
            public void act(NpcController npcController) {
                actionPatroll(npcController);
            }
        });

        registerState(AI_STATE_ESCAPING, new IAIAction() {
            @Override
            public void act(NpcController npcController) {
                actionEscape(npcController);
            }
        });

        registerState(AI_STATE_TIRED, new IAIAction() {

            @Override
            public void act(NpcController npcController) {
                if (npcController == null){
                    return;
                }
                Apartment apt = ((EntityRLHuman) owner).getApartment();
                if (apt == null){
                    if (npcController.hasPath()){
                        npcController.clearPath();
                    }
                    return; //little optimization for homeless npc
                }

                //slooow

                //force NPC to switch old path and move to bed
                if (npcController.hasPath()){
                    WorldTile target = owner.getLayer().get_tile(npcController.destination);
                    Entity ent = target.get_actor();

                    /*
                        If we have path to target and target is not a bed (or it is, but it is occupied bed)
                     */

                    if (ent != null && (!(ent instanceof EntFurniture) || ent.tile.has_ent(EntityRLHuman.class)  )){
                    //todo: replace with EntBed
                        //System.out.println("NPC's target is not BED, re-calculating target");
                        npcController.clearPath();
                    }
                }

                //System.out.println("Checking if in bed");
                if (!npcController.hasPath()){
                    if (isInBed(npcController)){
                        state = AI_STATE_SLEEPING;
                    }else{
                        calculatePath(npcController);
                    }
                }
                //System.out.println("following path to @" + npcController.destination);
                if ((int)(Math.random()*100) >= 15){
                    npcController.follow_path();
                }
            }

            private void calculatePath(NpcController npcController) {
                //System.out.println("Calculating path to the bedroom");
                Apartment apt = ((EntityRLHuman) owner).getApartment();
                Entity bed = null;

                if (apt != null && apt.beds != null && apt.beds.size()>0){
                    /*while (bed != null){
                        bed = apt.beds.get((int) Math.random() * apt.beds.size());
                        if (bed.tile.has_ent(EntityRLHuman.class)){
                            bed = null;
                        }
                    }*/
                    for (Entity currBed: apt.beds){
                        if (!currBed.tile.has_ent(EntityRLHuman.class)){
                            bed = currBed;
                        }
                        break;
                    }
                    //System.out.println("Found my bed @" + bed.origin);
                }

                if (bed != null){
                    //System.out.println("Setting path to bed, halleluyah");
                    //npcController.calculate_path(bed.origin.getX(), bed.origin.getY());
                    npcController.set_destination(bed.origin);
                }
            }

            private boolean isInBed(NpcController npcController) {
                EntityRLHuman actor = (EntityRLHuman)owner;
                if (actor.getApartment() == null){
                    return false;
                }

                //((RLWorldModel)actor.getEnvironment().getWorld());

                return false;
            }

        });

    }


    private void actionPatroll(NpcController npcController) {
        if (npcController == null){
            return;
        }

        if (!npcController.hasPath()){
            getRandomRoute(npcController);
        }

        //add 10% chance of standing still, making it more natural for player to chase target
        if ((int)(Math.random()*100) >= 15){
            npcController.follow_path();
        }
    }

    private void actionEscape(NpcController npcController) {

        if (npcController == null){
            return;
        }

        int disst = npcController.distanceToTarget(nearestEnemy.origin);

        if (disst > 0 && disst < 10){
            npcController.escapeTarget(nearestEnemy);
        } else {
            //System.out.println("Invalid escape distance :"+disst+" to target '" + nearestEnemy.getName() + "'@"+nearestEnemy.origin);
        }

        if (Math.random()*100 > 60){

            RLMessages.message(owner.getName() + " screams!", Color.red);

            ((EntityActor)owner).say_message("AAAAAAAA!");
        }

    }

    private void getRandomRoute(NpcController npcController) {
        RLWorldChunk chunk = (RLWorldChunk)(this.owner.get_chunk());
        List<Point> mst = chunk.getMilestones();
        
        Point rndMilestone = mst.get((int) (Math.random() * mst.size()));
        
        //get random point near the milestone node (more natural ai behavior)
        Point point = new Point(
                rndMilestone.getX() + (int)Math.random()*4-2,
                rndMilestone.getY() + (int)Math.random()*4-2
        );

        npcController.set_destination(point);
    }


    @Override
    public void update(){
        //state = AI_STATE_ROAMING;

        /*

        /*if self.player_in_fov():

        if self.state == ai_state_SLEEPING:
        #if hear loud sound, than awake

        #otherwise:
        return


        #TODO: check if crimeplace is near and catch player
                player = self.owner.em.services["player"]
        if player.bloody and player.is_alive():
        #RUN FOR YOUR LIFE
        self.state = ai_state_ESCAPING*/

        if (WorldTimer.is_night()){
            if (state != AI_STATE_SLEEPING){
                state = AI_STATE_TIRED;
            }
        }else{
            state = AI_STATE_PATROLLING;
        }


        nearestEnemy = null;
        //just get some criminal in fov
        for (EntityActor enemy : knowCriminals){
            if (((RLTile)enemy.tile).isVisible()){
                nearestEnemy = enemy;

                state = AI_STATE_ESCAPING;
            }
        }

        //------------------------pseudocode time
        //IF NO PLAYER (OR CRIMINAL) IS IN FOV, THAN RESET TO PATROLLING
        //ELSE DO NOTHING


    }

    @Override
    public void e_on_event(Event event) {
        if (event instanceof NPCWitnessCrimeEvent){
            NPCWitnessCrimeEvent e = (NPCWitnessCrimeEvent)event;

            //TODO: pass criminal to the AI manager, so state will be reset only if no known criminal is in fov
            knowCriminals.add((EntityActor) e.criminal);
        }
    }

    @Override
    public void e_on_obstacle(int x, int y) {
        Entity actor = owner.getLayer().get_tile(x, y).get_actor();
        if (actor instanceof EntDoor){
            ((EntDoor)actor).unlock();
        }
        if (actor instanceof EntFurniture){
            owner.get_combat().inflict_damage(actor);
        }
    }
}
