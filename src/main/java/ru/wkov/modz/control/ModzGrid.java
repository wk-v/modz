package ru.wkov.modz.control;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import ru.wkov.modz.ModzBean;

import static java.lang.Double.max;
import static javafx.scene.paint.Color.BLACK;
import static ru.wkov.modz.ModzUtil.prop;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzGrid extends Canvas implements ModzBean {

    private final GraphicsContext context;

    private final DoubleProperty delta;

    private final int size;

    public ModzGrid(double width, double height, int size) {
        super(width, height);
        this.size = size;

        context = getGraphicsContext2D();
        context.setImageSmoothing(false);
        context.setStroke(BLACK);

        var listener = prop(this::draw);

        delta = new SimpleDoubleProperty(1.0);
        delta.addListener(listener);

        visibleProperty()
                .addListener(listener);
    }

    public DoubleProperty deltaProperty() {
        return delta;
    }

    public double getDelta() {
        return delta.get();
    }

    public void setDelta(double delta) {
        this.delta.set(delta);
    }

    private void draw() {
        if (isVisible()) {
            var height = getHeight();
            var width = getWidth();

            context.clearRect(0, 0, width, height);
            context.setLineWidth(1.0 / max(0.11, delta.get()));

            for (int x = size; x <= width - size; x += size) {
                context.strokeLine(x, 0, x, height);
            }

            for (int y = size; y <= height - size; y += size) {
                context.strokeLine(0, y, width, y);
            }
        }
    }
}
