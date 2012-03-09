/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.world;

import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.render.TilesetRenderer;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Point;

/**
 * @author Administrator
 */
public class WorldViewCamera {
    public static float camera_x = 0;
    public static float camera_y = 0;

    public static float cameraOffsetX = 0;
    public static float cameraOffsetY = 80;


    public static Point target = new Point(0, 0);
    public static float speed = 1.0f;

    public static boolean follow_target = true;

    public static void move(float dx, float dy) {
        camera_x += dx * 1.5;
        camera_y += dy * 1.5;
    }

    public static void set(float x, float y) {
        camera_x = x;
        camera_y = y;
    }

    public static void setMatrix() {
        Point position = get_position();
        GL11.glTranslatef(-position.getX(), -position.getY(), 0);
    }

    public static Point get_position() {
        return new Point((int) camera_x, (int) camera_y);
    }

    /*
    * Returns true, if tile is in current camera view area
    */
    public static boolean tile_in_fov(int i, int j) {
        int x = i*TilesetRenderer.TILE_SIZE;
        int y = j*TilesetRenderer.TILE_SIZE;

        /*camera_tile_x = (int)((camera_x + WindowRender.get_window_w()/2)/TilesetRenderer.TILE_SIZE);
        camera_tile_y = (int)((camera_y + WindowRender.get_window_h()/2)/TilesetRenderer.TILE_SIZE);*/

        return (x >= camera_x && x <= camera_x + WindowRender.get_window_w()-32) &&
                (y >= camera_y && y <= camera_y + WindowRender.get_window_h()-32);
    }

    /*
     * Moves camera closer to the destination, if camera is set to the "chasing mode"
     */
    public static void update() {
        if (follow_target) {
            //todo: move to math

            if (target == null || Player.get_ent() == null) {
                return;
            }

            //TODO: FIXME! HERE IS AN UGLY HACK!

            float target_x = (target.getX()) * TilesetRenderer.TILE_SIZE + Player.get_ent().dx * TilesetRenderer.TILE_SIZE;
            float target_y = (target.getY()) * TilesetRenderer.TILE_SIZE + Player.get_ent().dy * TilesetRenderer.TILE_SIZE
                    + WorldView.getYOffset(Player.get_ent().tile);

            Point delta = WorldView.world2local(new Point((int) target_x, (int) target_y));
            target_x = delta.getX() + cameraOffsetX;
            target_y = delta.getY() + cameraOffsetY;

            int dxt = WindowRender.get_window_w() / TilesetRenderer.TILE_SIZE / 2;
            int dyt = WindowRender.get_window_h() / TilesetRenderer.TILE_SIZE / 2;

            float dx = target_x - dxt * TilesetRenderer.TILE_SIZE - camera_x;
            float dy = target_y - dyt * TilesetRenderer.TILE_SIZE - camera_y;


            double distance = Math.sqrt(dx * dx + dy * dy);

            //normalize it to length 1 (preserving direction), then round it and
            //convert to integer so the movement is restricted to the map grid
            /*float acceleration = 0.0f;
            if (distance > 10) {
                acceleration = 5.0f;
            }
            if (distance < 2) {
                acceleration = 0.0f;
            }*/

            dx = (float) (Math.round(dx / distance) * (distance / 4));
            dy = (float) (Math.round(dy / distance) * (distance / 4));

            move(dx, dy);
        }
    }


}
