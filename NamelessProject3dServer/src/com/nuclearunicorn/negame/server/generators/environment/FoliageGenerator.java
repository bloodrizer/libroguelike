package com.nuclearunicorn.negame.server.generators.environment;

import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.generators.ObjectGenerator;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.negame.server.game.world.entities.environment.foliage.EntityTree;
import org.lwjgl.util.Point;

import java.util.Random;

/**
 * @author Bloodrizer
 */
public class FoliageGenerator extends ObjectGenerator {

    public void generate_object(int x, int y, WorldTile tile, Random chunk_random){
        float tree_rate = 0.05f;

        int chance = (int)(chunk_random.nextFloat()*100);
        if (chance<tree_rate){
            add_tree(x, y);
        }
    }

    public void add_tree(int i, int j){
        EntityTree tree_ent = new EntityTree();

        tree_ent.setLayerId(WorldLayer.GROUND_LAYER);

        tree_ent.setEnvironment(environment);
        tree_ent.spawn(new Point(i, j));
        tree_ent.set_blocking(true);
    }

        /*public void generate_object(int x, int y, WorldTile tile, Random chunk_random){
            float tree_rate = 0.0f;

            if (tile.terrain_type == TerrainType.TERRAIN_WATER){
                return;
            }

            switch(tile.biome_type){
                case BIOME_TROPICAL_RAINFOREST:
                    tree_rate = 10;
                    break;

                case BIOME_SEASONAL_FOREST:
                    tree_rate = 3;
                    break;

                case BIOME_DECIDUOS_FOREST:
                    tree_rate = 3;
                    break;

                case BIOME_TEMP_RAINFOREST:
                    tree_rate = 3;
                    break;

                case BIOME_TAIGA:
                    tree_rate = 3;
                    break;

                case BIOME_GRASSLAND:
                    tree_rate = 1;
                    break;

                case BIOME_TEMP_DESERT:
                    tree_rate = 2;
                    break;

                case BIOME_SUBTROPICAL_DESERT:
                    tree_rate = 1;
                    break;
            }

            int chance = (int)(chunk_random.nextFloat()*100);
            if (chance<tree_rate){
                if (tile.biome_type == BiomeType.BIOME_TEMP_DESERT ||
                        tile.biome_type == BiomeType.BIOME_SUBTROPICAL_DESERT){
                    add_cacti(x,y);
                }else{
                    add_tree(x,y);
                }
            }
        }

        public void add_tree(int i, int j){
            EntityTree tree_ent = new EntityTree();

            tree_ent.setLayerId(WorldLayer.GROUND_LAYER);

            tree_ent.setEnvironment(environment);
            tree_ent.spawn(1, new Point(i,j));
            tree_ent.set_blocking(true);
        }*/
}
