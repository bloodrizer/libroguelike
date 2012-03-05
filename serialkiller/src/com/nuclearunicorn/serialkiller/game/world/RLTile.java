package com.nuclearunicorn.serialkiller.game.world;

import com.nuclearunicorn.libroguelike.game.world.WorldTile;

/*
Typical RogueLike tile - can be explored/unexplored, visible, etc
*/
public class RLTile extends WorldTile{
    private boolean isVisible = false;
    private boolean isExplored = false;
    private boolean isWall = false;
    private boolean isFovChecked = false;

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
}
