package com.nuclearunicorn.serialkiller.game.world;

import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import org.newdawn.slick.Color;

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


    private TileType tileType;
    
    private String tileModel = "";
    private Color tileModelColor;

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
}
