package ru.wkov.modz.event;

import javafx.event.EventType;
import javafx.scene.paint.Color;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzColor extends ModzEvent {

    public static final EventType<ModzColor> COLOR =
            new EventType<>(ModzEvent.MODZ, "COLOR");

    private final Color color;

    public ModzColor(Color color) {
        super(COLOR);
        this.color = color;
    }

    public Color getColor() {
        return color;
    }
}
