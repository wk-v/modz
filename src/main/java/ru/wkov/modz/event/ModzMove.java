package ru.wkov.modz.event;

import javafx.event.EventType;
import ru.wkov.modz.data.ModzItem;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzMove extends ModzEvent {

    public static final EventType<ModzMove> MOVE =
            new EventType<>(ModzEvent.MODZ, "MOVE");

    public static final EventType<ModzMove> ABOVE =
            new EventType<>(ModzMove.MOVE, "ABOVE");

    public static final EventType<ModzMove> BELOW =
            new EventType<>(ModzMove.MOVE, "BELOW");

    public static final EventType<ModzMove> SWAP =
            new EventType<>(ModzMove.MOVE, "SWAP");

    private final ModzItem item1;

    private final ModzItem item2;

    public ModzMove(EventType<ModzMove> type,
                    ModzItem item1,
                    ModzItem item2) {
        super(type);

        this.item1 = item1;
        this.item2 = item2;
    }

    public ModzItem getItem1() {
        return item1;
    }

    public ModzItem getItem2() {
        return item2;
    }
}
