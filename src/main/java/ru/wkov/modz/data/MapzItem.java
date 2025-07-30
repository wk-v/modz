package ru.wkov.modz.data;

import javafx.beans.property.*;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.lang.Integer.*;
import static javafx.scene.paint.Color.BLACK;
import static javafx.scene.paint.Color.WHITE;
import static ru.wkov.modz.ModzUtil.*;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public record MapzItem(Path path,
                       String id,
                       String name,
                       String hash,
                       Integer minX,
                       Integer minY,
                       Integer maxX,
                       Integer maxY,
                       List<MapzTile> tiles,
                       BooleanProperty includedProperty,
                       IntegerProperty priorityProperty,
                       ObjectProperty<Color> colorProperty) {

    public static final MapzItem DEFAULT = new MapzItem(
            null,
            null,
            "Muldraugh, KY",
            "d213070d415bc6b70661302b59cad0bc_muldraugh_ky",
            0,
            0,
            65,
            52,
            List.of(),
            new SimpleBooleanProperty(true),
            new SimpleIntegerProperty(0),
            new SimpleObjectProperty<>(WHITE)
    );

    public static MapzItem valueOf(Path path) {
        try (var paths = Files.list(path)) {
            var range = new int[]{MAX_VALUE, MAX_VALUE, MIN_VALUE, MIN_VALUE};
            var tiles = paths
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(name -> {
                        var d = name.lastIndexOf(".lotheader");
                        if (d == -1) {
                            return null;
                        }

                        var u = name.indexOf('_');

                        var x = parseInt(name.substring(0, u));
                        var y = parseInt(name.substring(u + 1, d));

                        if (range[0] > x) range[0] = x;
                        if (range[1] > y) range[1] = y;
                        if (range[2] < x) range[2] = x;
                        if (range[3] < y) range[3] = y;

                        return new MapzTile(x, y);
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .toList();

            var id = path.getFileName().toString();

            var hash = path.getParent().getParent().getParent().getFileName().toString();
            hash = hash.replaceAll("\\W+", "_").toLowerCase();
            hash = md5Hex(hash) + '_' + id.replaceAll("\\W+", "_").toLowerCase();

            return new MapzItem(
                    path,
                    id,
                    id,
                    hash,
                    range[0],
                    range[1],
                    range[2],
                    range[3],
                    tiles,
                    new SimpleBooleanProperty(true),
                    new SimpleIntegerProperty(0),
                    new SimpleObjectProperty<>(tiles.isEmpty() ? BLACK : color(0.28, 0.88))
            );
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public boolean isIncluded() {
        return includedProperty.get();
    }

    public void setIncluded(boolean included) {
        includedProperty.set(included);
    }

    public int getPriority() {
        return priorityProperty.get();
    }

    public void setPriority(int priority) {
        priorityProperty.set(priority);
    }

    public Color getColor() {
        return colorProperty.get();
    }

    public void setColor(Color color) {
        colorProperty.set(color);
    }

    public boolean isEmpty() {
        return tiles.isEmpty();
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof MapzItem that &&
                Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return name;
        }
        return name + String.format(" [%s:%s-%s:%s]", minX, minY, maxX, maxY);
    }

    public Object[] export() {
        return new Object[]{
                path.toString(),
                id,
                name,
                hash,
                minX,
                minY,
                maxX,
                maxY,
                tiles.stream().map(MapzTile::export).toArray(),
                isIncluded(),
                web(getColor())
        };
    }

    public static MapzItem valueOf(Object export) {
        return valueOf((Object[]) export);
    }

    public static MapzItem valueOf(Object[] export) {
        return new MapzItem(
                Path.of((String) export[0]),
                (String) export[1],
                (String) export[2],
                (String) export[3],
                (Integer) export[4],
                (Integer) export[5],
                (Integer) export[6],
                (Integer) export[7],
                Arrays.stream((Object[]) export[8]).map(MapzTile::valueOf).toList(),
                new SimpleBooleanProperty((Boolean) export[9]),
                new SimpleIntegerProperty(0),
                new SimpleObjectProperty<>(Color.web((String) export[10]))
        );
    }
}
