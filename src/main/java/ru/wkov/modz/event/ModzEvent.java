package ru.wkov.modz.event;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public abstract class ModzEvent extends Event {

    static final EventType<ModzEvent> MODZ =
            new EventType<>(Event.ANY, "MODZ");

    ModzEvent(EventType<? extends ModzEvent> type) {
        super(type);
    }
}
