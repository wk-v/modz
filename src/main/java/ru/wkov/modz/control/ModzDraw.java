package ru.wkov.modz.control;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import ru.wkov.modz.ModzBean;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static java.lang.Integer.parseInt;
import static java.nio.file.Files.list;
import static java.nio.file.Files.newInputStream;
import static javafx.scene.paint.Color.BLACK;
import static org.apache.commons.lang3.StringUtils.split;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzDraw extends Canvas implements ModzBean {

    private final Map<ModzLevel, List<ModzTile>> layers;

    private final Map<ModzLevel, Boolean> states;

    private final GraphicsContext context;

    private final CountDownLatch latch;

    public ModzDraw(double width, double height, ModzLevel... levels) {
        super(width, height);

        layers = new EnumMap<>(ModzLevel.class);
        states = new EnumMap<>(ModzLevel.class);

        context = getGraphicsContext2D();
        context.setImageSmoothing(false);
        context.setStroke(BLACK);

        latch = new CountDownLatch(levels.length);

        var mapz = Path.of("mapz");
        for (var level : levels) {
            var layer = new LinkedList<ModzTile>();
            var root = mapz.resolve(level.name());

            layers.put(level, layer);
            states.put(level, false);

            if (Files.exists(root)) {
                new Thread(() -> {
                    try (var paths = list(root)) {
                        paths.forEach(path -> {
                            try {
                                layer.add(new ModzTile(path));
                            } catch (IOException ex) {
                                throw new UncheckedIOException(ex);
                            }
                        });
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    } finally {
                        latch.countDown();
                    }
                }).start();
            } else {
                latch.countDown();
            }
        }
    }

    public void setStates(Map<ModzLevel, Boolean> states) {
        this.states.putAll(states);
        draw();
    }

    public void setState(ModzLevel level, boolean state) {
        states.put(level, state);
        draw();
    }

    private void draw() {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }

        var height = getHeight();
        var width = getWidth();

        context.clearRect(0, 0, width, height);

        states.forEach((level, state) -> {
            if (state) layers.get(level).forEach(tile ->
                    context.drawImage(tile, tile.x, tile.y));
        });
    }

    public enum ModzLevel {

        L0, L1, L2, L3, L4, L5, L6, L7, LF, LZ
    }

    private static class ModzTile extends Image {

        final int x;
        final int y;

        ModzTile(Path path) throws IOException {
            super(newInputStream(path));

            var name = path.getFileName().toString();
            var x_y = split(name, '_');

            x = parseInt(x_y[0]);
            y = parseInt(x_y[1]);
        }
    }
}
