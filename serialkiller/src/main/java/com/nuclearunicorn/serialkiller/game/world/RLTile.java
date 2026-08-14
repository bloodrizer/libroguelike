package com.nuclearunicorn.serialkiller.game.world;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import org.newdawn.slick.Color;

import java.util.ArrayList;
import java.util.List;

/*
Typical RogueLike tile - can be explored/unexplored, visible, etc
*/
public class RLTile extends WorldTile {

    public enum TileType {
        GROUND,
        ROAD,
        WALL,
        GRASS
    }

    private boolean isVisible = false;
    private boolean isExplored = false;
    private boolean isWall = false;
    private boolean isFovChecked = false;

    //inside a building footprint - the renderer floors it differently
    private boolean isIndoor = false;

    //a door or window was punched here: walkable, but part of a wall run
    private boolean isWallGap = false;

    //how much blood this tile is covered with
    float bloodAmt = 0.0f;

    /**
     * dB a sound loses on entering this tile (SOUND_DESIGN.md 4.1). Zero for open ground.
     *
     * <p>Baked rather than derived. {@link Acoustics} floods thousands of cells per gunshot
     * and its inner loop must stay a flat array read — walking {@code ent_list} to ask
     * whether the thing standing here is a door would put an allocation and a type check
     * inside it. The generator knows the answer at build time and writes it once; the two
     * things that change it later (a door locking, a window breaking) write it themselves.
     */
    private byte soundLoss = 0;

    private TileType tileType;
    
    private String tileModel = "";
    private Color tileModelColor;

    List<EntityRLHuman> owners = new ArrayList<EntityRLHuman>();


    public void addOwner(EntityRLHuman npc) {
        owners.add(npc);
    }

    public List<EntityRLHuman> getOwners(){
        return owners;
    }

    public boolean isOwned(){
        return !owners.isEmpty();
    }


    public void setModel(String model){
        tileModel = model;
    }
    
    public void setModelColor(Color color){
        tileModelColor = color;
    }

    public String getTileModel() {
        return tileModel;
    }

    public Color getTileModelColor() {
        return tileModelColor;
    }

    public TileType getTileType() {
        return tileType;
    }

    public void setTileType(TileType tileType) {
        this.tileType = tileType;
    }

    //render-specific shit
    //todo:replace with tile.contentType, etc

    public boolean isFovChecked() {
        return isFovChecked;
    }

    public void setFovChecked(boolean fovChecked) {
        isFovChecked = fovChecked;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public boolean isExplored() {
        return isExplored;
    }

    public void setExplored(boolean explored) {
        isExplored = explored;
    }

    public boolean isIndoor() {
        return isIndoor;
    }

    public boolean isWallGap() {
        return isWallGap;
    }

    public void setWallGap(boolean wallGap) {
        isWallGap = wallGap;
    }

    public void setIndoor(boolean indoor) {
        isIndoor = indoor;
    }

    public boolean isWall() {
        return isWall;
    }

    public void setWall(boolean wall) {
        isWall = wall;
    }
    
    public boolean isBlocked(){
        if (isWall){
            return true;
        }
        return super.isBlocked();
    }

    /**
     * Blocked by something that will not move out of the way on its own: a wall, a tree, a
     * crate, a shut door, a window. This is the question a <i>route</i> asks, and it is not
     * the same as {@link #isBlocked()}, which also counts people.
     *
     * <p>People are excluded deliberately. They walk on; a pathfinder that treats the
     * neighbour standing in the doorway as masonry declares the house sealed and sends the
     * whole household in through a window instead.
     */
    public boolean isPathBlocked(){
        if (isWall){
            return true;
        }
        Object[] list = ent_list.toArray();
        for (int i = list.length-1; i >= 0; i--){
            Entity entity = (Entity)list[i];
            if (entity.is_blocking() && entity.controller == null){
                return true;
            }
        }
        return false;
    }

    public int getSoundLoss() {
        return soundLoss;
    }

    public void setSoundLoss(int loss) {
        soundLoss = (byte) Math.max(0, Math.min(127, loss));
    }

    /**
     * Set the loss only if it is louder-stopping than what is already here.
     *
     * <p>Room perimeters are traced after the building outline and the two overlap wherever
     * a room touches the street, so a plain setter would quietly downgrade an exterior wall
     * to an interior one along every such run — and rooms touch the outer wall constantly.
     */
    public void raiseSoundLoss(int loss) {
        if (loss > soundLoss) {
            setSoundLoss(loss);
        }
    }

    public float getBloodAmt() {
        return bloodAmt;
    }

    public void setBloodAmt(float bloodAmt) {
        if (bloodAmt > 1.0f){
            bloodAmt = 1.0f;
        }
        if (bloodAmt < 0.0f){
            bloodAmt = 0.0f;
        }
        this.bloodAmt = bloodAmt;
    }

    @Override
    public void update() {
        super.update();
        setBloodAmt( bloodAmt - 0.03f);
    }

    @Override
    public String debug() {
        return Float.toString(bloodAmt);
    }
}
