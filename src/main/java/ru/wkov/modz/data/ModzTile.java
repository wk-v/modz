package ru.wkov.modz.data;

import java.io.Serializable;

import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.rightPad;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public record ModzTile(int x, int y) implements Serializable {

    public ModzTile(String x, String y) {
        this(parseInt(x), parseInt(y));
    }

    @Override
    public String toString() {
        return toString(x, y);
    }

    public static String toString(int x, int y) {
        return rightPad(x + "x" + y, 5);
    }
}
