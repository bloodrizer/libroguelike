package com.nuclearunicorn.negame.common.generators;

import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.world.Terrain;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.game.world.generators.ChunkGenerator;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.libroguelike.utils.NLTimer;
import com.nuclearunicorn.negame.common.world.GenericNETile;
import org.lwjgl.util.Point;

import com.nuclearunicorn.libroguelike.game.world.generators.ObjectGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 22.06.12
 * Time: 2:00
 * To change this template use File | Settings | File Templates.
 */
public abstract class GenericGroundChunkGenerator extends ChunkGenerator {

        protected List<ObjectGenerator> chunkObjectGenerators = new ArrayList<ObjectGenerator>();

        public GenericGroundChunkGenerator(){
            
        }

        @Override
        public void setEnvironment(GameEnvironment environment){
            super.setEnvironment(environment);

            for (ObjectGenerator gen: chunkObjectGenerators){
                gen.setEnvironment(environment);
            }
        }
    
        public void registerObjectGenerator(ObjectGenerator objGenerator){
            chunkObjectGenerators.add(objGenerator);
            if (environment != null ){
                objGenerator.setEnvironment(environment);
            }
        }

        @Override
        public void generate(WorldChunk chunk){

            NLTimer timer = new NLTimer();
            timer.push();

            Point origin = chunk.origin;

            Terrain.aquatic_tiles.clear();

            timer.push();

            Random chunk_random = new Random();
            chunk_random.setSeed(origin.getX()*10000+origin.getY());    //set chunk-specific seed

            //Thread.currentThread().dumpStack();
            //System.out.println("building data chunk @"+origin.toString());

            int x = chunk.origin.getX()* WorldChunk.CHUNK_SIZE;
            int y = chunk.origin.getY()*WorldChunk.CHUNK_SIZE;
            int size = WorldChunk.CHUNK_SIZE;

            final int OFFSET = WorldChunk.CHUNK_SIZE;
            //final int OFFSET = 0;

            //---------------------------------------------------------------------

            //Step 1. Generate heightmap

            /*
            * Iterate throught the chunk using offset (for smooth moisture map transition)
            * Store all aquatic-type tiles in temp array so we could quickly iterate them later
            */

            for (int i = x - OFFSET; i<x+size+OFFSET; i++ ){
                for (int j = y - OFFSET; j<y+size+OFFSET; j++){
                    if ( i>= x && i<x+size && j >=y && j < y+size){
                        build_chunk_tile(i, j, chunk_random);
                    }

                    if (Terrain.is_lake(Terrain.get_height(i, j))){
                        Terrain.aquatic_tiles.add(new Point(i,j));
                    }
                }
            }

            //---------------------------------------------------------------------
            //Step 2. Generate moisture map and biomes
            /*
            * Calculate tile moisture map based on distance from aqatic tiles
            * Assign biome type based on moisture amt and elevation
            *
            * Assign various ents (Trees, grass, etc) based on biome type
            */
            //---------------------------------------------------------------------

            //9k iterations
            for (int i = x; i<x+size; i++){
                for (int j = y; j<y+size; j++)
                {
                    WorldTile tile = getLayer().get_tile(i, j);
                    tile.moisture = Terrain.getHumidity(i, j);
                    //tile.moisture = Terrain.get_moisture(i, j);

                    //Do not calculate actual humidity. Get random perlin2d value instead
                    tile.update_biome_type();

                    if (tile.terrain_type != WorldTile.TerrainType.TERRAIN_WATER){
                        int biome_id = tile.biome_type.tile_id();
                        tile.set_tile_id(biome_id);
                    }
                    //TODO: this part probably need some future refactoring

                    //treeGenerator.generate_object(i, j, tile, chunk_random);
                    //stoneGenerator.generate_object(i, j, tile, chunk_random);
                    //grassGenerator.generate_object(i, j, tile, chunk_random);
                    //chestGenerator.generate_object(i, j, tile, chunk_random);
                }
            }

            //---------------------------------------------------------------------



            timer.pop("chunk @" + origin.getX() + "," + origin.getY());
        }

        //TODO: use environment.getWorldLayer there
        protected WorldLayer getLayer() {
            return environment.getWorldLayer(z_index);
        }

        //--------------------------------------------------------------------------
        protected GenericNETile build_chunk_tile(int i, int j, Random chunk_random){
            //OVERRIDE ME!
            return null;
        }
}
