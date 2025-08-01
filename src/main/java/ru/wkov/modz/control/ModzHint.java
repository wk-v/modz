package ru.wkov.modz.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.data.MapzTile;
import ru.wkov.modz.layout.ModzArea;

import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparingInt;
import static javafx.scene.control.ContentDisplay.GRAPHIC_ONLY;
import static javafx.scene.input.MouseEvent.ANY;
import static javafx.stage.WindowEvent.WINDOW_SHOWING;
import static javafx.util.Duration.INDEFINITE;
import static javafx.util.Duration.ZERO;
import static ru.wkov.modz.ModzUtil.prop;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzHint extends Tooltip implements ModzBean {

    private static final double CORR_SIZE = 12.0;

    private final BooleanProperty visible;

    private final List<Label> labels;

    private final Label coords;

    private final Label cell;

    private final VBox maps;

    public ModzHint(Node owner) {
        install(owner, this);

        visible = new SimpleBooleanProperty(false);
        visible.addListener(prop(value -> setOpacity(value ? 1.0 : 0.0)));

        labels = new ArrayList<>(10);
        coords = new Label();
        cell = new Label();
        maps = new VBox();

        setConsumeAutoHidingEvents(false);
        setContentDisplay(GRAPHIC_ONLY);
        setHideOnEscape(false);
        setShowDuration(INDEFINITE);
        setShowDelay(ZERO);
        setHideDelay(ZERO);
        setGraphic(new VBox(new HBox(new Label("Coords: "), coords), new HBox(new Label("  Cell: "), cell), maps));
        setVisible(true);

        var moving = new EventHandler<>() {

            final Robot robot = new Robot();

            @Override
            public void handle(Event event) {
                setX(robot.getMouseX() + CORR_SIZE);
                setY(robot.getMouseY() + CORR_SIZE);
            }
        };

        addEventFilter(WINDOW_SHOWING, moving);
        owner.addEventFilter(ANY, moving);
    }

    public String getCoords() {
        return coords.getText();
    }

    public String getCell() {
        return cell.getText();
    }

    @SuppressWarnings("all")
    public ObservableList<Label> getMaps() {
        return (ObservableList) maps.getChildren();
    }

    public BooleanProperty visibleProperty() {
        return visible;
    }

    public Boolean isVisible() {
        return visible.get();
    }

    public void setVisible(boolean visible) {
        this.visible.set(visible);
    }

    public void refresh(Point2D point, List<ModzArea> areas) {
        var x = point.getX();
        var y = point.getY();

        var size = getTileSize();
        var diff = 300.0 / size;

        var tile = new MapzTile((int) (x / size), (int) (y / size));

        coords.setText((int) (x * diff) + " x " + (int) (y * diff));
        cell.setText(tile.x() + " x " + tile.y());

        var i = 0;
        for (var area : areas) {
            var mod = area.getMod();
            if (mod.isIncluded()) {
                var map = area.getMap();
                if (map.isIncluded() && map.tiles().contains(tile)) {
                    if (labels.size() == i) {
                        labels.add(new Label());
                    }
                    var label = labels.get(i++);
                    label.setText(String.format("% 6d: %s - %s", mod.getPriority() + 1, mod.name(), map.name()));
                    label.setTextFill(map.getColor());
                    label.setUserData(mod.getPriority());
                }
            }
        }

        var maps = labels.subList(0, i);
        maps.sort(comparingInt(label -> (int) label.getUserData()));

        getMaps().setAll(maps);
    }
}
