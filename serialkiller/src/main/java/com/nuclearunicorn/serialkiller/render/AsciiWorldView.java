package com.nuclearunicorn.serialkiller.render;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.world.WorldModel;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.game.world.WorldViewCamera;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;

import static org.lwjgl.opengl.GL11.glLoadIdentity;

/**
 * World view for the pseudo-isometric ASCII/pixel hybrid.
 *
 * The base class walks whole chunks and lets every entity draw itself in list
 * order; that cannot express depth. This one hands the frame to
 * {@link SceneRenderer}, which walks only what is on screen and draws it row by
 * row so tall things overlap correctly, then applies the light field.
 */
public class AsciiWorldView extends WorldView {

    private final SceneRenderer scene = new SceneRenderer();

    public AsciiWorldView(WorldModel model) {
        super(model);
    }

    public SceneRenderer getScene() {
        return scene;
    }

    @Override
    public void render() {
        glLoadIdentity();

        WorldViewCamera.update();
        WorldViewCamera.setMatrix();

        WorldLayer layer = ClientGameEnvironment.getWorldLayer(get_zindex());
        if (layer != null) {
            scene.render(layer);
        }

        glLoadIdentity();
    }
}
