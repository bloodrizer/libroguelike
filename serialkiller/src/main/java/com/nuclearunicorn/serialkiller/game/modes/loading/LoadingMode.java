package com.nuclearunicorn.serialkiller.game.modes.loading;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.modes.AbstractGameMode;
import com.nuclearunicorn.libroguelike.game.ui.IUserInterface;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlamaServerManager;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmConfig;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmRuntime;
import com.nuclearunicorn.serialkiller.game.ai.llm.ModelDownloader;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;

import static org.lwjgl.opengl.GL11.*;

/**
 * Boot screen shown before the world exists: stages the GGUF models (§10.1) and brings the
 * llama-server tiers up on a worker thread while the render loop keeps drawing progress.
 * Hands off to "inGame" once ready. With LLM NPCs disabled it falls through in one frame.
 */
public class LoadingMode extends AbstractGameMode implements IEventListener {

    /** How long a staging failure stays on screen before the game starts without LLM NPCs. */
    private static final long ERROR_DWELL_MS = 4000;
    private static final int BAR_W = 420;
    private static final int BAR_H = 14;

    private IUserInterface wgt = null;

    private ModelDownloader downloader;
    private String binaryWarning;
    private volatile boolean bootingServer = false;
    private volatile boolean ready = false;
    private long problemSince = 0;
    private boolean handedOff = false;

    @Override
    public void run() {
        new OverlaySystem();   //seeds the shared OverlaySystem.ttf we draw with

        LlmConfig config = LlmRuntime.peekConfig();
        if (!config.enabled) {
            ready = true;   // nothing to stage, drop straight into the game
            return;
        }

        //warn while the models download rather than after: a 2.5GB wait then canned
        //replies reads like the config never took effect
        if (!LlamaServerManager.isBinaryAvailable(config.serverBinary)) {
            binaryWarning = "'" + config.serverBinary + "' not on PATH - install llama.cpp for live NPCs";
        }

        //reactor only: the director tier gets staged here too once M2 actually boots it
        downloader = new ModelDownloader(config.reactor);

        Thread worker = new Thread(() -> {
            downloader.run();
            if (downloader.getError() != null) {
                LlmRuntime.disable("model staging failed");
            } else {
                bootingServer = true;
                LlmRuntime.init();   //spawns llama-server and blocks until /health is green
            }
            ready = true;
        }, "llm-staging");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public void update() {
        super.update();

        render();

        if (!ready) {
            return;
        }
        //let a failure sit on screen for a moment, otherwise hand off immediately
        if (problem() != null) {
            if (problemSince == 0) {
                problemSince = System.currentTimeMillis();
            }
            if (System.currentTimeMillis() - problemSince < ERROR_DWELL_MS) {
                return;
            }
        }
        if (!handedOff) {
            handedOff = true;
            getGameManager().set_state("inGame");
        }
    }

    private void render() {
        TrueTypeFont ttf = OverlaySystem.ttf;
        int y = WindowRender.get_window_h() / 2 - 40;

        drawCentered(ttf, y, "Preparing LLM NPCs", Color.white);

        if (downloader == null) {
            return;
        }

        //staging failure kills the tier outright; a dead server only downgrades it to canned plans
        if (ready && downloader.getError() != null) {
            drawCentered(ttf, y + 30, downloader.getError(), Color.red);
            drawCentered(ttf, y + 55, "starting without LLM NPCs...", Color.lightGray);
            return;
        }
        if (ready && LlmRuntime.degradedReason() != null) {
            drawCentered(ttf, y + 30, LlmRuntime.degradedReason(), Color.red);
            drawCentered(ttf, y + 55, "NPCs will use canned replies...", Color.lightGray);
            return;
        }

        if (bootingServer) {
            drawCentered(ttf, y + 30, "starting llama-server...", Color.yellow);
        } else {
            drawCentered(ttf, y + 30, downloader.getStatus(), Color.yellow);

            String size = downloader.getSizeLine();
            if (!size.isEmpty()) {
                float fraction = downloader.getFraction();
                drawBar(y + 60, fraction);
                String label = fraction < 0 ? size
                        : size + String.format("   %d%%", Math.round(fraction * 100));
                drawCentered(ttf, y + 85, label, Color.lightGray);
            }
        }

        //keep the missing-binary warning up for the whole wait, not just at the end
        if (binaryWarning != null) {
            drawCentered(ttf, y + 120, binaryWarning, Color.orange);
        }
    }

    /** What kept live inference from coming up, or null while things still look fine. */
    private String problem() {
        if (downloader != null && downloader.getError() != null) {
            return downloader.getError();
        }
        return LlmRuntime.degradedReason();
    }

    /** Framed fill bar; overlay coords are top-left origin with y growing down. */
    private static void drawBar(int y, float fraction) {
        int x = (WindowRender.get_window_w() - BAR_W) / 2;

        glDisable(GL_TEXTURE_2D);
        glLineWidth(1);

        glColor4f(0.45f, 0.45f, 0.45f, 1f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(x, y);
        glVertex2f(x + BAR_W, y);
        glVertex2f(x + BAR_W, y + BAR_H);
        glVertex2f(x, y + BAR_H);
        glEnd();

        if (fraction > 0) {
            float fill = (BAR_W - 4) * fraction;
            glColor4f(0.85f, 0.75f, 0.2f, 1f);
            glBegin(GL_QUADS);
            glVertex2f(x + 2, y + 2);
            glVertex2f(x + 2 + fill, y + 2);
            glVertex2f(x + 2 + fill, y + BAR_H - 2);
            glVertex2f(x + 2, y + BAR_H - 2);
            glEnd();
        }

        glColor4f(1, 1, 1, 1);
        glEnable(GL_TEXTURE_2D);
    }

    private static void drawCentered(TrueTypeFont ttf, int y, String text, Color color) {
        int x = (WindowRender.get_window_w() - ttf.getWidth(text)) / 2;
        ttf.drawString(x, y, text, color);
    }

    @Override
    public IUserInterface get_ui() {
        if (wgt != null) {
            return wgt;
        }
        wgt = new LoadingUI();
        return wgt;
    }

    @Override
    public void e_on_event(Event event) {
    }
}
