package ru.wkov.modz.event;

import javafx.event.EventType;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzScroll extends ModzEvent {

    public static final EventType<ModzScroll> SCROLL =
            new EventType<>(ModzEvent.MODZ, "SCROLL");

    private final int priority;

    private final boolean selected;

    private final boolean multiple;

    public ModzScroll(int priority, boolean selected, boolean multiple) {
        super(SCROLL);
        this.priority = priority;
        this.selected = selected;
        this.multiple = multiple;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isMultiple() {
        return multiple;
    }
}
