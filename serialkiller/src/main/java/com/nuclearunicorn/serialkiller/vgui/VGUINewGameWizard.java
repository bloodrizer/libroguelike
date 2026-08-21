package com.nuclearunicorn.serialkiller.vgui;

import com.nuclearunicorn.libroguelike.events.EKeyPress;
import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_Button;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_FrameModern;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_Text;
import com.nuclearunicorn.serialkiller.game.character.CharacterPreset;
import com.nuclearunicorn.serialkiller.game.character.CharacterPresets;
import com.nuclearunicorn.serialkiller.game.character.CharacterSetup;
import org.lwjgl.input.Keyboard;
import org.newdawn.slick.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * The new-game screen: pick a life, then start it.
 *
 * <p>The role list is an {@link NE_GUI_Text} with clickable lines - the same widget the
 * inventory list is built from, which is where the click-a-line-to-act idiom already lives.
 * Arrow keys and Enter work too, because this is a roguelike.
 */
public class VGUINewGameWizard extends NE_GUI_FrameModern {

    /** What the menu does once a life is chosen, or the screen is dismissed. */
    public interface Listener {
        void onBegin(CharacterPreset preset);
        void onCancel();
    }

    private static final Color SELECTED = Color.white;
    private static final Color UNSELECTED = Color.lightGray;
    private static final Color LABEL = new Color(140, 140, 140);
    private static final int DETAIL_CHARS = 32;   //details column width, in characters

    private final List<CharacterPreset> presets = CharacterPresets.all();
    private final Listener listener;
    private final NE_GUI_Text roleList;
    private final NE_GUI_Text details;

    private int selected = 0;

    public VGUINewGameWizard(Listener listener) {
        super(true);   //close button

        this.listener = listener;

        title = "Who are you?";
        set_tw(17);
        set_th(12);
        dragable = false;

        roleList = new NE_GUI_Text(){
            @Override
            protected void e_on_line_click(int lineId, EMouseClick clickEvent) {
                select(lineId);
            }
        };
        roleList.max_lines = presets.size();
        roleList.set_size(28, 50, 170, presets.size() * 18);
        roleList.dragable = false;
        add(roleList);

        details = new NE_GUI_Text();
        details.max_lines = 14;
        details.set_size(215, 50, 300, 260);
        details.dragable = false;
        details.solid = false;   //clicks fall through to the frame, not into the blurb
        add(details);

        NE_GUI_Button begin = new NE_GUI_Button(){
            @Override
            public void e_on_mouse_click(EMouseClick e) {
                begin();
            }
        };
        begin.set_tw(3);
        begin.set_coord(28, 330);
        begin.text = "Begin";
        begin.color = Color.lightGray;
        add(begin);

        NE_GUI_Button back = new NE_GUI_Button(){
            @Override
            public void e_on_mouse_click(EMouseClick e) {
                cancel();
            }
        };
        back.set_tw(3);
        back.set_coord(140, 330);
        back.text = "Back";
        back.color = Color.lightGray;
        add(back);

        //re-open on whatever was picked last, the way a menu ought to
        selected = indexOf(CharacterSetup.chosen());
    }

    /** Start a game as the highlighted preset. */
    public void begin() {
        CharacterPreset preset = presets.get(selected);
        CharacterSetup.choose(preset);
        listener.onBegin(preset);
    }

    public void cancel() {
        visible = false;
        listener.onCancel();
    }

    @Override
    public void on_close() {
        cancel();
    }

    @Override
    public void e_on_key_press(EKeyPress e) {
        switch (e.key){
            case Keyboard.KEY_UP:   case Keyboard.KEY_W:
                select(selected - 1);
                break;
            case Keyboard.KEY_DOWN: case Keyboard.KEY_S:
                select(selected + 1);
                break;
            case Keyboard.KEY_RETURN:
                begin();
                break;
        }
    }

    private void select(int index) {
        if (index < 0 || index >= presets.size()){
            return;   //no wrap-around: the ends of a six-item list are easy enough to see
        }
        selected = index;
    }

    private int indexOf(CharacterPreset preset) {
        int at = presets.indexOf(preset);
        return at < 0 ? 0 : at;
    }

    private void refresh() {
        roleList.clearLines();
        for (int i = 0; i < presets.size(); i++){
            CharacterPreset preset = presets.get(i);
            String label = (i == selected ? "> " : "  ") + preset.getName();
            roleList.add_line(label, i == selected ? SELECTED : UNSELECTED);
        }

        CharacterPreset preset = presets.get(selected);
        details.clearLines();
        details.add_line(preset.getName(), SELECTED);
        for (String line : wrap(preset.getBlurb())){
            details.add_line(line, UNSELECTED);
        }
        details.add_line("");

        if (preset.isWildcard()){
            details.add_line("Everything below the line is", LABEL);
            details.add_line("rolled when the game starts.", LABEL);
            return;
        }

        details.add_line("Sex:   " + preset.getGender().displayName(), LABEL);
        details.add_line("Age:   " + preset.ageRange(), LABEL);
        details.add_line("Start: " + preset.getSpawn().displayName(), LABEL);
        details.add_line("");
        details.add_line("Carrying:", LABEL);
        for (String line : wrap(preset.gearSummary())){
            details.add_line(line, UNSELECTED);
        }
    }

    /** Word-wrap to the details column. The text widget draws lines, not paragraphs. */
    private static List<String> wrap(String text) {
        List<String> lines = new ArrayList<String>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")){
            if (line.length() > 0 && line.length() + 1 + word.length() > DETAIL_CHARS){
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0){
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0){
            lines.add(line.toString());
        }
        return lines;
    }

    @Override
    public void render() {
        if (!visible){
            return;
        }
        refresh();
        super.render();
    }
}
