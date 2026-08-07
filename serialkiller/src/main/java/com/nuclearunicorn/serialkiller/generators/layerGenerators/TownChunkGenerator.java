package com.nuclearunicorn.serialkiller.generators.layerGenerators;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.generators.ChunkGenerator;
import com.nuclearunicorn.libroguelike.utils.NLTimer;
import com.nuclearunicorn.serialkiller.game.ItemFactory;
import com.nuclearunicorn.serialkiller.game.ai.LLMAgentAI;
import com.nuclearunicorn.serialkiller.game.ai.PedestrianAI;
import com.nuclearunicorn.serialkiller.game.ai.PoliceAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmRuntime;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.modes.in_game.InGameMode;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.RLWorldChunk;
import com.nuclearunicorn.serialkiller.game.world.RLWorldModel;
import com.nuclearunicorn.serialkiller.game.world.entities.*;
import com.nuclearunicorn.serialkiller.generators.*;
import com.nuclearunicorn.serialkiller.generators.town.Building;
import com.nuclearunicorn.serialkiller.generators.town.BuildingLayout;
import com.nuclearunicorn.serialkiller.generators.town.BuildingTemplate;
import com.nuclearunicorn.serialkiller.generators.town.BuildingTemplates;
import com.nuclearunicorn.serialkiller.generators.town.BuildingType;
import com.nuclearunicorn.serialkiller.generators.town.Footprint;
import com.nuclearunicorn.serialkiller.generators.town.FootprintGenerator;
import com.nuclearunicorn.serialkiller.generators.town.GridMask;
import com.nuclearunicorn.serialkiller.generators.town.Lot;
import com.nuclearunicorn.serialkiller.generators.town.LotSplitter;
import com.nuclearunicorn.serialkiller.generators.town.Room;
import com.nuclearunicorn.serialkiller.generators.town.RoomSplitter;
import com.nuclearunicorn.serialkiller.generators.town.RoomType;
import com.nuclearunicorn.serialkiller.generators.town.TownGenConfig;
import com.nuclearunicorn.serialkiller.generators.town.TypeSelector;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive.AdaptivePathfinder;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 */
public class TownChunkGenerator extends ChunkGenerator {

    private static final int NPC_PER_ROAD_RATE = 35;    //50% is a hell lot of npc , 35 is sorta ok
    private static final int MAX_POLICEMAN_COUNT = 4;

    int seed;
    Random chunk_random;

    List<Block> districts = null;
    List<Block> roads = new ArrayList<Block>();
    private static final int ROAD_SIZE = 3;

    //per-chunk building-type picker and the police station (if one was placed)
    private TypeSelector typeSelector;
    private Building policeStation;

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


        int x = chunk.origin.getX() * WorldChunk.CHUNK_SIZE;
        int y = chunk.origin.getY() * WorldChunk.CHUNK_SIZE;
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
        mapgen.setMinBlockSize(TownGenConfig.DISTRICT_MIN_AREA);   //smaller, denser districts

        List<Block> blocks = new ArrayList<Block>();
        blocks.add(gameBlock);

        districts = mapgen.process(blocks);

        typeSelector = new TypeSelector();
        policeStation = null;

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

            generateRoads(district);
            district.scale(-ROAD_SIZE,-ROAD_SIZE);

            //sidewalk ring + street furniture, then shrink 1 so lots sit inside it
            traceSidewalk(district);
            district.scale(-1,-1);
        }

        //-----------------------------------------------------------
        //		split districts into lots (some districts stay parks)
        //-----------------------------------------------------------
        List<Lot> lots = new ArrayList<Lot>();
        for (int di = 0; di < districts.size(); di++){
            Block district = districts.get(di);
            //never let the whole chunk become parks: force the last district to
            //be housing if nothing else was, so a safehouse (and player) can spawn
            boolean forceHousing = lots.isEmpty() && di == districts.size() - 1;
            if (!forceHousing && chunk_random.nextInt(100) < TownGenConfig.PARK_DISTRICT_PCT){
                generatePark(district);
            } else {
                lots.addAll(LotSplitter.splitIntoLots(district, chunk_random));
            }
        }

        //-----------------------------------------------------------
        //		randomly place safehouse on one lot
        //-----------------------------------------------------------
        if (!lots.isEmpty()){
            Lot safehouseLot = lots.remove(chunk_random.nextInt(lots.size()));
            Building safehouse = new Building(safehouseLot);
            safehouse.type = BuildingType.APARTMENT;        //family/bed logic needs this
            typeSelector.forceRecord(BuildingType.APARTMENT);
            getApartments().add(safehouse);
            generateSafehouse(safehouse, safehouseLot);
        }

        //-----------------------------------------------------------
        //		create other buildings (data-driven type per lot)
        //-----------------------------------------------------------
        for (Lot lot : lots){
            Building building = new Building(lot);
            building.type = typeSelector.pick(lot.getW(), lot.getH(), chunk_random);
            getApartments().add(building);
            generateBuilding(building, lot, false);
        }

        populateMap();

        //furnish: residential homes get beds/kitchen, commercial gets per-type props
        for (Apartment apt : getApartments()){
            if (apt instanceof Building && !((Building)apt).isResidential()){
                furnishCommercial((Building)apt);
            } else {
                fillApartmentRooms(apt);
            }
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
    private void generateSafehouse(Building safehouseBlock, Lot lot) {
        generateBuilding(safehouseBlock, lot, true);

        for (int i = 0; i <= safehouseBlock.getW(); i++)
            for (int j = 0; j <= safehouseBlock.getH(); j++){
                RLTile tile = (RLTile)(getLayer().get_tile(safehouseBlock.getX()+i,safehouseBlock.getY()+j));
                tile.setExplored(true);
            }
        //TODO: save safehouse

        //PLACE PLAYER
        InGameMode.spawn_player(new Point(0,0));
        EntityRLHuman playerEnt = ((EntityRLHuman) Player.get_ent());

        //add family members. One FamilyGenerator = one surname, and everyone in the
        //household wears it - the player included, who was literally named "Player" while
        //the mate carried an unrelated generated surname and the children a third and
        //fourth. The safehouse is also their home; without setApartment they are homeless
        //and every "goto home" they plan resolves to UNRESOLVED.
        FamilyGenerator familyGen = new FamilyGenerator();
        playerEnt.setName(familyGen.generateName(playerEnt.getSex() == EntityRLHuman.Sex.MALE));
        moveIn(playerEnt, safehouseBlock);

        Point mateOrigin = safehouseBlock.getFreeTileSafe(chunk_random, getLayer());
        if (mateOrigin != null && chunk_random.nextInt(100) <= 60){    //60% you have a mate

            //TODO: change family layout if you are a child

            //TODO: move to the separate generator

            Point origin = mateOrigin;
            EntityRLHuman mate = NPCGenerator.generateNPC(chunk_random, this, origin.getX(), origin.getY());
            mate.age = NPCGenerator.generateAge(chunk_random, true);  //adult age

            //set correct mate sex
            if (playerEnt.getSex() == EntityRLHuman.Sex.MALE){
                mate.setSex(EntityRLHuman.Sex.FEMALE);
            }else{
                mate.setSex(EntityRLHuman.Sex.MALE);
            }

            //set family surname & name
            Boolean isMale = mate.getSex() == EntityRLHuman.Sex.MALE;
            mate.setName(familyGen.generateName(isMale));

            playerEnt.setMate(mate);    //TODO: possible family relationship for monsters, etc. Inherite them from RLHuman?

            giveBrain(mate, "mate");
            moveIn(mate, safehouseBlock);
            //mate.setBodysim(new BodySimulation());
        }

        //===========================
        //  children
        //===========================

        Point child1Origin = safehouseBlock.getFreeTileSafe(chunk_random, getLayer());
        if (child1Origin != null && chunk_random.nextInt(100) <= 40){    //40% you have a one child
            Point origin = child1Origin;
            EntityRLHuman child = NPCGenerator.generateNPC(chunk_random, this, origin.getX(), origin.getY());
            child.age = NPCGenerator.generateAge(chunk_random, false);  //young age
            child.setName(familyGen.generateName(child.getSex() == EntityRLHuman.Sex.MALE));

            giveBrain(child, "child");
            moveIn(child, safehouseBlock);
            //child.setBodysim(new BodySimulation());

            playerEnt.addChild(child);
        }

        Point child2Origin = safehouseBlock.getFreeTileSafe(chunk_random, getLayer());
        if (child2Origin != null && chunk_random.nextInt(100) <= 15){    //15% you have a second child
            Point origin = child2Origin;
            EntityRLHuman child = NPCGenerator.generateNPC(chunk_random, this, origin.getX(), origin.getY());
            child.age = NPCGenerator.generateAge(chunk_random, false);  //young age
            child.setName(familyGen.generateName(child.getSex() == EntityRLHuman.Sex.MALE));

            giveBrain(child, "child");
            moveIn(child, safehouseBlock);
            //child.setBodysim(new BodySimulation());

            playerEnt.addChild(child);
        }

        fillApartmentRooms(safehouseBlock, true);

        //store safehouse to place player there later
        Point playerPosition = safehouseBlock.getFreeTileSafe(chunk_random, getLayer());
        if (playerPosition == null){
            playerPosition = new Point(safehouseBlock.getX()+1, safehouseBlock.getY()+1);
        }
        RLWorldModel.playerSafeHouseLocation = playerPosition;

        /*Point playerPosition = this.blockGetFreeTile(safehouseBlock);
        Player.get_ent().move_to(playerPosition);*/




        getApartments().remove(safehouseBlock);  //no one dares to live in my house!



    }

    private void fillApartmentRooms(Apartment apt) {
        fillApartmentRooms(apt, false);
    }

    private void fillApartmentRooms(Apartment apt, boolean isSafehouse) {

        List<Block> rooms = apt.rooms;
        if (rooms == null || rooms.isEmpty()){
            System.err.println("no rooms, failed to fill apartment");
            return;
        }

        int ownerCount = 1; //at least one owner
        if (apt instanceof Building){
            ownerCount = Math.max(1, ((Building)apt).residentCount);
        } else {
            RLTile sampleTile = (RLTile)(getLayer().get_tile(apt.getX()+1,apt.getY()+1));
            if (sampleTile != null && !sampleTile.getOwners().isEmpty()){
                ownerCount = sampleTile.getOwners().size();
            }
        }

        //Spread functions across distinct rooms when there are enough; in a small
        //(one/two-room) home the same room doubles up. `free` holds not-yet-used
        //rooms; takeRoom() prefers those but falls back to any room so a bed is
        //ALWAYS placed for every resident.
        List<Block> free = new ArrayList<Block>(rooms);

        //one bed per resident (guaranteed)
        for (int i = 0; i < ownerCount; i++){
            Block bedroom = takeRoom(free, rooms);
            fillRoom(apt, bedroom, RoomType.BEDROOM);
        }

        //one kitchen (prefers a room without a bed, else shares)
        Block kitchen = takeRoom(free, rooms);
        fillRoom(apt, kitchen, RoomType.KITCHEN);

        //one storeroom -> descending ladder (40% for a normal building, always for safehouse)
        int ladderChance = isSafehouse ? 100 : 40;
        if (chunk_random.nextInt(100) < ladderChance){
            Block storeroom = takeRoom(free, rooms);
            fillRoom(apt, storeroom, RoomType.STOREROOM);
        }
    }

    /**
     * Remove and return a random room from {@code free} to spread furniture out;
     * when {@code free} is exhausted, return a random room from {@code all} so
     * small homes reuse a room rather than skipping placement.
     */
    private Block takeRoom(List<Block> free, List<Block> all) {
        if (!free.isEmpty()){
            return free.remove(chunk_random.nextInt(free.size()));
        }
        return all.get(chunk_random.nextInt(all.size()));
    }

    /** Residential furnishing by explicit role (bed/kitchen/store roles are chosen
     *  dynamically by {@link #fillApartmentRooms}, not from room.type). */
    private void fillRoom(Apartment apt, Block room, RoomType type) {
        switch (type) {
            case KITCHEN:
                placeFridge(room);
                break;
            case BEDROOM:
                placeBed(apt, room);
                break;
            case BATHROOM:
                placeBathtub(room);
                break;
            case STOREROOM:
                placeCrates(room, 1 + chunk_random.nextInt(2));
                placeLadder(room);
                break;
            default:
                break;
        }
    }

    //-----------------------------------------------------------------------
    //  Commercial / civic furnishing (TOWN_GENERATION_DESIGN.md 5.6)
    //-----------------------------------------------------------------------

    /** Furnish a non-residential building room-by-type, then drop in a little staff. */
    private void furnishCommercial(Building building) {
        for (Room room : building.roomList){
            furnishRoom(building, room);
        }

        //0-2 staff NPCs milling about. These are the NPCs the player meets *indoors* -
        //including in the building they start in - so they get the same brain pedestrians
        //do. They were hardwired to PedestrianAI, which is why the nearest NPC at spawn
        //ignored both speech and being attacked: it had no LLM brain to sense either.
        int staff = chunk_random.nextInt(3);
        for (int i = 0; i < staff; i++){
            Point coord = interiorFreeTile(building);
            if (coord == null){ break; }
            EntityRLHuman npc = (EntityRLHuman) placeNPC(coord.getX(), coord.getY());
            giveBrain(npc, "staff");
        }
    }

    private void furnishRoom(Building building, Room room) {
        if (room.type == null){
            return;
        }
        switch (room.type) {
            case KITCHEN:
                placeFridge(room);
                break;
            case BEDROOM:
            case PRIVATE_ROOM:
            case CELL:
                placeBed(building, room);
                break;
            case BATHROOM:
                placeBathtub(room);
                break;
            case STOREROOM:
            case BACKROOM:
                placeCrates(room, 1 + chunk_random.nextInt(3));
                break;
            case LIVING_ROOM:
                placeSofa(room);
                break;
            case LOBBY:
            case RECEPTION:
                placeDesk(room, "reception desk");
                break;
            case OFFICE_ROOM:
                placeDesk(room, "desk");
                if (chunk_random.nextInt(100) < 50){ placeDesk(room, "desk"); }
                break;
            case MANAGER_OFFICE:
                placeDesk(room, "desk");
                if (chunk_random.nextInt(100) < 50){ placeSafe(room, false); }
                break;
            case VAULT:
                int safes = 2 + chunk_random.nextInt(3);   //2-4 safes with cash
                for (int i = 0; i < safes; i++){ placeSafe(room, true); }
                break;
            case SHOP_FLOOR:
                int shelves = 2 + chunk_random.nextInt(3);
                for (int i = 0; i < shelves; i++){ placeShelf(room); }
                placeCounter(room);
                break;
            case CORRIDOR:
            default:
                break;   //corridors stay walkable
        }
    }

    //-----------------------------------------------------------------------
    //  Furniture placement primitives
    //-----------------------------------------------------------------------

    private void placeFridge(Block room) {
        Point coord = wallAdjacentFreeTile(room);
        if (coord == null){ return; }
        EntityFurniture fridge = new EntityFurniture();
        placeEntity(coord.getX(), coord.getY(), fridge, "Fridge", "F", Color.green);
        fridge.get_combat().set_hp(20);
        BaseItem food = ItemFactory.produceFood("generic food", 10);
        food.set_count(10);
        fridge.getContainer().add_item(food);
    }

    private void placeBed(Apartment apt, Block room) {
        Point coord = wallAdjacentFreeTile(room);
        if (coord == null){ return; }
        EntityBed bed = new EntityBed();
        placeEntity(coord.getX(), coord.getY(), bed, "bed", "B", Color.green);
        bed.get_combat().set_hp(50);    //good wooden bed, hard to break >:3
        bed.set_blocking(false);        //npc can stand on the same tile
        apt.beds.add(bed);
    }

    private void placeLadder(Block room) {
        Point coord = room.getFreeTileSafe(chunk_random, getLayer());
        if (coord == null){ return; }
        EntLadder ladder = new EntLadder(); //desc ladder
        placeEntity(coord.getX(), coord.getY(), ladder, "ladder", ">", Color.green);
        ladder.setDescending(true);
        //save ladder position to generate underlaying rooms later
        System.out.println("adding ladder position for layer #"+getLayer().get_zindex());
        BasementGenerator.addLadder(getLayer().get_zindex(), coord);
    }

    private void placeBathtub(Block room) {
        Point coord = wallAdjacentFreeTile(room);
        if (coord == null){ return; }
        EntityFurniture tub = new EntityFurniture();
        placeEntity(coord.getX(), coord.getY(), tub, "bathtub", "b", new Color(220, 220, 235));
        tub.get_combat().set_hp(30);
    }

    private void placeCrates(Block room, int count) {
        for (int i = 0; i < count; i++){
            Point coord = wallAdjacentFreeTile(room);
            if (coord == null){ return; }
            EntityFurniture crate = new EntityFurniture();
            placeEntity(coord.getX(), coord.getY(), crate, "crate", "x", new Color(150, 110, 60));
            crate.get_combat().set_hp(15);
        }
    }

    private void placeSofa(Block room) {
        Point coord = wallAdjacentFreeTile(room);
        if (coord == null){ return; }
        EntityFurniture sofa = new EntityFurniture();
        placeEntity(coord.getX(), coord.getY(), sofa, "sofa", "n", new Color(120, 90, 160));
        sofa.get_combat().set_hp(25);
        sofa.set_blocking(false);
    }

    private void placeDesk(Block room, String name) {
        Point coord = wallAdjacentFreeTile(room);
        if (coord == null){ return; }
        EntityFurniture desk = new EntityFurniture();
        placeEntity(coord.getX(), coord.getY(), desk, name, "d", new Color(170, 130, 80));
        desk.get_combat().set_hp(20);
        //a chair beside the desk, if there's room
        Point chair = wallAdjacentFreeTile(room);
        if (chair != null){
            EntityFurniture seat = new EntityFurniture();
            placeEntity(chair.getX(), chair.getY(), seat, "chair", "h", new Color(140, 100, 60));
            seat.get_combat().set_hp(8);
            seat.set_blocking(false);
        }
    }

    private void placeSafe(Block room, boolean withMoney) {
        Point coord = wallAdjacentFreeTile(room);
        if (coord == null){ return; }
        EntityFurniture safe = new EntityFurniture();
        placeEntity(coord.getX(), coord.getY(), safe, "safe", "$", new Color(200, 190, 90));
        safe.get_combat().set_hp(60);
        if (withMoney){
            safe.getContainer().add_item(ItemFactory.produceMoney(100 + chunk_random.nextInt(400)));
        }
    }

    private void placeShelf(Block room) {
        Point coord = wallAdjacentFreeTile(room);
        if (coord == null){ return; }
        EntityFurniture shelf = new EntityFurniture();
        placeEntity(coord.getX(), coord.getY(), shelf, "shelf", "s", new Color(160, 140, 90));
        shelf.get_combat().set_hp(15);
    }

    private void placeCounter(Block room) {
        Point coord = wallAdjacentFreeTile(room);
        if (coord == null){ return; }
        EntityFurniture counter = new EntityFurniture();
        placeEntity(coord.getX(), coord.getY(), counter, "counter", "c", new Color(150, 120, 80));
        counter.get_combat().set_hp(20);
    }

    /** A random free interior tile of the building (prefers non-corridor rooms). */
    private Point interiorFreeTile(Building building) {
        List<Room> rooms = building.roomList;
        if (rooms == null || rooms.isEmpty()){
            return null;
        }
        for (int attempt = 0; attempt < 8; attempt++){
            Room r = rooms.get(chunk_random.nextInt(rooms.size()));
            if (r.isCorridor()){ continue; }
            Point p = r.getFreeTileSafe(chunk_random, getLayer());
            if (p != null){ return p; }
        }
        return null;
    }

    /**
     * A free interior tile next to a wall (furniture-against-walls look,
     * TOWN_GENERATION_DESIGN.md 5.6). Falls back to any free interior tile.
     */
    private Point wallAdjacentFreeTile(Block room) {
        List<Point> candidates = new ArrayList<Point>();
        for (int i = 1; i < room.getW(); i++){
            for (int j = 1; j < room.getH(); j++){
                int wx = room.getX() + i, wy = room.getY() + j;
                if (isFreeFloor(wx, wy) && nextToWall(wx, wy)){
                    candidates.add(new Point(wx, wy));
                }
            }
        }
        if (candidates.isEmpty()){
            return room.getFreeTileSafe(chunk_random, getLayer());
        }
        return candidates.get(chunk_random.nextInt(candidates.size()));
    }

    private boolean isFreeFloor(int x, int y) {
        RLTile tile = (RLTile)(getLayer().get_tile(x, y));
        return tile != null && !tile.isWall() && !tile.isBlocked();
    }

    private boolean nextToWall(int x, int y) {
        return isWallTile(x - 1, y) || isWallTile(x + 1, y)
            || isWallTile(x, y - 1) || isWallTile(x, y + 1);
    }

    private boolean isWallTile(int x, int y) {
        RLTile tile = (RLTile)(getLayer().get_tile(x, y));
        return tile != null && tile.isWall();
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
     * Mark the district's current perimeter ring as sidewalk (rendered as road)
     * and dot lampposts along it. Called after roads are traced and before the
     * district is shrunk by 1 so the ring sits between street and building lots.
     */
    private void traceSidewalk(Block d) {
        int x = d.getX(), y = d.getY(), w = d.getW(), h = d.getH();

        for (int i = 0; i <= w; i++){
            markSidewalk(x + i, y);
            markSidewalk(x + i, y + h);
        }
        for (int j = 0; j <= h; j++){
            markSidewalk(x, y + j);
            markSidewalk(x + w, y + j);
        }

        int step = TownGenConfig.LAMPPOST_SPACING;
        for (int i = 0; i <= w; i += step){
            placeLamppost(x + i, y);
            placeLamppost(x + i, y + h);
        }
        for (int j = step; j < h; j += step){
            placeLamppost(x, y + j);
            placeLamppost(x + w, y + j);
        }
    }

    private void markSidewalk(int i, int j) {
        RLTile tile = (RLTile)(getLayer().get_tile(i, j));
        if (tile != null && !tile.isWall()){
            tile.setTileType(RLTile.TileType.ROAD);
            tile.setModelColor(new Color(90, 90, 90));
        }
    }

    private void placeLamppost(int i, int j) {
        RLTile tile = (RLTile)(getLayer().get_tile(i, j));
        if (tile == null || tile.isWall()){
            return;
        }
        EntityFurniture lamp = new EntityFurniture();
        placeEntity(i, j, lamp, "lamppost", "i", new Color(210, 210, 140));
        lamp.get_combat().set_hp(30);
        lamp.setBlockSight(false);
    }

    /**
     As legacy code says:
     #FILL MAP WITH NPC
     #this method should be called BEFORE room structure generation!
     */
    private void populateMap() {
        //residents live only in APARTMENT buildings (5.7); if a chunk somehow has
        //no residential building, fall back to any apartment so we never nextInt(0).
        List<Apartment> residential = getResidentialApartments();

        for (Block road: roads){

            //add pedestrians to the road

            int npcCount = 0;

            if (chunk_random.nextInt(100) < NPC_PER_ROAD_RATE){
                npcCount = 1;
            }
            for (int i = 0; i< npcCount; i++){
                Point coord = road.getFreeTileSafe(chunk_random, getLayer());
                if (coord == null){ continue; }

                EntityRLHuman npc = (EntityRLHuman)placeNPC(coord.getX(), coord.getY());
                //Every pedestrian is an LLM agent when inference is enabled; the reactor
                //self-throttles to the near bucket so far NPCs cost nothing (§11).
                giveBrain(npc, "agent");

                if (residential.isEmpty()){ continue; }   //nowhere to live (all parks)
                Apartment apt = residential.get(chunk_random.nextInt(residential.size()));

                npc.setApartment(apt);
                stampOwnership(apt, npc);
            }
        }

        //police — spawn at the police station lobby if one exists, else on roads
        List<Point> policePosts = policeSpawnPoints();

        for (int i=0; i<MAX_POLICEMAN_COUNT; i++){    //4 policemans
            Point coord = null;
            if (!policePosts.isEmpty()){
                coord = policePosts.get(chunk_random.nextInt(policePosts.size()));
            } else if (!roads.isEmpty()){
                Block road = roads.get(chunk_random.nextInt(roads.size()));
                coord = road.getFreeTileSafe(chunk_random, getLayer());
            }
            if (coord == null){ continue; }

            EntityRLHuman police = new EntityRLHuman();
            placeEntity(coord.getX(), coord.getY(), police, "Policeman", "P", new Color(127, 127, 255));

            if (LlmRuntime.isEnabled()){
                police.set_ai(new LLMAgentAI());
                com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug.log(
                        "spawned LLM police %s at %d,%d", police.get_uid(), coord.getX(), coord.getY());
            }else{
                police.set_ai(new PoliceAI());
            }
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

    /** Apartments that actually house residents (APARTMENT type / legacy Apartment). */
    private List<Apartment> getResidentialApartments() {
        List<Apartment> res = new ArrayList<Apartment>();
        for (Apartment apt : getApartments()){
            if (!(apt instanceof Building) || ((Building)apt).isResidential()){
                res.add(apt);
            }
        }
        if (res.isEmpty()){
            res.addAll(getApartments());   //degrade gracefully, never nextInt(0)
        }
        return res;
    }

    /** Free interior tiles of the police station lobby, if a station was built. */
    private List<Point> policeSpawnPoints() {
        List<Point> posts = new ArrayList<Point>();
        if (policeStation == null || policeStation.roomList == null){
            return posts;
        }
        for (Room room : policeStation.roomList){
            if (room.type != RoomType.LOBBY){
                continue;
            }
            for (int i = 1; i < room.getW(); i++){
                for (int j = 1; j < room.getH(); j++){
                    int wx = room.getX() + i, wy = room.getY() + j;
                    if (isFreeFloor(wx, wy)){
                        posts.add(new Point(wx, wy));
                    }
                }
            }
        }
        return posts;
    }

    public void placeEntity(int x, int y, EntityRLActor entity, String symbol, String name, Color color) {
        placeEntity(x, y, entity, symbol, name);
        ((AsciiEntRenderer) entity.get_render()).setColor(color);
    }

    /**
     * Mark every interior floor tile of the building as owned by {@code npc}.
     * For a {@link Building} this walks the footprint mask (so only real floor,
     * not the yard or the lot rect, is stamped); otherwise falls back to the
     * legacy rect stamp.
     */
    private void stampOwnership(Apartment apt, EntityRLHuman npc) {
        if (apt instanceof Building && ((Building)apt).footprint != null){
            Building b = (Building)apt;
            GridMask m = b.footprint;
            for (int lx = 0; lx < m.w; lx++){
                for (int ly = 0; ly < m.h; ly++){
                    if (m.isInterior(lx, ly)){
                        RLTile tile = (RLTile)(getLayer().get_tile(m.ox + lx, m.oy + ly));
                        if (tile != null){
                            tile.addOwner(npc);
                        }
                    }
                }
            }
            b.residentCount++;
        } else {
            for (int n = apt.getX(); n < apt.getX()+apt.getW(); n++)
                for (int m = apt.getY(); m < apt.getY()+apt.getH(); m++){
                    RLTile tile = (RLTile)(getLayer().get_tile(n,m));
                    if (tile != null){
                        tile.addOwner(npc);
                    }
                }
        }
    }

    private void generatePark(Block block) {
        //RLTile tile = (RLTile)(getLayer().get_tile(i,j));

        for(int i = 0; i<=block.getW(); i++ )
            for(int j = 0; j<=block.getH(); j++ ){
                if (chunk_random.nextInt(200) < 1){
                    EntityActor npc = placeNPC(block.getX()+i, block.getY()+j);
                    giveBrain(npc, "loiterer");
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

    /** Move a resident in: somewhere to sleep, and somewhere {@code goto home} resolves to. */
    private void moveIn(EntityRLHuman resident, Apartment apartment) {
        resident.setApartment(apartment);
        stampOwnership(apartment, resident);
    }

    /**
     * Give an NPC the standard brain+body: an LLM agent when inference is on, the legacy
     * FSM otherwise. Every spawn site must go through here — the four sites used to each
     * roll their own, and the one that forgot (the player's own family, in the building the
     * player starts in) produced NPCs with no AI and no controller at all. Those are inert:
     * no sensors, so speech and being attacked both land on nothing.
     */
    private void giveBrain(EntityActor npc, String role) {
        if (LlmRuntime.isEnabled()){
            npc.set_ai(new LLMAgentAI());
            com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug.log(
                    "spawned LLM %s %s (%s) at %d,%d",
                    role, npc.get_uid(), npc.getName(), npc.origin.getX(), npc.origin.getY());
        }else{
            npc.set_ai(new PedestrianAI());
        }
        npc.set_controller(new RLController());
        npc.set_combat(new RLCombat());
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

    /**
     * Build one building on a lot: organic footprint -> outer walls from the
     * mask -> constrained-BSP rooms per footprint part -> connectivity doors ->
     * street-facing windows/entrance -> yard decoration.
     */
    private void generateBuilding(Building building, Lot lot, boolean isSafehouse) {
        Footprint fp = FootprintGenerator.generate(lot, chunk_random);
        building.footprint = fp.mask;
        building.parts = fp.parts;

        //outer shell traced from the (possibly non-rectangular) mask
        traceMask(fp.mask);

        //Stage 4: interior rooms. APARTMENTs use open-plan constrained BSP (rooms
        //stay untyped, furnished by the residential logic); commercial/civic
        //buildings use the template-driven corridor/BSP layout with typed rooms.
        List<Room> rooms = new ArrayList<Room>();
        boolean commercial = !building.isResidential();

        if (commercial){
            BuildingTemplate template = BuildingTemplates.forType(building.type);
            rooms = BuildingLayout.layout(template, fp.parts, chunk_random);
        } else {
            List<Block> bsp = new ArrayList<Block>();
            for (Block part : fp.parts){
                RoomSplitter.split(part, chunk_random, bsp);
            }
            for (Block b : bsp){
                rooms.add(new Room(b));
            }
        }

        //draw each room's interior walls
        for (Room room : rooms){
            traceBlock(room);
            room.clearNeighbours(); //rebuild the door graph from scratch
        }

        //Stage 5.5: connectivity — commercial rooms door onto the corridor first,
        //then a spanning-tree pass guarantees every room is reachable.
        connect(rooms, commercial);

        //street-facing windows + a guaranteed exterior entrance
        placeExteriorFeatures(building, lot, rooms);

        //yard: grass and the odd crate against exterior walls (mock look)
        decorateYard(building, lot);

        building.rooms = new ArrayList<Block>(rooms);
        building.roomList = rooms;

        //remember the police station so patrolmen spawn at its lobby
        if (building.type == BuildingType.POLICE_STATION){
            policeStation = building;
        }
    }

    /** Trace the building outline: place a wall on every footprint-mask edge cell. */
    private void traceMask(GridMask m) {
        for (int i = 0; i < m.w; i++){
            for (int j = 0; j < m.h; j++){
                if (m.isEdge(i, j)){
                    placeWall(m.ox + i, m.oy + j);
                } else if (m.isInterior(i, j)){
                    //flag the floor so the renderer gives it an interior material
                    RLTile tile = (RLTile)(getLayer().get_tile(m.ox + i, m.oy + j));
                    if (tile != null){
                        tile.setIndoor(true);
                    }
                }
            }
        }
    }

    /**
     * Punch doors so every room is reachable (TOWN_GENERATION_DESIGN.md 5.5).
     * When {@code preferCorridor} is set, each non-corridor room first gets a
     * direct door onto a corridor (the mock's "one door per room" look); then a
     * BFS spanning tree plus a repair pass guarantee full connectivity. A shared
     * {@code punched} set keeps any wall from receiving two stacked door entities.
     */
    private void connect(List<Room> rooms, boolean preferCorridor) {
        if (rooms.size() < 2){
            return;
        }

        Set<Long> punched = new HashSet<Long>();

        if (preferCorridor){
            for (Room r : rooms){
                if (r.isCorridor()){
                    continue;
                }
                for (Room c : rooms){
                    if (c.isCorridor() && punchBetween(r, c, punched)){
                        break;
                    }
                }
            }
        }

        List<Room> visited = new ArrayList<Room>();
        int head = 0;
        visited.add(rooms.get(0));

        while (head < visited.size()){
            Room cur = visited.get(head++);
            for (Room other : rooms){
                if (visited.contains(other)){
                    continue;
                }
                if (cur.hasNeighbour(other) || punchBetween(cur, other, punched)){
                    visited.add(other);
                }
            }
        }

        //repair: force-connect anything the BFS could not reach
        for (Room room : rooms){
            if (visited.contains(room)){
                continue;
            }
            for (Room other : visited){
                if (room.hasNeighbour(other) || punchBetween(room, other, punched)){
                    visited.add(room);
                    break;
                }
            }
        }
    }

    /**
     * If {@code a} and {@code b} share a wall, ensure a door exists on it (once)
     * and record the adjacency. Vault/cell rooms get a reinforced locked door.
     * Returns true when the two rooms are (now) door-connected.
     */
    private boolean punchBetween(Room a, Room b, Set<Long> punched) {
        List<Point> doors = RoomSplitter.sharedWallDoors(a, b);
        if (doors.isEmpty()){
            return false;
        }
        Point d = doors.get(doors.size() / 2);
        long key = ((long) d.getX() << 32) ^ (d.getY() & 0xffffffffL);
        if (!punched.contains(key)){
            boolean secure = isSecure(a) || isSecure(b);
            punchDoor(d.getX(), d.getY(), secure, secure ? 200 : 5);
            punched.add(key);
        }
        a.addNeighbour(b);
        b.addNeighbour(a);
        return true;
    }

    private boolean isSecure(Room r) {
        return r.type != null && r.type.isSecure();
    }

    /**
     * Windows on exterior (footprint-edge) walls, spaced by WINDOW_SPACING, plus
     * a guaranteed exterior entrance preferring a street-facing wall. Exterior
     * wall tiles are found per room-side using the footprint mask, so only real
     * outside-facing walls (not walls between stacked rooms) qualify.
     */
    /** One exterior wall tile plus the room it belongs to and whether it faces a street. */
    private static class ExtTile {
        final Point p;
        final Room room;
        final boolean street;
        ExtTile(Point p, Room room, boolean street){ this.p = p; this.room = room; this.street = street; }
    }

    private void placeExteriorFeatures(Building building, Lot lot, List<Room> rooms) {
        GridMask m = building.footprint;

        List<ExtTile> exterior = new ArrayList<ExtTile>();
        for (Room room : rooms){
            collectSide(room, m, lot, Lot.N, exterior);
            collectSide(room, m, lot, Lot.S, exterior);
            collectSide(room, m, lot, Lot.W, exterior);
            collectSide(room, m, lot, Lot.E, exterior);
        }

        //entrance: prefer a street-facing wall of an entrance room (lobby/shop/
        //living room), then any street wall, then any exterior wall.
        ExtTile entrance = pickEntrance(exterior);
        if (entrance != null){
            boolean locked = entranceLocked(building.type);
            punchDoor(entrance.p.getX(), entrance.p.getY(), locked);
            building.entrance = entrance.p;
            if (!chunk.hasMilestone(entrance.p)){
                chunk.addMilestone(entrance.p);   //let NPCs path to the door
            }
        }

        //windows on the rest, regularly spaced; never on windowless room types
        int counter = 0;
        for (ExtTile e : exterior){
            if (entrance != null && e.p.getX() == entrance.p.getX() && e.p.getY() == entrance.p.getY()){
                continue;
            }
            if (!roomAllowsWindow(e.room)){
                continue;
            }
            if (counter % TownGenConfig.WINDOW_SPACING == 0){
                placeWindow(e.p.getX(), e.p.getY());
            }
            counter++;
        }
    }

    /** Best available entrance wall, in descending order of desirability. */
    private ExtTile pickEntrance(List<ExtTile> exterior) {
        List<ExtTile> streetEntrance = new ArrayList<ExtTile>();
        List<ExtTile> street = new ArrayList<ExtTile>();
        List<ExtTile> entranceRoom = new ArrayList<ExtTile>();

        for (ExtTile e : exterior){
            boolean isEntranceRoom = roomIsEntrance(e.room);
            if (e.street && isEntranceRoom){ streetEntrance.add(e); }
            else if (e.street){ street.add(e); }
            else if (isEntranceRoom){ entranceRoom.add(e); }
        }

        List<ExtTile> pool = !streetEntrance.isEmpty() ? streetEntrance
                : !street.isEmpty() ? street
                : !entranceRoom.isEmpty() ? entranceRoom
                : exterior;
        if (pool.isEmpty()){
            return null;
        }
        return pool.get(chunk_random.nextInt(pool.size()));
    }

    /** Banks and offices lock their street door; everything else stays open. */
    private boolean entranceLocked(BuildingType type) {
        return type == BuildingType.BANK || type == BuildingType.OFFICE;
    }

    /** Untyped (apartment) rooms allow both windows and entrances. */
    private boolean roomAllowsWindow(Room r) {
        return r.type == null || r.type.allowsWindow();
    }

    private boolean roomIsEntrance(Room r) {
        return r.type == null || r.type.isEntranceRoom();
    }

    /**
     * Append the exterior wall tiles of one room side (excluding corners). A tile
     * qualifies when it is on the footprint boundary and the cell immediately
     * outside (in this side's direction) is not part of the building — i.e. it
     * genuinely faces {@code side}.
     */
    private void collectSide(Room room, GridMask m, Lot lot, int side, List<ExtTile> out) {
        int x0 = room.getX(), y0 = room.getY();
        int x1 = x0 + room.getW(), y1 = y0 + room.getH();
        boolean streetFacing = lot.street[side];

        if (side == Lot.N || side == Lot.S){
            int wy = (side == Lot.N) ? y0 : y1;
            int outY = (side == Lot.N) ? wy - 1 : wy + 1;
            for (int x = x0 + 1; x < x1; x++){
                if (m.isEdgeWorld(x, wy) && !m.getWorld(x, outY)){
                    out.add(new ExtTile(new Point(x, wy), room, streetFacing));
                }
            }
        } else {
            int wx = (side == Lot.W) ? x0 : x1;
            int outX = (side == Lot.W) ? wx - 1 : wx + 1;
            for (int y = y0 + 1; y < y1; y++){
                if (m.isEdgeWorld(wx, y) && !m.getWorld(outX, y)){
                    out.add(new ExtTile(new Point(wx, y), room, streetFacing));
                }
            }
        }
    }

    /** Scatter grass across the yard (lot cells outside the footprint) and the
     *  occasional crate against an exterior wall, echoing the mock's street look. */
    private void decorateYard(Building building, Lot lot) {
        GridMask m = building.footprint;
        for (int i = 0; i <= lot.getW(); i++){
            for (int j = 0; j <= lot.getH(); j++){
                int wx = lot.getX() + i;
                int wy = lot.getY() + j;
                if (m.getWorld(wx, wy)){
                    continue;   //inside the building
                }
                boolean adjacentToWall = m.getWorld(wx-1, wy) || m.getWorld(wx+1, wy)
                                      || m.getWorld(wx, wy-1) || m.getWorld(wx, wy+1);
                if (adjacentToWall && chunk_random.nextInt(100) < 5){
                    EntityFurniture crate = new EntityFurniture();
                    placeEntity(wx, wy, crate, "crate", "x", new Color(150, 110, 60));
                    crate.get_combat().set_hp(15);
                } else if (chunk_random.nextInt(100) < 8){
                    Entity grass = new Entity();
                    placeEntity(wx, wy, grass, "grass", "\"");
                    grass.set_blocking(false);
                }
            }
        }
    }

    private void punchDoor(int x, int y, boolean locked) {
        punchDoor(x, y, locked, 5);
    }

    private void punchDoor(int x, int y, boolean locked, int hp) {
        clearWall(x, y);
        EntityDoor door = new EntityDoor();
        placeEntity(x, y, door, "door", "+", locked ? Color.red : Color.green);
        door.get_combat().set_hp(hp);
        if (locked){
            door.lock();
        } else {
            door.unlock();
        }
    }

    private void placeWindow(int x, int y) {
        EntityFurniture window = new EntityFurniture();
        placeEntity(x, y, window, "window", "=", Color.green);
        window.get_combat().set_hp(1);
        window.setBlockSight(false);
        clearWall(x, y);
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

    /**
     * Knock a hole in a wall for a door or a window. The tile stops being a wall
     * for gameplay, but stays flagged as a gap so the renderer keeps the wall run
     * visually continuous through the frame.
     */
    private void clearWall(int i, int j) {
        RLTile tile = (RLTile)(getLayer().get_tile(i,j));
        tile.setWall(false);
        tile.setWallGap(true);
    }


    private WorldTile addTile(int i, int j, Random chunk_random) {
        WorldTile tile = new RLTile();
        Point origin = new Point(i,j);
        tile.origin = origin;

        getLayer().set_tile(origin, tile);

        return tile;
    }
}