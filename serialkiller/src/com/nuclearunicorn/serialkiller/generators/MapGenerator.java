package com.nuclearunicorn.serialkiller.generators;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 04.03.12
 * Time: 18:11
 * To change this template use File | Settings | File Templates.
 */
public class MapGenerator {

    private int refSize;
    private int MIN_BLOCK_SIZE = 1800;
    private boolean MERGE_BLOCKS = true;

    private Random random = new Random();
    public int chunkSeed = 123456;

    public MapGenerator(Block block){
        refSize = block.getArea();
    }

    public void setSeed(int seed){
        chunkSeed = seed;
        random.setSeed(seed);
    }
    
    public int getMinBlockSize(){
        return MIN_BLOCK_SIZE;
    }


    public void setMinBlockSize(int size) {
        MIN_BLOCK_SIZE = size;
    }

    public ArrayList<Block> mergeBlocks(Block tl, Block tr, Block br, Block bl){
        Block[] blocks = new Block[] {tl, tr, br, bl};
        ArrayList<Block> mergedBlocks = new ArrayList<Block>();

        if (!MERGE_BLOCKS){

            mergedBlocks.add(tl);
            mergedBlocks.add(tr);
            mergedBlocks.add(br);
            mergedBlocks.add(bl);

            return mergedBlocks;
        }

        blocks[0].setNeighbors(new Block[]{tr,bl});     //tl    //<---- ???         shouldn't it be (tr,bl), (tr,br), (tl, bl), (tl, bl)?
        blocks[1].setNeighbors(new Block[]{tl,br});     //tr
        blocks[2].setNeighbors(new Block[]{tr,bl});     //br
        blocks[3].setNeighbors(new Block[]{tl,br});     //bl

        for(Block block : blocks){
            if(block != null && block.getArea() < getMinBlockSize()){
                /*
                    This code block is cheasy.
                    Why 4 at all, why not nextInt(block.size) ?
                    etc, etc
                 */
                int i = random.nextInt(4);
                if (blocks[i] != null && block.hasNeighbour(blocks[i])){
                    block.merge(blocks[i]);
                    blocks[i] = null;
                }
            }
        }
        
        for (Block block: blocks){
            //debug blocks there
        }

        for (Block block: blocks){
            if (block != null){
                mergedBlocks.add(block);
            }
        }
        return mergedBlocks;
    }

    /*
        Merge small town blocks until resulted blocks are not big enough
     */
    public List<Block> process(List<Block> blocks){
        if (blocks.size() == 1 && blocks.get(0).getArea() < getMinBlockSize()){
            return blocks;
        }
        
        List<Block> dividedBlocks = new ArrayList<Block>();
        for(Block subBlock: blocks){
            
            List<Block> splited;
            if ((Math.min(subBlock.getW(), subBlock.getH())*3) < Math.max(subBlock.getW(), subBlock.getH())){
                splited = halfSplit(subBlock);
            } else {
                splited = quadSplit(subBlock);
            }
            
            if (splited != null){
                for (Block block: splited ){
                    dividedBlocks.add(block);
                }
            }else{
                dividedBlocks.add(subBlock);
            }
        }
        
        if (dividedBlocks.size() <= blocks.size()){    //# or equals?
            return blocks;
        }

        dividedBlocks = process(dividedBlocks);
        
        return dividedBlocks;
    }
    
    public List<Block> halfSplit(Block block){

        if (block.getArea() < getMinBlockSize()){
            return null;
        }
        List<Block> blocks = new ArrayList<Block>();
        
        int x = block.getX();
        int y = block.getY();
        int w = block.getW();
        int h = block.getH();

        //horisontal split
        if (block.getH() > block.getW() ){

            int topOffset = (int)(h * ((float)(random.nextInt(20) + 40) / 100));

            blocks.add(new Block(x, y, w, topOffset));  //top
            blocks.add(new Block(x, y+topOffset, w, h-topOffset));   //bottom
        }else{
        //vertical split

            int leftOffset = (int)(w * ((float)(random.nextInt(20) + 40) / 100));

            blocks.add(new Block(x, y, leftOffset, h));  //left
            blocks.add(new Block(x+leftOffset, y, w-leftOffset, h));   //right
        }
        return blocks;
    }

    public List<Block> quadSplit(Block block){

        if (block.getArea() < getMinBlockSize()){
            return null;
        }
        List<Block> blocks = new ArrayList<Block>();

        int x = block.getX();
        int y = block.getY();
        int w = block.getW();
        int h = block.getH();

        int topOffset = (int)(h * ((float)(random.nextInt(20) + 40) / 100));
        int leftOffset = (int)(w * ((float)(random.nextInt(20) + 40) / 100));

        Block tl = new Block(x, y, leftOffset, topOffset);
        Block tr = new Block(x + leftOffset, y, w-leftOffset, topOffset);
        Block br = new Block(x + leftOffset, y+topOffset, (w-leftOffset), (h-topOffset));
        Block bl = new Block(x, y+topOffset, leftOffset, (h-topOffset));

        blocks = mergeBlocks(tl, tr, br, bl);

        return blocks;
    }

  

    public List<Block> roomProcess(List<Block> blocks) {
        //#some tricky algorythms there

        List<Block> rooms = new ArrayList<Block>();
        List<Block> splited = new ArrayList<Block>();

        //#TODO: make me a neat room
        for (Block subBlock : blocks){
            if (Math.min(subBlock.getW(),subBlock.getH())*2 < Math.max(subBlock.getW(), subBlock.getH())){
                splited = halfSplit(subBlock);    
            } else {
                splited = quadSplit(subBlock);
            }
            
            if (splited != null){
                for(Block sb: splited){
                    rooms.add(sb);
                }
            }
        }

        if (rooms.size() <= blocks.size()){
            return blocks;
        }

        rooms = roomProcess(rooms);
        return rooms;
    }
}
