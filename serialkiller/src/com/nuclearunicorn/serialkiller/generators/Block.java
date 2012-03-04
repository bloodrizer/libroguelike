package com.nuclearunicorn.serialkiller.generators;

import org.lwjgl.util.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 04.03.12
 * Time: 15:13
 * To change this template use File | Settings | File Templates.
 */
public class Block {
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }

    private int x;
    private int y;
    private int w;
    private int h;
    private List<Block> neighbors = new ArrayList<Block>();

    public Block(int x, int y, int w, int h){
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;

        //neighbours
    }

    public void moveTo(int x, int y){
        this.x = x;
        this.y = y;
    }
    
    public int getArea(){
        return this.w*this.h;
    }

    public void locate(int x, int y){
        this.x = x - w/2;
        this.y = y - h/2;
    }

    /*
    TODO: produce unexpected results for diagonal merging
     */
    public void merge(Block block){

        int i = Math.max(x+w, block.x + block.w);
        int j = Math.max(y+h, block.y + block.h);
        
        x = Math.min(x, block.x);
        y = Math.min(y, block.y);
        
        w = i - x;
        h = j - y;
    }

    public void draw(){
        /*def draw(self, char="X"):
        print "drawing region ["+str(self.x)+","+str(self.y)+","+str(self.x+self.w)+","+str(self.y+self.h)+"]"

        for i in range(self.h+1):
        libtcod.console_put_char(self.con, self.x, 	self.y+i, char, libtcod.red)
        libtcod.console_put_char(self.con, self.x+self.w, self.y+i, char, libtcod.red)

        for j in range(self.w+1):
        libtcod.console_put_char(self.con, self.x+j, 	self.y, char, libtcod.red)
        libtcod.console_put_char(self.con, self.x+j, 	self.y+self.h, char, libtcod.red)*/
    }

    public void scale(int dx, int dy){
        if (w + dx*2 > 0){
            w = w+dx*2;
            x = x-dx;
        }
        if (h + dy*2 > 0){
            h = h+dy*2;
            y = y-dy;
        }
    }
    /*
        Returns 4 array lists representing tiles that forms 4 outer walls of block (?) or not?
     */
    public List[] getOuterWall(Block building){
        List[] tiles = new List[4];
        for(int i=0; i<4; i++){
            tiles[i] = new ArrayList<Point>();
        }
        
        for(int i=0; i<=w; i++)
            for(int j=0; j<h; j++){
                int x = this.x+i;
                int y = this.y+j;
                
                if (x == building.x + building.w){
                    tiles[0].add(new Point(x,y));
                }
                if( x == building.x){
                    tiles[1].add(new Point(x,y));
                }
                if (y == building.y + building.h){
                    tiles[2].add(new Point(x,y));
                }
                if (y == building.y){
                    tiles[3].add(new Point(x,y));
                }
            }

        return tiles;
    }
    
    
    
    
    /*-------------------------------------------------------------------
    Door/Window generation helpers
    -------------------------------------------------------------------*/
    public boolean intersect(Block other){
        return (x <= other.x+other.w && x+w >= other.x &&
        y <= other.y+h && y+h >= other.y);    
    }
    
    public boolean isConnected(Block other){
        for (Block block: neighbors){
            if (block == other){
                return true;
            }
        }
        return false;
    }
    
    public Block getIntersection(Block other){
        Block block = new Block(
            Math.max(x, other.x),
            Math.max(y, other.y),
            Math.min(x+w, other.x+other.w)-Math.max(x,other.x),
            Math.min(y+h, other.y+other.h)-Math.max(y,other.y) 
        );
        //some obscure tweaks
        
        if (block.w <= 0 && block.h <=0){
            return null;
        }
        if (block.w == 0){
            block.w = 1;
        }
        if (block.h == 0){
            block.h = 1;
        }

        return block;
    }

    /*
        Get all tiles in the block area. If you need only outer wall of block,
        use getOuterWall() instead
     */
    public List<Point> getTiles(){
        List<Point> tiles = new ArrayList<Point>();
        
        for(int i =0; i<w; i++)
            for(int j=0; j<h; j++){
                Point point = new Point(x+i,y+j);
                tiles.add(point);
            }
        return tiles;
    }

    public void setNeighbors(Block[] blocks) {
        for(Block block: blocks){
            neighbors.add(block);
        }
    }

    public boolean hasNeighbour(Block block) {
        return neighbors.contains(block);
    }

    @Override
    public String toString(){
        return "["+x+","+y+"]("+w+","+h+")";
    }
}
