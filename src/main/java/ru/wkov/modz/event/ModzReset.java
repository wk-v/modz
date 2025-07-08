package ru.wkov.modz.event;

import javafx.event.EventType;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzReset extends ModzEvent {

    public static final EventType<ModzReset> RESET =
            new EventType<>(ModzEvent.MODZ, "RESET");

    public ModzReset() {
        super(RESET);
    }
}
