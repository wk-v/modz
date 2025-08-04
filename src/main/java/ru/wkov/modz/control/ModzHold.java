package ru.wkov.modz.control;

import javafx.animation.AnimationTimer;
import javafx.scene.Node;
import javafx.scene.control.Button;
import ru.wkov.modz.ModzBean;

import static javafx.scene.input.MouseEvent.*;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzHold extends Button implements ModzBean {

    private Runnable action;

    public ModzHold(Node graphic) {
        super("", graphic);

        var timer = new AnimationTimer() {

            final long diff = 280000000;

            long count;
            long hold;
            long prev;

            @Override
            public void handle(long time) {
                if (prev == 0) {
                    prev = time;
                    return;
                }

                if (time - prev > hold) {
                    action.run();

                    prev = time;

                    if (count++ % 10 == 0) {
                        hold = (long) (hold / 1.8);
                    }
                }
            }

            @Override
            public void start() {
                count = 0;
                prev = 0;
                hold = diff;
                super.start();
            }
        };

        addEventFilter(ANY, event -> {
            var type = event.getEventType();
            if (type == MOUSE_PRESSED) {
                action.run();
                timer.start();
            } else if (type == MOUSE_RELEASED) {
                timer.stop();
            }
        });
    }

    public void setOnHold(Runnable action) {
        this.action = action;
    }
}
