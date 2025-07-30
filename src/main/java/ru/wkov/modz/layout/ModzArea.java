package ru.wkov.modz.layout;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.data.MapzItem;
import ru.wkov.modz.data.ModzItem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Integer.*;
import static javafx.util.Duration.ZERO;
import static javafx.util.Duration.millis;
import static ru.wkov.modz.ModzUtil.prop;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzArea extends StackPane implements ModzBean {

    private static final ParallelTransition TRANSITION = new ParallelTransition();

    private final List<List<ModzTile>> cache;

    private final IntegerProperty layer;

    private final Canvas canvas;

    private final ModzItem mod;

    private final MapzItem map;

    private final int size;

    private int offsetX;

    private int offsetY;

    public ModzArea(ModzItem mod, MapzItem map) {
        this.mod = mod;
        this.map = map;

        cache = new ArrayList<>(8);
        size = getTileSize();

        offsetX = MAX_VALUE;
        offsetY = MAX_VALUE;

        var path = getRootPath().resolve(Path.of("maps", map.hash()));
        if (Files.exists(path)) {
            try (var stream1 = Files.list(path)) {
                stream1.forEach(path1 -> {
                    var level = new LinkedList<ModzTile>();
                    cache.add(level);

                    try (var stream2 = Files.list(path1)) {
                        stream2.forEach(path2 -> {
                            var name = path2.getFileName().toString();

                            var u = name.indexOf('_');
                            var d = name.indexOf('.');

                            var x = parseInt(name.substring(0, u));
                            var y = parseInt(name.substring(u + 1, d));

                            offsetX = min(x, offsetX);
                            offsetY = min(y, offsetY);

                            level.add(new ModzTile(path2, size, x, y));
                        });
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                });
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        var width = (map.maxX() - map.minX() + 1) * size;
        var height = (map.maxY() - map.minY() + 1) * size;

        canvas = new Canvas(width, height);
        getChildren().add(canvas);

        layer = new SimpleIntegerProperty(-1);
        layer.addListener((prop, prev, next) -> {
            drawLayers(prev.intValue(), next.intValue());
        });

        map.colorProperty()
                .addListener(prop(() -> drawLayers(-1, layer.get())));

        setPrefSize(width, height);
        setTranslateX(map.minX() * size);
        setTranslateY(map.minY() * size);

        if (mod == null) {
            visibleProperty().bind(map.includedProperty());
            setViewOrder(MAX_VALUE);
            return;
        }

        visibleProperty().bind(mod.includedProperty().and(map.includedProperty()));

        var transition = new FadeTransition(millis(380), canvas);
        transition.setAutoReverse(true);
        transition.setCycleCount(-1);
        transition.setFromValue(1.0);
        transition.setToValue(0.0);

        mod.selectedProperty().addListener(prop(selected -> {
            if (selected) {
                TRANSITION.getChildren().add(transition);
            } else {
                TRANSITION.getChildren().remove(transition);
                transition.jumpTo(ZERO);
            }
            TRANSITION.playFromStart();
        }));

        var o = prop(() -> setViewOrder(mod.getPriority() * 100 + map.getPriority()));
        o.changed(null, null, null);

        map.priorityProperty().addListener(o);
        mod.priorityProperty().addListener(o);
    }

    public ModzItem getMod() {
        return mod;
    }

    public MapzItem getMap() {
        return map;
    }

    public int getLayer() {
        return layer.get();
    }

    public void setLayer(int layer) {
        this.layer.set(layer);
    }

    private void drawLayers(int prev, int next) {
        var ctx = canvas.getGraphicsContext2D();
        if (cache.isEmpty() || next < 0) {
            if (mod == null) {
                canvas.setOpacity(0.0);
            } else {
                canvas.setOpacity(0.5);

                ctx.setFill(map.getColor());
                ctx.clearRect(0, 0, getPrefWidth(), getPrefHeight());
                for (var tile : map.tiles()) {
                    ctx.fillRect((tile.x() - map.minX()) * size, (tile.y() - map.minY()) * size, size, size);
                }
            }
            return;
        } else {
            canvas.setOpacity(1.0);
        }

        for (int n = (prev < next ? prev + 1 : 0); n <= next && n < cache.size(); n++) {
            for (var tile : cache.get(n)) {
                var progress = tile.progressProperty();
                if (progress.get() < 1.0) {
                    var listener = new InvalidationListener() {

                        boolean removed;

                        @Override
                        public void invalidated(Observable unused) {
                            if (progress.get() < 1.0 || removed) {
                                return;
                            }

                            progress.removeListener(this);
                            removed = true;

                            ctx.drawImage(tile, (tile.x - offsetX) * size, (tile.y - offsetY) * size);
                        }
                    };

                    progress.addListener(listener);
                    listener.invalidated(null);
                } else {
                    ctx.drawImage(tile, (tile.x - offsetX) * size, (tile.y - offsetY) * size);
                }
            }
        }
    }

    private static class ModzTile extends Image {

        int x, y;

        ModzTile(Path path, int size, int x, int y) {
            super("file:/" + path.toAbsolutePath(), size, size, false, false, true);

            this.x = x;
            this.y = y;
        }
    }
}
