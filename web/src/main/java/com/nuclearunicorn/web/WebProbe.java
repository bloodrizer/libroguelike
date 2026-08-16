package com.nuclearunicorn.web;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;
import rlforj.los.ILosBoard;
import rlforj.los.PrecisePermissive;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Diagnostic entry point, selected with -Dweb.mainClass=com.nuclearunicorn.web.WebProbe.
 *
 * Isolates the pieces the game leans on that TeaVM is most likely to implement
 * differently, so a failure can be attributed to the runtime rather than guessed
 * at from a game-sized stack trace.
 */
public class WebProbe {

    private static final StringBuilder log = new StringBuilder();

    public static void main(String[] args) {
        listIteratorProbe();
        fovProbe();

        HTMLElement status = Window.current().getDocument().getElementById("status");
        if (status != null) {
            status.setInnerHTML("<pre>" + log + "</pre>");
        }
    }

    private static void say(String s) {
        log.append(s).append('\n');
        System.out.println(s);
    }

    /** rlforj's FOV drives a ListIterator with add()/previous(); check those agree with the JDK. */
    private static void listIteratorProbe() {
        try {
            LinkedList<String> list = new LinkedList<>();
            list.add("a");
            list.add("c");

            ListIterator<String> it = list.listIterator();
            it.next();              // -> a
            it.add("b");            // insert before cursor: [a, b, c]
            String prev = it.previous();
            say("listIterator: list=" + list + " previous-after-add=" + prev
                    + " (JDK: [a, b, c] / b)");

            ListIterator<String> it2 = list.listIterator();
            it2.next();
            it2.remove();
            say("listIterator: after remove=" + list + " (JDK: [b, c])");

            // Iterating while inserting is the pattern rlforj actually uses.
            LinkedList<Integer> nums = new LinkedList<>();
            nums.add(1);
            nums.add(2);
            ListIterator<Integer> it3 = nums.listIterator();
            while (it3.hasNext()) {
                int v = it3.next();
                if (v == 1) {
                    it3.add(99);
                }
            }
            say("listIterator: insert-during-scan=" + nums + " (JDK: [1, 99, 2])");

            // rlforj only ever calls getLast/addLast/listIterator/isEmpty, so the
            // interesting case is whether an insert made through the iterator is
            // visible to getLast().
            LinkedList<String> tail = new LinkedList<>();
            tail.addLast("x");
            ListIterator<String> it4 = tail.listIterator();
            it4.next();
            it4.add("y");           // appended at the very end: [x, y]
            say("getLast after iterator add at end: list=" + tail
                    + " getLast=" + tail.getLast() + " size=" + tail.size()
                    + " (JDK: [x, y] / y / 2)");

            LinkedList<String> head = new LinkedList<>();
            head.addLast("x");
            ListIterator<String> it5 = head.listIterator();
            it5.add("w");           // inserted before everything: [w, x]
            say("getLast after iterator add at head: list=" + head
                    + " getLast=" + head.getLast() + " size=" + head.size()
                    + " (JDK: [w, x] / x / 2)");

            LinkedList<String> del = new LinkedList<>();
            del.addLast("p");
            del.addLast("q");
            ListIterator<String> it6 = del.listIterator();
            it6.next();
            it6.next();
            it6.remove();           // drop the tail through the iterator
            say("getLast after iterator remove of tail: list=" + del
                    + " getLast=" + del.getLast() + " size=" + del.size()
                    + " (JDK: [p] / p / 1)");
            // rlforj's CLikeIterator.insertBeforeCurrent is exactly previous()+add();
            // its whole algorithm depends on next() still returning the same element.
            LinkedList<String> ins = new LinkedList<>();
            ins.add("A");
            ins.add("B");
            ListIterator<String> it7 = ins.listIterator();
            String cur = it7.next();            // cur = A, cursor after A
            it7.previous();                     // step back over A
            it7.add("NEW");                     // insert before A
            String afterInsert = it7.next();    // JDK: A again
            say("insertBeforeCurrent(cur=" + cur + "): list=" + ins
                    + " next-after-insert=" + afterInsert + " (JDK: [NEW, A, B] / A)");

            // Same, but with the cursor on the last element.
            LinkedList<String> ins2 = new LinkedList<>();
            ins2.add("A");
            ins2.add("B");
            ListIterator<String> it8 = ins2.listIterator();
            it8.next();
            String cur2 = it8.next();           // cur = B, cursor at end
            it8.previous();
            it8.add("NEW");
            say("insertBeforeCurrent(cur=" + cur2 + ") at tail: list=" + ins2
                    + " hasNext=" + it8.hasNext() + " next=" + (it8.hasNext() ? it8.next() : "-")
                    + " (JDK: [A, NEW, B] / true / B)");
        } catch (Throwable t) {
            say("listIterator PROBE FAILED: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    /** The FOV call the game makes every turn, on a trivial open board. */
    private static void fovProbe() {
        try {
            final boolean[][] seen = new boolean[21][21];
            ILosBoard board = new ILosBoard() {
                @Override
                public boolean contains(int x, int y) {
                    return x >= 0 && y >= 0 && x < 21 && y < 21;
                }

                @Override
                public boolean isObstacle(int x, int y) {
                    return false;
                }

                @Override
                public void visit(int x, int y) {
                    if (contains(x, y)) {
                        seen[x][y] = true;
                    }
                }
            };

            new PrecisePermissive().visitFieldOfView(board, 10, 10, 5);

            int count = 0;
            for (boolean[] row : seen) {
                for (boolean b : row) {
                    if (b) {
                        count++;
                    }
                }
            }
            say("PrecisePermissive open board: visited " + count + " tiles (expect ~80+)");
        } catch (Throwable t) {
            say("PrecisePermissive PROBE FAILED: " + t.getClass().getName() + ": " + t.getMessage());
        }

        // The game's boards are walled and its radii come from lighting, which can
        // be small; both are cheap to rule in or out.
        probeRadius(0, false);
        probeRadius(1, false);
        probeRadius(5, true);
        probeRadius(12, true);
    }

    private static void probeRadius(final int radius, final boolean walls) {
        try {
            ILosBoard board = new ILosBoard() {
                @Override
                public boolean contains(int x, int y) {
                    return x >= 0 && y >= 0 && x < 21 && y < 21;
                }

                @Override
                public boolean isObstacle(int x, int y) {
                    // A room with a doorway: the shape that exercises the bump lists.
                    return walls && (x == 5 || y == 5) && !(x == 5 && y == 8);
                }

                @Override
                public void visit(int x, int y) {
                }
            };
            new PrecisePermissive().visitFieldOfView(board, 10, 10, radius);
            say("PrecisePermissive radius=" + radius + " walls=" + walls + ": ok");
        } catch (Throwable t) {
            say("PrecisePermissive radius=" + radius + " walls=" + walls + " FAILED: "
                    + t.getClass().getName() + ": " + t.getMessage());
        }

        // Candidate replacement if PrecisePermissive stays broken under TeaVM.
        try {
            final int[] count = {0};
            ILosBoard board2 = new ILosBoard() {
                @Override
                public boolean contains(int x, int y) {
                    return x >= 0 && y >= 0 && x < 21 && y < 21;
                }

                @Override
                public boolean isObstacle(int x, int y) {
                    return walls && (x == 5 || y == 5) && !(x == 5 && y == 8);
                }

                @Override
                public void visit(int x, int y) {
                    count[0]++;
                }
            };
            com.nuclearunicorn.serialkiller.game.world.fov.FovFactory.create()
                    .visitFieldOfView(board2, 10, 10, radius);
            say("FovFactory        radius=" + radius + " walls=" + walls
                    + ": ok (" + count[0] + " visits)");
        } catch (Throwable t) {
            say("FovFactory        radius=" + radius + " walls=" + walls + " FAILED: "
                    + t.getClass().getName() + ": " + t.getMessage());
        }
    }
}
