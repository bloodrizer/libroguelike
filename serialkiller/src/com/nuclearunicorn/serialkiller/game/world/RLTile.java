package com.nuclearunicorn.serialkiller.game.world;

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

    //how much blood this tile is covered with
    float bloodAmt = 0.0f;

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
