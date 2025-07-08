package ru.wkov.modz.control;

import javafx.animation.FillTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.data.ModzItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static java.util.Comparator.comparing;
import static javafx.scene.paint.Color.TRANSPARENT;
import static javafx.util.Duration.ZERO;
import static javafx.util.Duration.millis;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignN.*;
import static ru.wkov.modz.ModzUtil.prop;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzRect extends Rectangle implements ModzBean {

    private static final List<FillTransition> TRANSITIONS = new LinkedList<>();

    private static final Comparator<ModzItem> COMPARATOR =
            comparing(ModzItem::isChecked).reversed().thenComparing(ModzItem::getPriority);

    private static final double STROKE_WIDTH = 2.0;

    private final ChangeListener<Object> refresher;

    private final FillTransition transition;

    private final List<ModzItem> bindings;

    private final BooleanProperty blinked;

    private final BooleanProperty marked;

    private final DoubleProperty delta;

    private final ModzIcon marker;

    private boolean enabled;

    public ModzRect(int size) {
        super(size - STROKE_WIDTH, size - STROKE_WIDTH);

        refresher = prop(this::refresh);

        bindings = new ArrayList<>();

        marker = new ModzIcon();
        marker.setMouseTransparent(true);
        marker.setIconSize(size);

        blinked = new SimpleBooleanProperty(true);
        marked = new SimpleBooleanProperty(false);
        delta = marker.rotateProperty();

        blinked.addListener(refresher);
        marked.addListener(refresher);

        transition = new FillTransition(millis(500));
        transition.setAutoReverse(true);
        transition.setCycleCount(-1);

        TRANSITIONS.add(transition);

        setStrokeWidth(STROKE_WIDTH);
        refresh();
    }

    public void refresh() {
        transition.jumpTo(ZERO);
        transition.stop();

        enabled = false;

        var userData = (ModzItem) null;
        var stroke = TRANSPARENT;
        var color = TRANSPARENT;
        var fill = TRANSPARENT;

        var selected = false;
        var from = TRANSPARENT;
        var to = TRANSPARENT;

        var shape = (Shape) this;

        bindings.sort(COMPARATOR);

        var iterator = bindings.iterator();
        if (iterator.hasNext()) {
            var item = iterator.next();
            enabled = item.isChecked();

            if (enabled) {
                selected = item.isSelected();
                userData = item;

                fill = from = item.getColor();

                if (iterator.hasNext()) {
                    item = iterator.next();
                    if (item.isChecked()) {
                        if (item.isSelected()) {
                            if (selected || !blinked.get()) {
                                color = item.getColor();
                            } else {
                                selected = true;
                                shape = marker;
                                from = item.getColor();
                            }
                        } else {
                            color = to = item.getColor();
                        }
                    }
                }
            }
        }

        var count = (int) bindings.stream().filter(ModzItem::isChecked).count();

        if (marked.get() && count > 1) {
            marker.setIconCode(switch (count) {
                case 2 -> NUMERIC_2_CIRCLE;
                case 3 -> NUMERIC_3_CIRCLE;
                case 4 -> NUMERIC_4_CIRCLE;
                case 5 -> NUMERIC_5_CIRCLE;
                case 6 -> NUMERIC_6_CIRCLE;
                case 7 -> NUMERIC_7_CIRCLE;
                case 8 -> NUMERIC_8_CIRCLE;
                case 9 -> NUMERIC_9_CIRCLE;
                default -> NUMERIC_9_PLUS_CIRCLE;
            });
            marker.setIconColor(color);
            marker.setVisible(true);
        } else {
            marker.setVisible(false);
        }

        transition.setFromValue(from);
        transition.setToValue(to);
        transition.setShape(shape);

        if (enabled) {
            if (selected) {
                if (blinked.get()) {
                    transition.play();

                    TRANSITIONS.forEach(transition ->
                            transition.jumpTo(ZERO));
                } else {
                    stroke = fill.invert();
                }
            }
            setOpacity(1.0);
        } else {
            setOpacity(0.0);
        }

        setUserData(userData);
        setStroke(stroke);
        setFill(fill);
    }

    public void add(ModzItem item) {
        item.priorityProperty().addListener(refresher);
        item.selectedProperty().addListener(refresher);
        item.checkedProperty().addListener(refresher);
        item.colorProperty().addListener(refresher);

        bindings.add(item);
        refresh();
    }

    public void remove(ModzItem item) {
        item.priorityProperty().removeListener(refresher);
        item.selectedProperty().removeListener(refresher);
        item.checkedProperty().removeListener(refresher);
        item.colorProperty().removeListener(refresher);

        bindings.remove(item);
        refresh();
    }

    public BooleanProperty blinkedProperty() {
        return blinked;
    }

    public BooleanProperty markedProperty() {
        return marked;
    }

    public DoubleProperty deltaProperty() {
        return delta;
    }

    public List<ModzItem> getBindings() {
        return bindings;
    }

    public ModzIcon getMarker() {
        return marker;
    }

    public Boolean isEnabled() {
        return enabled;
    }
}
