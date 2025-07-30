package ru.wkov.modz.data;

import static java.lang.Integer.compare;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public record MapzTile(int x, int y) implements Comparable<MapzTile> {

    @Override
    public int compareTo(MapzTile that) {
        var result = compare(this.x, that.x);
        if (result == 0) {
            result = compare(this.y, that.y);
        }
        return result;
    }

    public Object[] export() {
        return new Object[]{x, y};
    }

    public static MapzTile valueOf(Object export) {
        return valueOf((Object[]) export);
    }

    public static MapzTile valueOf(Object[] export) {
        return new MapzTile((int) export[0], (int) export[1]);
    }
}
