package com.nuclearunicorn.serialkiller.generators;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.generators.ChunkGenerator;
import com.nuclearunicorn.libroguelike.utils.NLTimer;
import com.nuclearunicorn.serialkiller.game.ai.PedestrianAI;
import com.nuclearunicorn.serialkiller.game.ai.PoliceAI;
import com.nuclearunicorn.serialkiller.game.bodysim.BodySimulation;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.modes.in_game.InGameMode;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.RLWorldChunk;
import com.nuclearunicorn.serialkiller.game.world.RLWorldModel;
import com.nuclearunicorn.serialkiller.game.world.entities.*;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive.AdaptivePathfinder;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 */
public class TownChunkGenerator extends ChunkGenerator {

    private static final int NPC_PER_ROAD_RATE = 35;    //50% is a hell lot of npc , 35 is sorta ok
    private static final int MAX_POLICEMAN_COUNT = 4;

    enum RoomType {
        KITCHEN,
        BEDROOM,
        BATHROOM
    }


    int seed;
    Random chunk_random;

    List<Block> districts = null;
    List<Block> roads = new ArrayList<Block>();
    private static final int ROAD_SIZE = 3;

    //List<Block> apartments = new ArrayList<Block>();

    //TODO: replace with a class, lol
    //Map<Block,List<Block>> apartmentRooms = new HashMap<Block, List<Block>>();
    //Map<Block,List<Entity>> apartmentBeds = new HashMap<Block, List<Entity>>();

    RLWorldChunk chunk;

    /*
        List of Path nodes in the crossroads or corner points of town. Used for generation of patroling routes
    */

    public List<Apartment> getApartments(){
        return ((RLWorldModel)environment.getWorld()).getApartments();
    }

    public void generate(WorldChunk chunk){

        if (chunk instanceof RLWorldChunk){
            this.chunk = (RLWorldChunk)chunk;
        }else{
            throw new RuntimeException("trying to generate non-RLWorldChunk element");
        }

        seed = chunk.origin.getX()*10000 + chunk.origin.getY();
        chunk_random = new Random(seed);


        int x = chunk.origin.getX()* WorldChunk.CHUNK_SIZE;
        int y = chunk.origin.getY()*WorldChunk.CHUNK_SIZE;
        int size = WorldChunk.CHUNK_SIZE;

        final int OFFSET = WorldChunk.CHUNK_SIZE;

        for (int i = x - OFFSET; i<x+size+OFFSET; i++ ){
            for (int j = y - OFFSET; j<y+size+OFFSET; j++){
                if ( i>= x && i<x+size && j >=y && j < y+size){
                    addTile(i,j, chunk_random);
                }
            }
        }

        //Now, time to generate sum town

        Block gameBlock = new Block(
                x + 5,
                y + 5,
                WorldChunk.CHUNK_SIZE - 10 ,
                WorldChunk.CHUNK_SIZE - 10
        );

        MapGenerator mapgen = new MapGenerator(gameBlock);
        mapgen.setSeed(seed);

        List<Block> blocks = new ArrayList<Block>();
        blocks.add(gameBlock);

        districts = mapgen.process(blocks);

        for(Block district: districts){

            //register corner nodes before scaling and tracing roads

            Point[] ms = new Point[]{
                    new Point(district.getX(), district.getY()),
                    new Point(district.getX(), district.getY()+district.getH()),
                    new Point(district.getX()+district.getW(), district.getY()),
                    new Point(district.getX()+district.getW(), district.getY()+district.getH()),
            };
            for (Point milestone: ms){
                if (!this.chunk.hasMilestone(milestone)){
                    this.chunk.addMilestone(milestone);

                }
            }

            //AdaptivePathfinder.addPoint(this.chunk, milestone); //sub-optimal
            
            /*for(Point ms1 :this.chunk.getMilestones()){
                for(Point ms2: this.chunk.getMilestones()){

                }
            }*/

            generateRoads(district);
            district.scale(-ROAD_SIZE,-ROAD_SIZE);
        }

        //-----------------------------------------------------------
        //		randomly place safehouse
        //-----------------------------------------------------------
        Block safehouseBlock = districts.get(chunk_random.nextInt(districts.size()));

        Apartment safehouseBlockApt = new Apartment(safehouseBlock);
        getApartments().add(safehouseBlockApt);

        generateSafehouse(safehouseBlockApt);
        districts.remove(safehouseBlock);

        //-----------------------------------------------------------
        //		create other housing areas
        //-----------------------------------------------------------

        for(Block district: districts){
            fillBlock(district);
        }

        populateMap();

        for (Apartment apt : getApartments()){
            fillApartmentRooms(apt);
        }

        NLTimer graphTimer = new NLTimer();
        graphTimer.push();
        for (Point milestone: this.chunk.getMilestones()){
            AdaptivePathfinder.addPoint(this.chunk, milestone); //sub-optimal
        }
        //AdaptivePathfinder.buildGraph(this.chunk);  //finally, build graph
        graphTimer.pop("Adaptive graph generation");
    }

    /*
        Safehouse it the apartment block owned by a player.
     */
    private void generateSafehouse(Apartment safehouseBlock) {
        generateHousing(safehouseBlock);

        for (int i = 0; i <= safehouseBlock.getW(); i++)
            for (int j = 0; j <= safehouseBlock.getH(); j++){
                RLTile tile = (RLTile)(getLayer().get_tile(safehouseBlock.getX()+i,safehouseBlock.getY()+j));
                tile.setExplored(true);
            }
        //TODO: save safehouse

        //PLACE PLAYER
        InGameMode.spawn_player(new Point(0,0));

        //add family members
        FamilyGenerator familyGen = new FamilyGenerator();
        if (chunk_random.nextInt(100) <= 60){    //60% you have a mate

            //TODO: change family layout if you are a child

            //TODO: move to the separate generator

            Point origin = safehouseBlock.getFreeTile(chunk_random, getLayer());
            EntityRLHuman mate = NPCGenerator.generateNPC(chunk_random, this, origin.getX(), origin.getY());
            mate.age = NPCGenerator.generateAge(chunk_random, true);  //adult age

            //set correct mate sex
            if (((EntityRLHuman) Player.get_ent()).getSex() == EntityRLHuman.Sex.MALE){
                mate.setSex(EntityRLHuman.Sex.FEMALE);
            }else{
                mate.setSex(EntityRLHuman.Sex.MALE);
            }

            //set family surname & name
            Boolean isMale = mate.getSex() == EntityRLHuman.Sex.MALE;
            mate.setName(familyGen.generateName(isMale));

            ((EntityRLHuman) Player.get_ent()).setMate(mate);    //TODO: possible family relationship for monsters, etc. Inherite them from RLHuman?

            mate.set_combat(new RLCombat());
            mate.setBodysim(new BodySimulation());
        }

        //===========================
        //  children
        //===========================

        if (chunk_random.nextInt(100) <= 30){    //30% you have a one child
            Point origin = safehouseBlock.getFreeTile(chunk_random, getLayer());
            EntityRLHuman child = NPCGenerator.generateNPC(chunk_random, this, origin.getX(), origin.getY());
            child.age = NPCGenerator.generateAge(chunk_random, false);  //young age

            child.set_combat(new RLCombat());
            child.setBodysim(new BodySimulation());
        }

        fillApartmentRooms(safehouseBlock);

        //store safehouse to place player there later
        Point playerPosition = safehouseBlock.getFreeTile(chunk_random, getLayer());
        RLWorldModel.playerSafeHouseLocation = playerPosition;

        /*Point playerPosition = this.blockGetFreeTile(safehouseBlock);
        Player.get_ent().move_to(playerPosition);*/




        getApartments().remove(safehouseBlock);  //no one dares to live in my house!



    }

    private void fillApartmentRooms(Apartment apt) {

        List<Block> rooms = apt.rooms;
        if (rooms == null || rooms.isEmpty()){
            System.err.println("no rooms, failed to fill apartment");
            return;
        }

        //one kitchen
        Block kitchen = rooms.get(chunk_random.nextInt(rooms.size()));
        fillRoom(apt, kitchen, RoomType.KITCHEN);
        rooms.remove(kitchen);

        //one bathroom

        //multiple bedrooms

        int ownerCount = 1; //at least one owner
        RLTile sampleTile = (RLTile)(getLayer().get_tile(apt.getX()+1,apt.getY()+1));
        if (!sampleTile.getOwners().isEmpty()){
            ownerCount = sampleTile.getOwners().size();
        }

        for (int i = 0; i<ownerCount; i++){
            if (rooms == null || rooms.isEmpty()){
                return;
            }
            Block bedroom = rooms.get(chunk_random.nextInt(rooms.size()));
            fillRoom(apt, bedroom, RoomType.BEDROOM);

            //rooms.remove(kitchen);    //two ore more beds can be placed in same room, so do not remove it
        }

    }

    private void fillRoom(Apartment apt, Block room, RoomType type) {
        switch (type) {
            case KITCHEN:

                break;
            case BEDROOM:
                Point coord = room.getFreeTile(chunk_random, getLayer());

                EntityBed bed = new EntityBed();
                placeEntity(coord.getX(), coord.getY(), bed, "bed", "B", Color.green);
                bed.get_combat().set_hp(50);    //good wooden bed, hard to break >:3
                bed.set_blocking(false);    //npc can stand on the same tile

                /*if (apt.beds == null){
                    apartmentBeds.put(apt, new ArrayList<Entity>(3));
                }*/
                apt.beds.add(bed);

                break;
            case BATHROOM:

                break;
        }
    }

    private void generateRoads(Block block) {
        for ( List<Point> outerWall : block.getOuterWall(block) ){
            if( outerWall != null ){
                int wlen = outerWall.size()-1;
                int x = outerWall.get(0).getX();
                int y = outerWall.get(0).getY();
                int w = outerWall.get(wlen).getX()-x;
                int h = outerWall.get(wlen).getY()-y;

                Block road = new Block(x,y,w,h);
                road.scale(ROAD_SIZE-2,ROAD_SIZE-2);
                roads.add(road);

                for (int i = 0; i<=road.getW(); i++)
                    for (int j = 0; j<=road.getH(); j++){
                        RLTile tile = (RLTile)(getLayer().get_tile(road.getX()+i,road.getY()+j));

                        //TODO: use textures instead of broken pseudographics
                        //String model = new String(Character.toChars(178));
                        //tile.setModel(".");

                        tile.setModelColor(new Color(127,127,0));
                        tile.setTileType(RLTile.TileType.ROAD);

                    }

                //TODO : place road on a map
                /*
                    self.tiles[(road.x+i,road.y+j)].model = libtcod.CHAR_BLOCK3
                    self.tiles[(road.x+i,road.y+j)].color = libtcod.darker_yellow
                    self.tiles[(road.x+i,road.y+j)].road = True
                */

            }
        }
    }

    /**
     As legacy code says:
     #FILL MAP WITH NPC
     #this method should be called BEFORE room structure generation!
     */
    private void populateMap() {
        for (Block road: roads){

            //add pedestrians to the road

            int npcCount = 0;

            if (chunk_random.nextInt(100) < NPC_PER_ROAD_RATE){
                npcCount = 1;
            }
            for (int i = 0; i< npcCount; i++){
                Point coord = road.getFreeTile(chunk_random, getLayer());

                EntityRLHuman npc = (EntityRLHuman)placeNPC(coord.getX(), coord.getY());
                npc.set_ai(new PedestrianAI());
                npc.set_controller(new RLController());
                npc.set_combat(new RLCombat());

                int randomApt = chunk_random.nextInt(getApartments().size());
                Apartment apt = getApartments().get(randomApt);
                
                npc.setApartment(apt);

                for (int n = apt.getX(); n < apt.getX()+apt.getW(); n++)
                    for (int m = apt.getY(); m < apt.getY()+apt.getH(); m++){
                        RLTile tile = (RLTile)(getLayer().get_tile(n,m));
                        tile.addOwner(npc);
                    }

            }
        }

        //police
        
        for (int i=0; i<MAX_POLICEMAN_COUNT; i++){    //4 policemans
            Block road = roads.get(chunk_random.nextInt(roads.size()));
            Point coord = road.getFreeTile(chunk_random, getLayer());

            EntityRLHuman police = new EntityRLHuman();
            placeEntity(coord.getX(), coord.getY(), police, "Policeman", "P", new Color(127, 127, 255));

            police.set_ai(new PoliceAI());
            police.set_controller(new RLController());
            police.set_combat(new RLCombat());

            //TODO: add police equipment

            BaseItem vest = BaseItem.produce("Bulletproof est", 1);
            vest.set_slot("armor");
            vest.setEffect("defence", "60"); //60% damage resistance

            police.getContainer().add_item(vest);
            police.equipment.equip_item(vest);

            BaseItem stunstick = BaseItem.produce("Stunstick", 1);

            stunstick.set_slot("weapon");
            stunstick.setEffect("stun_chance", "40");
            stunstick.setEffect("damage", "5");
            stunstick.setEffect("damage_type", "dmg_blunt");

            police.getContainer().add_item(stunstick);
            police.equipment.equip_item(stunstick);


        }
    }

    void placeEntity(int x, int y, EntityRLActor entity, String symbol, String name, Color color) {
        placeEntity(x, y, entity, symbol, name);
        ((AsciiEntRenderer) entity.get_render()).setColor(color);
    }

    private void fillBlock(Block district){
        int chance = chunk_random.nextInt(100);
        if (chance > 20){

            Apartment apt = new Apartment(district);
            getApartments().add(apt);
            generateHousing(apt);

        }else{
            generatePark(district);
        }
    }

    private void generatePark(Block block) {
        //RLTile tile = (RLTile)(getLayer().get_tile(i,j));

        for(int i = 0; i<=block.getW(); i++ )
            for(int j = 0; j<=block.getH(); j++ ){
                if (chunk_random.nextInt(200) < 1){
                    EntityActor npc = placeNPC(block.getX()+i, block.getY()+j);
                    npc.set_ai(new PedestrianAI());
                    npc.set_controller(new RLController());
                    npc.set_combat(new RLCombat());
                }

                if (chunk_random.nextInt(100) < 2){
                    Entity tree = new Entity();
                    placeEntity(block.getX() + i, block.getY() + j, tree, "tree", "T");
                    //((RLTile)tree.tile).set
                }

                if (chunk_random.nextInt(100) < 15){
                    Entity grass = new Entity();
                    placeEntity(block.getX() + i, block.getY() + j, grass, "grass", "\"");
                    grass.set_blocking(false);
                }

            }
    }


    private EntityActor placeNPC(int x, int y  ) {
        EntityRLHuman playerEnt = NPCGenerator.generateNPC(chunk_random, this, x, y);

        return playerEnt;
    }

    /*
        Helper function. Place given entity at given point as ascii-art RL entity
     */
    private void placeEntity(int x, int y, Entity ent, String name, String symbol){
        ent.setName(name);
        ent.setEnvironment(environment);
        ent.setRenderer(new AsciiEntRenderer(symbol));
        ent.set_blocking(true);

        ent.setLayerId(z_index);
        ent.spawn(new Point(x,y));
    }

    private void generateHousing(Apartment block) {
        traceBlock(block);

        int ROOM_COUNT = 4;

        MapGenerator gen = new MapGenerator(block);
        gen.setMinBlockSize( block.getArea() / ROOM_COUNT );

        List<Block> housePrefab = new ArrayList<Block>();
        housePrefab.add(block);

        List<Block> rooms = gen.roomProcess(housePrefab,1);



        //TODO: extract method traceBlock
        for(Block room: rooms){
            traceBlock(room);
            room.clearNeighbours(); //so we could correctly generate rooms
        }

        /*
            This algorythm doesn't work with buggy corner rooms like

            -----------|
            |       ___|
            |      |   |
            |      |   |
            -------|---|

            Smaller room will result in pfanthom intersection wall.
            Altho this bug is invisible, we should probably assimilate smaller room by larger one
         */

        System.err.println("checking intersections from apartment " + block + ": iterating " + rooms.size() + " rooms");

        for (Block room : rooms){
            for(Block room2 : rooms){
                if(room != room2 && room.intersect(room2)){
                    if( !room.isConnected(room2) && !room2.isConnected(room)){ //transitive FTW?
                        Block intrs = room.getIntersection(room2);
                        if (intrs != null){


                            //------------room intersection debug start-------------
                            /*for(Point debug: intrs.getTiles()){
                                Entity debugActor = new EntityRLActor();

                                debugActor.setName("Room intersection");
                                debugActor.setEnvironment(environment);
                                debugActor.setRenderer(new AsciiEntRenderer("X", Color.red));

                                debugActor.setLayerId(z_index);
                                debugActor.spawn(new Point(debug.getX(),debug.getY()));
                            }*/
                            //------------debug end---------------

                            List<Point> wall = intrs.getTiles();
                            if (wall.size() > 0){
                                Point door_coord = wall.get(chunk_random.nextInt(wall.size()));
                                clearWall(door_coord.getX(), door_coord.getY());

                                EntityDoor door = new EntityDoor();
                                placeEntity(door_coord.getX(), door_coord.getY(), door, "door", "+", Color.green);
                                door.get_combat().set_hp(5);

                                door.unlock();

                                room.addNeighbour(room2);
                            }
                        }
                    }
                }
            }


            for (List<Point> outerWall : room.getOuterWall(block)){
                //int rndChar =
                if (outerWall != null && outerWall.size()>0){
                    Point windowCoord = outerWall.get(outerWall.size()/2);
                    if (chunk_random.nextInt(100) > 20){

                        //Window

                        EntityFurniture window = new EntityFurniture();
                        placeEntity(windowCoord.getX(), windowCoord.getY(), window, "window", "=", Color.green);
                        window.get_combat().set_hp(1);
                        window.setBlockSight(false);
                        clearWall(windowCoord.getX(), windowCoord.getY());


                    } else {

                        //Door

                        EntityDoor door = new EntityDoor();
                        placeEntity(windowCoord.getX(), windowCoord.getY(), door, "door", "+", Color.green);
                        door.get_combat().set_hp(5);
                        door.lock();
                        clearWall(windowCoord.getX(), windowCoord.getY());
                    }
                }
            }
        }

        block.rooms = rooms;
        //apartmentRooms.put(block, rooms);   //save apartment and rooms in in for later handling
    }



    /**
     Trace outer conture of block and mark every outer block as wall
     */
    private void traceBlock(Block block){
        for (int i = 0; i< block.getH()+1; i++){
            placeWall(block.getX(), block.getY()+i);
            placeWall(block.getX()+block.getW(), block.getY()+i);
        }
        for (int j = 0; j< block.getW()+1; j++){
            placeWall(block.getX()+j, block.getY());
            placeWall(block.getX()+j, block.getY()+block.getH());
        }
    }

    private void placeWall(int i, int j){
        RLTile tile = (RLTile)(getLayer().get_tile(i,j));
        tile.setWall(true);
        //TODO: add isBlockSight to RLTile

        //self.tiles[(x,y)].blocked = True
        //self.tiles[(x,y)].block_sight = True
    }

    private void clearWall(int i, int j) {
        RLTile tile = (RLTile)(getLayer().get_tile(i,j));
        tile.setWall(false);
    }


    private WorldTile addTile(int i, int j, Random chunk_random) {
        WorldTile tile = new RLTile();
        Point origin = new Point(i,j);
        tile.origin = origin;

        getLayer().set_tile(origin, tile);

        return tile;
    }
}