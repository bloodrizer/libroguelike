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
import com.nuclearunicorn.serialkiller.game.ai.PedestrianAI;
import com.nuclearunicorn.serialkiller.game.ai.PoliceAI;
import com.nuclearunicorn.serialkiller.game.ai.ProstituteAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmRuntime;
import com.nuclearunicorn.serialkiller.game.character.CharacterPreset;
import com.nuclearunicorn.serialkiller.game.character.CharacterSetup;
import com.nuclearunicorn.serialkiller.game.character.SpawnPlace;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.modes.in_game.InGameMode;
import com.nuclearunicorn.serialkiller.game.sound.SoundConfig;
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
import com.nuclearunicorn.libroguelike.utils.Rng;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 */
public class TownChunkGenerator extends ChunkGenerator {

    private static final int NPC_PER_ROAD_RATE = 35;    //50% is a hell lot of npc , 35 is sorta ok
    private static final int MAX_POLICEMAN_COUNT = 4;
    //one worker per private room - the brothel template builds 3 to 6 of them, so a big
    //house keeps more girls than a small one and nobody is doubled up in a bedroom
    private static final int MAX_PROSTITUTE_COUNT = 6;
    private static final int MAX_HOUSEHOLD = 4;         //people per home, however big the house

    long seed;
    Random chunk_random;

    List<Block> districts = null;
    List<Block> roads = new ArrayList<Block>();
    private static final int ROAD_SIZE = 3;

    //per-chunk building-type picker, the police station and the player's own home
    private TypeSelector typeSelector;
    private Building policeStation;
    private Building brothel;
    private Building playerHome;

    //everything that has to wait until the whole street is built - see generate()
    private final List<Point> lampposts = new ArrayList<Point>();
    private final List<Building> yards = new ArrayList<Building>();
    private final List<Block> parks = new ArrayList<Block>();
    private int loiterers;

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

    /* Every building that went up, safehouse included - the town map reads this one */
    public List<Building> getBuildings(){
        return ((RLWorldModel)environment.getWorld()).getBuildings();
    }

    public void generate(WorldChunk chunk){

        if (chunk instanceof RLWorldChunk){
            this.chunk = (RLWorldChunk)chunk;
        }else{
            throw new RuntimeException("trying to generate non-RLWorldChunk element");
        }

        //per chunk so regenerating one reproduces it, and per session seed so the seed means
        //something - it used to be x*10000+y alone, making every seed the same town
        seed = Rng.chunkSeed(Rng.WORLDGEN, chunk.origin.getX(), chunk.origin.getY());
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
        brothel = null;
        playerHome = null;
        lampposts.clear();
        yards.clear();
        parks.clear();
        loiterers = 0;

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
        //the player has to start somewhere, so keep trying lots until one takes a building
        while (!lots.isEmpty()){
            Lot safehouseLot = lots.remove(chunk_random.nextInt(lots.size()));
            Building safehouse = new Building(safehouseLot);
            safehouse.type = BuildingType.APARTMENT;        //family/bed logic needs this
            if (!generateSafehouse(safehouse, safehouseLot)){
                continue;   //too small to build on; that lot is now yard
            }
            typeSelector.forceRecord(BuildingType.APARTMENT);
            //deliberately NOT in getApartments(): that list is "homes going spare", and it is
            //read twice below - once to house pedestrians, once to furnish. The safehouse has
            //already housed and furnished itself for the player's family, so being on it a
            //second time billeted strangers in the player's flat and laid a second set of beds
            //on top of the family's, which is the room full of beds you start the game in.
            playerHome = safehouse;
            safehouse.isPlayerHome = true;
            break;
        }

        //-----------------------------------------------------------
        //		create other buildings (data-driven type per lot)
        //-----------------------------------------------------------
        for (Lot lot : lots){
            Building building = new Building(lot);
            building.type = typeSelector.pick(lot.getW(), lot.getH(), chunk_random);
            if (generateBuilding(building, lot, false)){
                getApartments().add(building);
            }
        }

        //-----------------------------------------------------------
        //		outdoor props, now that every door and window is cut
        //-----------------------------------------------------------
        for (Building building : yards){
            decorateYard(building);
        }
        for (Point post : lampposts){
            placeLamppost(post.getX(), post.getY());
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

        //last, before the nav graph is built off it: make sure the place hangs together
        repairConnectivity(x, y, size);

        //the town is final, so the preset's spawn place can now be looked up in it
        resolvePlayerSpawn();

        NLTimer graphTimer = new NLTimer();
        graphTimer.push();
        for (Point milestone: this.chunk.getMilestones()){
            AdaptivePathfinder.addPoint(this.chunk, milestone); //sub-optimal
        }
        //AdaptivePathfinder.buildGraph(this.chunk);  //finally, build graph
        graphTimer.pop("Adaptive graph generation");
    }

    /**
     * Open up anywhere the generator has walled off by accident, so the chunk is one place
     * you can walk around.
     *
     * <p>Placement rules alone cannot promise this. Keeping props out of doorways and off
     * the room's waist stops the obvious cases, but the ways to seal a pocket are endless —
     * a crate in a yard's only gap, two shelves meeting across a corridor, a room the layout
     * gave no door at all — and each new rule only narrows the odds. So the guarantee is made
     * where it can actually be checked: flood the finished chunk, and for every pocket that
     * came out separate, take out the one thing holding it shut.
     *
     * <p>This matters more than tidiness. An NPC whose bed is behind a sealed door has no
     * route home, so they stand wherever they were when they gave up — and in a hallway that
     * plugs it for everyone walking home behind them.
     */
    private void repairConnectivity(int x0, int y0, int size) {
        int repaired = 0;
        for (int pass = 0; pass < 12; pass++){
            int[][] component = new int[size][size];
            int biggest = 0, biggestId = 0, count = 0;
            for (int i = 0; i < size; i++){
                for (int j = 0; j < size; j++){
                    if (component[i][j] != 0 || !walkable(x0 + i, y0 + j)){
                        continue;
                    }
                    int filled = floodComponent(component, i, j, ++count, x0, y0, size);
                    if (filled > biggest){
                        biggest = filled;
                        biggestId = count;
                    }
                }
            }
            if (count <= 1){
                //this is a net, not a design: every tile it opens is a room some placement
                //rule sealed, and the count is the only sign of it before a player walks in
                if (repaired > 0){
                    System.err.println("town: repaired " + repaired + " sealed tile(s)");
                }
                return;   //one place, nothing to do
            }
            int opened = openPockets(component, biggestId, x0, y0, size);
            repaired += opened;
            if (opened == 0){
                System.err.println("town: " + (count - 1) + " sealed pocket(s) left after repair"
                        + " (" + repaired + " tile(s) opened)");
                return;
            }
        }
    }

    /** Fill one component, returning its size. Coordinates are chunk-local. */
    private int floodComponent(int[][] component, int si, int sj, int id, int x0, int y0, int size) {
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<int[]>();
        queue.add(new int[]{si, sj});
        component[si][sj] = id;
        int filled = 0;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!queue.isEmpty()){
            int[] at = queue.poll();
            filled++;
            for (int[] d : dirs){
                int ni = at[0] + d[0], nj = at[1] + d[1];
                if (ni < 0 || nj < 0 || ni >= size || nj >= size){ continue; }
                if (component[ni][nj] != 0 || !walkable(x0 + ni, y0 + nj)){ continue; }
                component[ni][nj] = id;
                queue.add(new int[]{ni, nj});
            }
        }
        return filled;
    }

    /**
     * Join every cut-off pocket back onto the main area, opening <b>one</b> tile per pocket.
     *
     * <p>The one-per-pocket part is the whole point. The component map is a snapshot taken
     * before any of this ran, so every tile around a sealed room still reads as "pocket on one
     * side, town on the other" long after the first one was opened — carry on down the list
     * and the room comes out ringed with doors, its windows torn out, which is exactly what a
     * player sees and calls nonsense. Once a pocket is joined it is struck off and the rest of
     * its perimeter is left alone.
     *
     * <p>Props go first and masonry second, for the same reason: what usually seals a room is
     * a crate set down in the doorway, and lifting the crate is the repair. Knocking a fresh
     * hole in a wall is the fallback for a room the layout genuinely never doored.
     *
     * @return how many tiles were opened — zero means the remaining pockets are beyond helping
     */
    private int openPockets(int[][] component, int mainId, int x0, int y0, int size) {
        Set<Integer> joined = new HashSet<Integer>();
        int opened = sweepPockets(component, mainId, x0, y0, size, joined, LIFT_PROP);
        opened += sweepPockets(component, mainId, x0, y0, size, joined, DOOR_CLEAR);
        opened += sweepPockets(component, mainId, x0, y0, size, joined, DOOR_ANYWHERE);
        return opened;
    }

    private static final int LIFT_PROP     = 0;   //take the crate out of the doorway
    private static final int DOOR_CLEAR    = 1;   //punch a door where no gap adjoins (C2)
    private static final int DOOR_ANYWHERE = 2;   //last resort: reachable beats tidy

    /** One sweep of {@link #openPockets} in the given repair mode. */
    private int sweepPockets(int[][] component, int mainId, int x0, int y0, int size,
                             Set<Integer> joined, int mode) {
        int opened = 0;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int i = 1; i < size - 1; i++){
            for (int j = 1; j < size - 1; j++){
                int wx = x0 + i, wy = y0 + j;
                if (walkable(wx, wy)){
                    continue;
                }
                //does opening this tile join the main area to a pocket still cut off?
                boolean touchesMain = false;
                Set<Integer> pockets = new HashSet<Integer>();
                for (int[] d : dirs){
                    int ni = i + d[0], nj = j + d[1];
                    if (ni < 0 || nj < 0 || ni >= size || nj >= size){ continue; }
                    int id = component[ni][nj];
                    if (id == 0 || joined.contains(id)){ continue; }
                    if (id == mainId){ touchesMain = true; } else { pockets.add(id); }
                }
                if (!touchesMain || pockets.isEmpty()){
                    continue;
                }
                RLTile tile = (RLTile)(getLayer().get_tile(wx, wy));
                if (tile == null){
                    continue;
                }
                Entity obstacle = tile.get_obstacle();
                if (mode == LIFT_PROP){
                    if (obstacle == null || obstacle.controller != null){
                        continue;   //masonry: leave it for the later sweeps
                    }
                    if (tile.isWallGap()){
                        //a window or a shut door is the building, not the thing plugging it.
                        //Deleting one leaves the hole in the wall the repair was meant to
                        //avoid, and the real culprit - a crate in the alley outside - is
                        //still standing two tiles away, waiting to be lifted instead.
                        continue;
                    }
                    tile.remove_entity(obstacle);
                    environment.getEntityManager().remove_entity(obstacle);
                } else if (obstacle == null && tile.isWall()){
                    if (mode == DOOR_CLEAR && nextToWallGap(wx, wy)){
                        continue;   //would sit against another gap; try elsewhere first (C2)
                    }
                    punchDoor(wx, wy, false);   //a room the layout forgot to give a door
                } else {
                    continue;
                }
                joined.addAll(pockets);
                opened++;
            }
        }
        return opened;
    }

    /**
     * Walkable as far as the repair pass is concerned. A door counts even when it is shut and
     * locked: a bank is <i>locked</i>, not walled off, and calling it a wall sent the repair
     * hunting for another way in — which it found, by deleting the bank's front door.
     */
    private boolean walkable(int x, int y) {
        RLTile tile = (RLTile)(getLayer().get_tile(x, y));
        if (tile == null){
            return false;
        }
        return !tile.isPathBlocked() || tile.has_ent(EntityDoor.class);
    }

    /*
        Safehouse it the apartment block owned by a player.
     */
    private boolean generateSafehouse(Building safehouseBlock, Lot lot) {
        if (!generateBuilding(safehouseBlock, lot, true)){
            return false;   //caller tries another lot: the player has to start somewhere
        }

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

        Point mateOrigin = homeTile(safehouseBlock);
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

        Point child1Origin = homeTile(safehouseBlock);
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

        Point child2Origin = homeTile(safehouseBlock);
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
        Point playerPosition = homeTile(safehouseBlock);
        if (playerPosition == null){
            playerPosition = new Point(safehouseBlock.getX()+1, safehouseBlock.getY()+1);
        }
        RLWorldModel.playerSafeHouseLocation = playerPosition;

        /*Point playerPosition = this.blockGetFreeTile(safehouseBlock);
        Player.get_ent().move_to(playerPosition);*/




        return true;   //the caller keeps it out of getApartments(): no one lives in my house!
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

    /** Furnish a non-residential building room-by-type. Its staff live elsewhere in town. */
    private void furnishCommercial(Building building) {
        for (Room room : building.roomList){
            furnishRoom(building, room);
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
        //a ladder does not need a wall behind it, but it is still solid, and a solid thing
        //on the threshold is the barricade C1 forbids - this was the commonest one left
        Point coord = clearFreeTile(room);
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

    /**
     * A free interior tile next to a wall (furniture-against-walls look,
     * TOWN_GENERATION_DESIGN.md 5.6), never one a door or window opens onto.
     *
     * <p>There used to be a fallback here for the room with no clean spot against a wall —
     * first the doorway's own mouth, then any free tile at all. Both put a fridge on the
     * threshold, which is the barricade INVARIANTS C1 exists to forbid, and it is the one
     * placement a player reads as broken from across the street. A room that cannot hold the
     * thing tidily does without it: {@link #clearFreeTile} tries the middle of the floor, and
     * every caller already handles a null.
     */
    private Point wallAdjacentFreeTile(Block room) {
        List<Point> clear = new ArrayList<Point>();
        for (int i = 1; i < room.getW(); i++){
            for (int j = 1; j < room.getH(); j++){
                int wx = room.getX() + i, wy = room.getY() + j;
                if (!isFreeFloor(wx, wy) || !nextToWall(wx, wy) || nextToWallGap(wx, wy)){
                    continue;
                }
                clear.add(new Point(wx, wy));
            }
        }
        List<Point> candidates = notSealing(clear, room);
        if (candidates.isEmpty()){
            return clearFreeTile(room);
        }
        return candidates.get(chunk_random.nextInt(candidates.size()));
    }

    /** Any free tile of the room clear of doorways and window frames, or null (C1). */
    private Point clearFreeTile(Block room) {
        List<Point> free = new ArrayList<Point>();
        for (int i = 1; i < room.getW(); i++){
            for (int j = 1; j < room.getH(); j++){
                int wx = room.getX() + i, wy = room.getY() + j;
                if (isFreeFloor(wx, wy) && !nextToWallGap(wx, wy)){
                    free.add(new Point(wx, wy));
                }
            }
        }
        List<Point> candidates = notSealing(free, room);
        if (candidates.isEmpty()){
            return null;
        }
        return candidates.get(chunk_random.nextInt(candidates.size()));
    }

    /**
     * Drop the tiles where a solid prop would cut the room in two.
     *
     * <p>Keeping props out of doorways is not enough on its own: a crate across the waist of
     * an L-shaped room, or the second of two set side by side, seals just as thoroughly and
     * no local rule sees it coming. The consequence is invisible for a long time — the room
     * still looks right, and only some NPC who lives behind it ever finds out, hours later
     * and three subsystems away. So ask the question directly, on a room-sized flood fill
     * that costs nothing at generation time.
     */
    private List<Point> notSealing(List<Point> candidates, Block room) {
        List<Point> safe = new ArrayList<Point>();
        for (Point p : candidates){
            if (!sealsRoom(p, room)){
                safe.add(p);
            }
        }
        return safe;
    }

    /**
     * True if blocking this tile would leave part of the room unreachable from the rest.
     *
     * <p>The region reasoned over is the room <i>and its own walls</i> — {@code Block} is
     * inclusive of its perimeter, so the doorways are already on that boundary. Reaching a
     * tile further out was the earlier attempt, and it answered the wrong question: the extra
     * ring is street and neighbouring rooms, which are of course not reachable from in here,
     * so every candidate came back "seals" and every prop in town fell through to the
     * unguarded fallback — the opposite of what the check was added for.
     */
    private boolean sealsRoom(Point blocked, Block room) {
        int x0 = room.getX(), y0 = room.getY();
        int w = room.getW() + 1, h = room.getH() + 1;

        boolean[][] open = new boolean[w][h];
        int total = 0, si = 0, sj = 0;
        for (int i = 0; i < w; i++){
            for (int j = 0; j < h; j++){
                int x = x0 + i, y = y0 + j;
                if (x == blocked.getX() && y == blocked.getY()){
                    continue;
                }
                RLTile tile = (RLTile)(getLayer().get_tile(x, y));
                if (tile == null || tile.isPathBlocked()){
                    continue;
                }
                open[i][j] = true;
                total++;
                si = i;
                sj = j;
            }
        }
        if (total < 2){
            return false;   //nothing left to disconnect
        }

        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<int[]>();
        boolean[][] seen = new boolean[w][h];
        queue.add(new int[]{si, sj});
        seen[si][sj] = true;
        int reached = 0;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!queue.isEmpty()){
            int[] at = queue.poll();
            reached++;
            for (int[] d : dirs){
                int ni = at[0] + d[0], nj = at[1] + d[1];
                if (ni < 0 || nj < 0 || ni >= w || nj >= h){ continue; }
                if (!open[ni][nj] || seen[ni][nj]){ continue; }
                seen[ni][nj] = true;
                queue.add(new int[]{ni, nj});
            }
        }
        return reached != total;
    }

    /**
     * Somewhere a prop may stand. Doorways and window frames are excluded: furnishing runs
     * after the doors are punched, so a gap in a wall is not a wall and is next to one —
     * exactly what {@link #wallAdjacentFreeTile} looks for. A crate in the only doorway
     * seals the room behind it, and the pathfinder can do nothing but declare it unreachable.
     */
    private boolean isFreeFloor(int x, int y) {
        RLTile tile = (RLTile)(getLayer().get_tile(x, y));
        if (tile == null || tile.isWall() || tile.isWallGap() || tile.isBlocked()){
            return false;
        }
        //nor on top of a prop that does not block: a bed is walkable, so nothing stopped a
        //crate being set down on one, and the sleeper then had nowhere to sleep
        return !tile.has_ent(EntityFurniture.class);
    }

    private boolean nextToWall(int x, int y) {
        return isWallTile(x - 1, y) || isWallTile(x + 1, y)
            || isWallTile(x, y - 1) || isWallTile(x, y + 1);
    }

    private boolean isWallTile(int x, int y) {
        RLTile tile = (RLTile)(getLayer().get_tile(x, y));
        return tile != null && tile.isWall();
    }

    /** True if a door or window opens onto this tile — the one square that must stay clear. */
    private boolean nextToWallGap(int x, int y) {
        return isWallGapTile(x - 1, y) || isWallGapTile(x + 1, y)
            || isWallGapTile(x, y - 1) || isWallGapTile(x, y + 1);
    }

    private boolean isWallGapTile(int x, int y) {
        RLTile tile = (RLTile)(getLayer().get_tile(x, y));
        return tile != null && tile.isWallGap();
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

        //where the lampposts go, not the lampposts themselves: the buildings they stand
        //outside do not exist yet, so at this point there is no telling which of these spots
        //will turn out to be somebody's doorstep. See the deferred pass in generate().
        int step = TownGenConfig.LAMPPOST_SPACING;
        for (int i = 0; i <= w; i += step){
            lampposts.add(new Point(x + i, y));
            lampposts.add(new Point(x + i, y + h));
        }
        for (int j = step; j < h; j += step){
            lampposts.add(new Point(x, y + j));
            lampposts.add(new Point(x + w, y + j));
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
        if (tile == null || tile.isWall() || tile.isBlocked() || nextToWallGap(i, j)){
            return;   //C1: not on somebody's doorstep, and not stacked on another prop
        }
        EntityFurniture lamp = new EntityFurniture();
        placeEntity(i, j, lamp, "lamppost", "i", new Color(210, 210, 140));
        lamp.get_combat().set_hp(30);
        lamp.setBlockSight(false);
    }

    /**
     * Everyone who lives here, put where they live.
     *
     * <p>The town used to be populated by scattering people along the roads and posting each
     * one a house afterwards. That reads wrong the moment you look at the clock — the game
     * opens at 21:00, so the streets came up full of citizens standing about in the dark
     * outside their own front doors — and it hid two real faults. Anyone the layout had
     * walled off from their home simply never arrived, which looks identical to an NPC that
     * is merely slow; and the shop staff spawned indoors could be sealed in by their
     * building's own locked entrance, since A* will not route through a locked door.
     * Spawning people at home instead (INVARIANTS C3) makes both loud: an NPC in the wrong
     * place at turn zero is a generator bug, not a pathfinding one.
     *
     * <p>The road, park and shop rolls are kept exactly as they were, so the town holds the
     * same number of people as before — they just start indoors.
     */
    private void populateMap() {
        //residents live only in APARTMENT buildings (5.7); if a chunk somehow has
        //no residential building, fall back to any apartment so we never nextInt(0).
        List<Apartment> residential = getResidentialApartments();
        Collections.shuffle(residential, chunk_random);   //whose house takes the odd lodger
        int[] taken = new int[residential.size()];

        int population = loiterers;     //the park's idlers: rolled before any house existed
        for (Block road: roads){
            if (chunk_random.nextInt(100) < NPC_PER_ROAD_RATE){
                population++;
            }
        }
        //shop and office staff. The roll stays where it always was in the sequence, but a
        //shop at 21:00 is shut, so its staff are at home with everybody else.
        for (Apartment apt : getApartments()){
            if (apt instanceof Building && !((Building)apt).isResidential()){
                population += chunk_random.nextInt(3);
            }
        }

        //who ended up under which roof, so the household can be made into a family below
        Map<Apartment, List<EntityRLHuman>> households =
                new LinkedHashMap<Apartment, List<EntityRLHuman>>();

        for (int i = 0; i < population; i++){
            if (residential.isEmpty()){ break; }   //nowhere to live (all parks)
            Apartment apt = claimHome(residential, taken);
            Point coord = homeTile(apt);
            if (coord == null){ continue; }        //a home with no room to stand in

            EntityRLHuman npc = (EntityRLHuman)placeNPC(coord.getX(), coord.getY());
            //Every pedestrian is an LLM agent when inference is enabled; the reactor
            //self-throttles to the near bucket so far NPCs cost nothing (§11).
            giveBrain(npc, "agent");

            npc.setApartment(apt);
            stampOwnership(apt, npc);

            List<EntityRLHuman> household = households.get(apt);
            if (household == null){
                household = new ArrayList<EntityRLHuman>();
                households.put(apt, household);
            }
            household.add(npc);
        }

        marryHouseholds(households);

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

            // One brain per kind of person, inference or not. Swapping the brain when the
            // LLM came on used to quietly disband the police force: same uniform, same
            // stunstick, civilian reflexes underneath.
            police.set_ai(new PoliceAI());
            com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug.log(
                    "spawned police %s at %d,%d", police.get_uid(), coord.getX(), coord.getY());
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

        //prostitutes — live and work at the brothel, so libido has a lawful outlet (see SexAction)
        spawnProstitutes();
    }

    /**
     * The brothel's staff. Unlike the pedestrians above they are not housed in a random flat:
     * the brothel is their home, so at night "go home" puts them in a private-room bed and by
     * day the workplace impulse keeps them where a customer can find them.
     */
    private void spawnProstitutes() {
        if (brothel == null) {
            return;
        }
        List<Point> posts = brothelSpawnPoints();
        int staff = Math.min(MAX_PROSTITUTE_COUNT, Math.max(1, privateRooms()));
        for (int i = 0; i < staff && !posts.isEmpty(); i++) {
            Point coord = posts.remove(chunk_random.nextInt(posts.size()));

            EntityRLHuman prostitute = NPCGenerator.generateNPC(chunk_random, this,
                    coord.getX(), coord.getY());
            prostitute.age = NPCGenerator.generateAge(chunk_random, true);
            prostitute.setSex(EntityRLHuman.Sex.FEMALE);
            prostitute.setName(new NameGenerator().generate(false));

            prostitute.set_ai(new ProstituteAI());
            prostitute.set_controller(new RLController());
            prostitute.set_combat(new RLCombat());
            prostitute.setApartment(brothel);   //home is the brothel, not a flat of her own

            com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug.log(
                    "spawned prostitute %s at %d,%d", prostitute.get_uid(), coord.getX(), coord.getY());
        }
    }

    /** How many rooms the brothel has to take a client into, and so how many girls it keeps. */
    private int privateRooms() {
        int rooms = 0;
        if (brothel != null && brothel.roomList != null) {
            for (Room room : brothel.roomList) {
                if (room.type == RoomType.PRIVATE_ROOM) {
                    rooms++;
                }
            }
        }
        return rooms;
    }

    /** Free floor tiles in the brothel's private rooms, or its reception as a fallback. */
    private List<Point> brothelSpawnPoints() {
        List<Point> posts = new ArrayList<Point>();
        if (brothel == null || brothel.roomList == null) {
            return posts;
        }
        for (Room room : brothel.roomList) {
            if (room.type == RoomType.PRIVATE_ROOM) {
                collectFreeFloor(room, posts);
            }
        }
        if (posts.isEmpty()) {
            for (Room room : brothel.roomList) {
                if (room.type == RoomType.RECEPTION) {
                    collectFreeFloor(room, posts);
                }
            }
        }
        return posts;
    }

    /**
     * The emptiest home still under its capacity, or the emptiest one of all when the town has
     * more people than beds.
     *
     * <p>Homes used to be drawn uniformly at random and independently, which is not "everyone
     * lives somewhere" but a balls-into-bins draw: with sixty pedestrians over twenty-odd
     * houses the busiest reliably collects six or seven of them, and since a resident is what
     * {@link #fillApartmentRooms} counts beds from, that house is furnished as a dormitory —
     * seven beds crammed into three rooms, next door to an empty one.
     */
    private Apartment claimHome(List<Apartment> homes, int[] taken) {
        int best = -1;
        for (int i = 0; i < homes.size(); i++){
            if (taken[i] >= sleeps(homes.get(i))){
                continue;
            }
            if (best < 0 || taken[i] < taken[best]){
                best = i;
            }
        }
        if (best < 0){   //every house is full: the emptiest one takes one more anyway
            for (int i = 0; i < homes.size(); i++){
                if (best < 0 || taken[i] < taken[best]){
                    best = i;
                }
            }
        }
        taken[best]++;
        return homes.get(best);
    }

    /**
     * Somewhere inside the home to stand — a tile of one of its <i>rooms</i> (INVARIANTS C3).
     *
     * <p>Not the home's rect: for a {@link Building} that is the lot it was cut from, yard
     * and all, and picking a free tile out of it put the player's own family on the lawn as
     * often as in the house.
     */
    private Point homeTile(Apartment apt) {
        if (apt.rooms == null || apt.rooms.isEmpty()){
            return null;
        }
        List<Point> free = new ArrayList<Point>();
        for (Block room : apt.rooms){
            collectFreeFloor(room, free);
        }
        if (free.isEmpty()){
            return null;
        }
        return free.get(chunk_random.nextInt(free.size()));
    }

    /**
     * Turn each household of strangers into a family: one surname, and the adults paired off.
     *
     * <p>Only the player used to get a mate — {@code setMate} was called exactly once in the
     * whole generator. So {@code Relations} could say "your wife" and never did for anybody
     * but you, and {@link com.nuclearunicorn.serialkiller.game.ai.behavior.SexAction}'s mate
     * branch was dead for the entire town: every adult in it had to walk to the brothel or
     * escalate, and towns without a brothel could only escalate. Measured before this, over
     * 900 turns: 0 couplings and 106 rapes.
     *
     * <p>Pairing is opposite-sex first because that is the household the rest of the family
     * code already models (the player's mate is chosen the same way). Whoever is left over
     * stays single — a town where everybody is married has no reason to have a brothel.
     */
    private void marryHouseholds(Map<Apartment, List<EntityRLHuman>> households) {
        int couples = 0;
        for (List<EntityRLHuman> household : households.values()){
            if (household.size() < 2){
                continue;   //a lodger on their own is not a family
            }
            renameAsFamily(household);

            List<EntityRLHuman> men = new ArrayList<EntityRLHuman>();
            List<EntityRLHuman> women = new ArrayList<EntityRLHuman>();
            for (EntityRLHuman person : household){
                if (!person.isAdult()){
                    continue;   //the children of the house, who are nobody's mate
                }
                (person.getSex() == EntityRLHuman.Sex.MALE ? men : women).add(person);
            }
            for (int i = 0; i < men.size() && i < women.size(); i++){
                men.get(i).setMate(women.get(i));
                couples++;
            }
        }
        com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug.log(
                "married %d couple(s) across %d household(s)", couples, households.size());
    }

    /**
     * One roof, one surname. Their given names are already rolled and are theirs to keep —
     * only the family name is replaced, so the household reads as the Hales rather than as
     * four unrelated people who happen to share a kitchen.
     */
    private void renameAsFamily(List<EntityRLHuman> household) {
        String surname = new NameGenerator().generateSurname();
        for (EntityRLHuman person : household){
            String name = person.getName();
            if (name == null || name.trim().isEmpty()){
                continue;
            }
            int space = name.indexOf(' ');
            person.setName((space < 0 ? name : name.substring(0, space)) + " " + surname);
        }
    }

    /** How many residents a home holds: a room each, less one for the kitchen. */
    private int sleeps(Apartment apt) {
        int rooms = (apt.rooms == null) ? 1 : apt.rooms.size();
        int cap = rooms - 1;
        if (cap < 1){ cap = 1; }
        if (cap > MAX_HOUSEHOLD){ cap = MAX_HOUSEHOLD; }
        return cap;
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
        if (res.isEmpty() && playerHome != null){
            res.add(playerHome);           //a town of nothing but shops: lodgers after all
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
            if (room.type == RoomType.LOBBY){
                collectFreeFloor(room, posts);
            }
        }
        return posts;
    }

    /**
     * Where the chosen preset wakes the player up, looked up in the town that has just been
     * built and left for {@code InGameMode} to move them to.
     *
     * <p>Only the chunk that built the safehouse answers: it is the one the player was
     * spawned into, and a neighbour generated later must not quietly move them out of it.
     *
     * <p>A place this town does not contain - the brothel cap is one per chunk, and a chunk
     * of small lots may hold none - falls back home. Starting in your own bed is a worse
     * start than the one you asked for; starting nowhere is not a start at all.
     */
    private void resolvePlayerSpawn() {
        if (playerHome == null){
            return;
        }

        CharacterPreset preset = CharacterSetup.current();
        SpawnPlace place = preset.getSpawn();
        Point spot = spawnPoint(place);

        if (spot == null && place != SpawnPlace.HOME){
            System.out.println("[preset] no " + place + " in this town - "
                    + preset.getId() + " starts at home instead");
            spot = RLWorldModel.playerSafeHouseLocation;
        }
        RLWorldModel.playerSpawnLocation = spot;
    }

    private Point spawnPoint(SpawnPlace place) {
        switch (place){
            case HOME:   return RLWorldModel.playerSafeHouseLocation;
            case STREET: return outdoorTile(roads);
            case PARK:   return outdoorTile(parks);
            default:     return roomTile(place.building, place.room);
        }
    }

    /** A free tile in a room of that type, in a building of that type, or null. */
    private Point roomTile(BuildingType buildingType, RoomType roomType) {
        List<Point> free = new ArrayList<Point>();
        for (Apartment apt : getApartments()){
            if (!(apt instanceof Building)){
                continue;
            }
            Building building = (Building)apt;
            if (building.type != buildingType || building.roomList == null){
                continue;
            }
            for (Room room : building.roomList){
                if (room.type == roomType){
                    collectFreeFloor(room, free);
                }
            }
        }
        if (free.isEmpty()){
            return null;
        }
        return free.get(chunk_random.nextInt(free.size()));
    }

    /** Somewhere to stand on one of these open-air blocks - a street, a park. */
    private Point outdoorTile(List<Block> blocks) {
        List<Block> shuffled = new ArrayList<Block>(blocks);
        Collections.shuffle(shuffled, chunk_random);
        for (Block block : shuffled){
            Point spot = block.getFreeTileSafe(chunk_random, getLayer());
            if (spot != null){
                return spot;
            }
        }
        return null;
    }

    /** Every free interior tile of the room, appended to {@code out}. */
    private void collectFreeFloor(Block room, List<Point> out) {
        for (int i = 1; i < room.getW(); i++){
            for (int j = 1; j < room.getH(); j++){
                int wx = room.getX() + i, wy = room.getY() + j;
                if (isFreeFloor(wx, wy)){
                    out.add(new Point(wx, wy));
                }
            }
        }
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

        parks.add(block);   //nothing on the finished map says "park", so remember them

        for(int i = 0; i<=block.getW(); i++ )
            for(int j = 0; j<=block.getH(); j++ ){
                //a person, not a place: parks are laid out before any house exists, so there
                //is nowhere to put him yet. populateMap houses him with everyone else (C3)
                if (chunk_random.nextInt(200) < 1){
                    loiterers++;
                }

                //not on an occupied tile: the loiterer rolled just above shares this square,
                //and a tree is solid, so planting one on him walls him inside it for good -
                //he shows up in the world probe as a person outside the town's one component
                if (chunk_random.nextInt(100) < 2
                        && isFreeFloor(block.getX() + i, block.getY() + j)){
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
        npc.set_ai(new PedestrianAI());
        com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug.log(
                "spawned %s %s (%s) at %d,%d",
                role, npc.get_uid(), npc.getName(), npc.origin.getX(), npc.origin.getY());
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

        bakeSoundLoss(x, y, ent);
    }

    /**
     * Fold a placed prop's muffling into its tile, so {@link SoundConfig} values reach the
     * acoustic flood without it ever touching an entity list (SOUND_DESIGN.md 9.2).
     *
     * <p>People are skipped deliberately, exactly as they are in {@code isPathBlocked}: a
     * neighbour standing in the doorway does not soundproof the house. Doors and windows
     * are skipped too — they overwrite the tile with their own, larger value immediately
     * after this returns, and a door's value has to track its lock state anyway.
     */
    private void bakeSoundLoss(int x, int y, Entity ent){
        if (ent instanceof EntityRLHuman || ent instanceof EntityDoor){
            return;
        }
        RLTile tile = (RLTile)(getLayer().get_tile(x, y));
        if (tile == null){
            return;
        }
        if (ent instanceof EntityTree){
            tile.raiseSoundLoss(SoundConfig.TL_TREE);
        } else if (ent instanceof EntityFurniture){
            tile.raiseSoundLoss(SoundConfig.TL_PROP);
        }
    }

    /**
     * Build one building on a lot: organic footprint -> outer walls from the
     * mask -> constrained-BSP rooms per footprint part -> connectivity doors ->
     * street-facing windows/entrance -> yard decoration.
     */
    private boolean generateBuilding(Building building, Lot lot, boolean isSafehouse) {
        Footprint fp = FootprintGenerator.generate(lot, chunk_random);
        if (fp == null){
            //the lot is too small or too thin to hold anything; leave it as yard. The
            //generator has always been able to say this - nobody was listening, and with
            //every seed producing the same town the case simply never came up.
            return false;
        }
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

        //the yard is decorated in generate(), once the neighbours are up: a crate here is
        //clear of this building's doors, and then the house across the boundary punches its
        //own front door beside it. Nothing local to one building can see that coming.
        yards.add(building);   //the building's own rect is the lot it was cut from

        building.rooms = new ArrayList<Block>(rooms);
        building.roomList = rooms;

        //remember the police station so patrolmen spawn at its lobby
        if (building.type == BuildingType.POLICE_STATION){
            policeStation = building;
        }
        //remember the brothel too, so prostitutes can live and work there and customers
        //(and the model's "goto brothel") can route to its front door
        if (building.type == BuildingType.BROTHEL){
            brothel = building;
            RLWorldModel.brothelLocation = building.entrance != null
                    ? new Point(building.entrance) : new Point(building.getX() + 1, building.getY() + 1);
        }
        getBuildings().add(building);
        return true;
    }

    /** Trace the building outline: place a wall on every footprint-mask edge cell. */
    private void traceMask(GridMask m) {
        for (int i = 0; i < m.w; i++){
            for (int j = 0; j < m.h; j++){
                if (m.isEdge(i, j)){
                    //the mask edge IS the street-facing shell, so this is the expensive one
                    placeWall(m.ox + i, m.oy + j, SoundConfig.TL_WALL_OUTER);
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
        Point d = pickDoorTile(doors);
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
     * The most central tile on a shared wall that keeps this door clear of any gap already
     * knocked through (INVARIANTS C2) — walking outwards from the middle, so the door still
     * lands centrally when it can. A wall with nowhere clean left takes the middle anyway:
     * the room has to be reachable, and that outranks tidiness.
     */
    private Point pickDoorTile(List<Point> doors) {
        int mid = doors.size() / 2;
        for (int off = 0; off < 2 * doors.size(); off++){
            int i = (off % 2 == 0) ? mid + off / 2 : mid - (off / 2 + 1);
            if (i < 0 || i >= doors.size()){
                continue;
            }
            if (!nextToWallGap(doors.get(i).getX(), doors.get(i).getY())){
                return doors.get(i);
            }
        }
        return doors.get(mid);
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
            //the tiles arrive room by room and side by side, so consecutive entries can meet
            //around a corner: spacing alone lets two gaps end up touching (INVARIANTS C2)
            if (counter % TownGenConfig.WINDOW_SPACING == 0 && !nextToWallGap(e.p.getX(), e.p.getY())){
                placeWindow(e.p.getX(), e.p.getY());
            }
            counter++;
        }
    }

    /** Best available entrance wall, in descending order of desirability. */
    private ExtTile pickEntrance(List<ExtTile> exterior) {
        //an entrance beside an interior door punched by connect() breaks C2 just as surely
        List<ExtTile> clear = new ArrayList<ExtTile>();
        for (ExtTile e : exterior){
            if (!nextToWallGap(e.p.getX(), e.p.getY())){
                clear.add(e);
            }
        }
        if (!clear.isEmpty()){
            exterior = clear;
        }

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
    private void decorateYard(Building building) {
        GridMask m = building.footprint;
        for (int i = 0; i <= building.getW(); i++){
            for (int j = 0; j <= building.getH(); j++){
                int wx = building.getX() + i;
                int wy = building.getY() + j;
                if (m.getWorld(wx, wy)){
                    continue;   //inside the building
                }
                boolean adjacentToWall = m.getWorld(wx-1, wy) || m.getWorld(wx+1, wy)
                                      || m.getWorld(wx, wy-1) || m.getWorld(wx, wy+1);
                //never right outside a door or window - a crate on the step is a locked house.
                //isFreeFloor also keeps two lots from each dropping a crate on the shared
                //column between them: stacked, the repair pass has to lift them one per pass
                if (adjacentToWall && !nextToWallGap(wx, wy) && chunk_random.nextInt(100) < 5
                        && isFreeFloor(wx, wy)){
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
        //after clearWall, which zeroes the tile: glass is the thinnest thing in the shell,
        //and it is how a scream indoors ever reaches the street
        ((RLTile)getLayer().get_tile(x, y)).setSoundLoss(SoundConfig.TL_WINDOW);
    }



    /**
     Trace outer conture of block and mark every outer block as wall
     */
    private void traceBlock(Block block){
        for (int i = 0; i< block.getH()+1; i++){
            placeWall(block.getX(), block.getY()+i, SoundConfig.TL_WALL_INNER);
            placeWall(block.getX()+block.getW(), block.getY()+i, SoundConfig.TL_WALL_INNER);
        }
        for (int j = 0; j< block.getW()+1; j++){
            placeWall(block.getX()+j, block.getY(), SoundConfig.TL_WALL_INNER);
            placeWall(block.getX()+j, block.getY()+block.getH(), SoundConfig.TL_WALL_INNER);
        }
    }

    private void placeWall(int i, int j, int soundLoss){
        RLTile tile = (RLTile)(getLayer().get_tile(i,j));
        tile.setWall(true);
        //raise, never set: room perimeters are traced over the building shell, and an
        //exterior wall must not be downgraded to an interior one where a room touches it
        tile.raiseSoundLoss(soundLoss);
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
        //the masonry is gone; whatever plugs the hole sets its own loss after this
        tile.setSoundLoss(SoundConfig.TL_OPEN);
    }


    private WorldTile addTile(int i, int j, Random chunk_random) {
        WorldTile tile = new RLTile();
        Point origin = new Point(i,j);
        tile.origin = origin;

        getLayer().set_tile(origin, tile);

        return tile;
    }
}