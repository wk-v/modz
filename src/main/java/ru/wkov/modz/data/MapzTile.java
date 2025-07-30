package ru.wkov.modz.data;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public record MapzTile(int x, int y) {

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
