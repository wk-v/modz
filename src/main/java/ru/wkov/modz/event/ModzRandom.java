package ru.wkov.modz.event;

import javafx.event.EventType;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzRandom extends ModzEvent {

    public static final EventType<ModzRandom> RANDOM =
            new EventType<>(ModzEvent.MODZ, "RANDOM");

    public ModzRandom() {
        super(RANDOM);
    }
}
