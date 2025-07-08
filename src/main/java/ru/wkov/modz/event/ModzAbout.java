package ru.wkov.modz.event;

import javafx.event.EventType;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzAbout extends ModzEvent {

    public static final EventType<ModzAbout> ABOUT =
            new EventType<>(ModzEvent.MODZ, "ABOUT");

    public ModzAbout() {
        super(ABOUT);
    }
}
