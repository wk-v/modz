package ru.wkov.modz.layout;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.controlsfx.control.HiddenSidesPane;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.control.*;
import ru.wkov.modz.data.ModzItem;
import ru.wkov.modz.data.ModzTile;

import java.util.LinkedList;
import java.util.List;

import static java.lang.Math.min;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.of;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Side.LEFT;
import static javafx.scene.input.MouseButton.*;
import static javafx.scene.input.MouseEvent.*;
import static javafx.scene.input.ScrollEvent.SCROLL;
import static javafx.scene.paint.Color.WHITE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignA.ALERT_CIRCLE_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignC.CIRCLE_OFF_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignC.CLOSE_BOX_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignG.GRID;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignG.GRID_OFF;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignL.LEAF;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignL.LEAF_OFF;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignM.*;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignN.*;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignP.PIN_OFF_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignP.PIN_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignV.*;
import static ru.wkov.modz.ModzUtil.list;
import static ru.wkov.modz.ModzUtil.prop;
import static ru.wkov.modz.control.ModzDraw.ModzLevel.*;
import static ru.wkov.modz.event.ModzReset.RESET;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzView extends StackPane implements ModzBean {

    private static final double DEFAULT_SCALE = 0.21762913579014873;

    private static final int HEIGHT = 6572;
    private static final int WIDTH = 8184;
    private static final int SIZE = 124;

    private static final int Y = HEIGHT / SIZE;
    private static final int X = WIDTH / SIZE;

    private final ModzRect[][] rects;

    public ModzView(ModzList list) {
        var base = new ModzDraw(WIDTH, HEIGHT, L0);
        var draw = new ModzDraw(WIDTH, HEIGHT, L1, L2, L3, L4, L5, L6, L7, LF, LZ);
        var grid = new ModzGrid(WIDTH, HEIGHT, SIZE);

        var hbox = new HBox();
        hbox.setMaxHeight(HEIGHT);
        hbox.setMinHeight(HEIGHT);
        hbox.setMaxWidth(WIDTH);
        hbox.setMinWidth(WIDTH);

        var view = new StackPane(base, draw, grid, hbox);
        view.setLayoutX(1000);
        view.setLayoutY(700);
        view.setManaged(false);

        var content = new StackPane(view);
        content.setScaleX(DEFAULT_SCALE);
        content.setScaleY(DEFAULT_SCALE);

        grid.deltaProperty()
                .bind(content.scaleXProperty());

        var pane = new HiddenSidesPane();
        pane.setPinnedSide(LEFT);
        pane.setContent(content);

        var hint = new ModzHint(hbox);
        hint.setVisible(true);

        var cell = new Label();
        cell.getStyleClass().clear();
        cell.setTextFill(WHITE);

        var desc = new LinkedList<Label>();

        var blinkedProperty = new SimpleBooleanProperty(true);
        var markedProperty = new SimpleBooleanProperty(true);
        var deltaProperty = view.rotateProperty().map(rotate -> -rotate.doubleValue());

        rects = new ModzRect[100][100];
        for (int x = 0; x < 100; x++) {
            var vbox = new VBox();
            hbox.getChildren().add(vbox);

            for (int y = 0; y < 100; y++) {
                var rect = rects[x][y] = new ModzRect(SIZE);
                vbox.getChildren().add(new StackPane(rect, rect.getMarker()));

                rect.blinkedProperty().bind(blinkedProperty);
                rect.markedProperty().bind(markedProperty);
                rect.deltaProperty().bind(deltaProperty);

                var game = x < X && y < Y;
                var text = "Cell: " + ModzTile.toString(x, y);

                var bindings = rect.getBindings();
                var nodes = hint.getChildren();

                rect.addEventFilter(ANY, event -> {
                    var type = event.getEventType();
                    if (type == MOUSE_ENTERED) {
                        if (hint.isVisible()) {
                            hint.setOpacity(game || rect.isEnabled() ? 1.0 : 0.0);
                        }

                        nodes.clear();

                        cell.setText(text);
                        nodes.add(cell);

                        var size = bindings.size() - desc.size();
                        for (int i = 0; i < size; i++) {
                            var node = new Label();
                            node.getStyleClass().clear();
                            desc.add(node);
                        }

                        for (int i = 0; i < desc.size(); i++) {
                            var node = desc.get(i);
                            if (i < bindings.size()) {
                                nodes.add(node);

                                var item = bindings.get(i);
                                node.setTextFill(item.getColor());
                                node.setText(item.getNum() + ": " + item.getTitle());
                            }
                        }
                    } else if (type == MOUSE_PRESSED && event.getButton() == PRIMARY && rect.isEnabled()) {
                        var model = list.getModel();
                        if (!event.isControlDown()) {
                            model.clearSelection();
                        }

                        model.select((ModzItem) rect.getUserData());
                        list.scrollSelected();
                    }
                });
            }
        }

        list.getUploaded().addListener(list((added, item) -> {
            for (var tile : item.getTiles()) {
                try {
                    var rect = rects[tile.x()][tile.y()];
                    if (added) {
                        rect.add(item);
                    } else {
                        rect.remove(item);
                    }
                } catch (Exception ex) {
                    getLogger().error("failed to {} tile: {}\n{}", added ? "bind" : "unbind", tile, item.getMapPath());
                }
            }
        }));

        var vbox = new VBox(
                new Button() {{
                    var icon = new ModzIcon(PIN_OUTLINE, PIN_OFF_OUTLINE);
                    setGraphic(icon);
                    setOnAction(event -> {
                        pane.setPinnedSide(null);
                        if (icon.nextState() == 0) {
                            pane.hide(); // fix bug
                            pane.setPinnedSide(LEFT);
                        }
                    });
                }},
                new Button() {{
                    var icon = new ModzIcon(MOTION_PLAY_OUTLINE, MOTION_PAUSE_OUTLINE);
                    setGraphic(icon);
                    setOnAction(event -> blinkedProperty.set(icon.nextState() == 0));
                }},
                new Button() {{
                    var icon = new ModzIcon(ALERT_CIRCLE_OUTLINE, CIRCLE_OFF_OUTLINE);
                    setGraphic(icon);
                    setOnAction(event -> markedProperty.set(icon.nextState() == 0));
                }},
                new Button() {{
                    var icon = new ModzIcon(MAP_MARKER_OUTLINE, MAP_MARKER_OFF_OUTLINE);
                    setGraphic(icon);
                    setOnAction(event -> hint.setVisible(icon.nextState() == 0));
                }},
                new MenuButton("", new ModzIcon(VIEW_GRID), new CustomMenuItem() {{
                    var slider = new Slider();
                    slider.setOrientation(VERTICAL);
                    slider.setValue(0.88);
                    slider.setMax(1.0);
                    slider.setMin(0.0);

                    hbox.opacityProperty()
                            .bind(slider.valueProperty());

                    setContent(slider);
                    setHideOnClick(false);
                }}),
                new Button() {{
                    var icon = new ModzIcon(GRID, GRID_OFF);
                    setGraphic(icon);
                    setOnAction(event -> grid.setVisible(icon.nextState() == 0));
                }},
                new Button() {{
                    var icon = new ModzIcon(LEAF_OFF, LEAF);
                    setGraphic(icon);
                    setOnAction(event -> draw.setState(LF, icon.nextState() != 0));
                }},
                new Button() {{
                    var icon = new ModzIcon(VIRUS_OFF_OUTLINE, VIRUS_OUTLINE);
                    setGraphic(icon);
                    setOnAction(event -> draw.setState(LZ, icon.nextState() != 0));
                }},
                new MenuButton() {{
                    var icon = new ModzIcon(
                            CLOSE_BOX_OUTLINE,
                            NUMERIC_0_BOX_OUTLINE,
                            NUMERIC_1_BOX_MULTIPLE_OUTLINE,
                            NUMERIC_2_BOX_MULTIPLE_OUTLINE,
                            NUMERIC_3_BOX_MULTIPLE_OUTLINE,
                            NUMERIC_4_BOX_MULTIPLE_OUTLINE,
                            NUMERIC_5_BOX_MULTIPLE_OUTLINE,
                            NUMERIC_6_BOX_MULTIPLE_OUTLINE,
                            NUMERIC_7_BOX_MULTIPLE_OUTLINE
                    );

                    setGraphic(icon);

                    var items = List.of(
                            new CustomMenuItem(new ModzIcon(CLOSE_BOX_OUTLINE), false),
                            new CustomMenuItem(new ModzIcon(NUMERIC_0_BOX_OUTLINE), false),
                            new CustomMenuItem(new ModzIcon(NUMERIC_1_BOX_MULTIPLE_OUTLINE), false),
                            new CustomMenuItem(new ModzIcon(NUMERIC_2_BOX_MULTIPLE_OUTLINE), false),
                            new CustomMenuItem(new ModzIcon(NUMERIC_3_BOX_MULTIPLE_OUTLINE), false),
                            new CustomMenuItem(new ModzIcon(NUMERIC_4_BOX_MULTIPLE_OUTLINE), false),
                            new CustomMenuItem(new ModzIcon(NUMERIC_5_BOX_MULTIPLE_OUTLINE), false),
                            new CustomMenuItem(new ModzIcon(NUMERIC_6_BOX_MULTIPLE_OUTLINE), false),
                            new CustomMenuItem(new ModzIcon(NUMERIC_7_BOX_MULTIPLE_OUTLINE), false)
                    );

                    var states = of(L1, L2, L3, L4, L5, L6, L7)
                            .collect(toMap(identity(), level -> false));

                    base.setVisible(false);

                    for (int i = 0; i < items.size(); i++) {
                        var item = items.get(i);
                        getItems().add(item);

                        var state = i;
                        item.setOnAction(event -> {
                            if (!base.getState(L0)) {
                                base.setState(L0, true);
                            }
                            base.setVisible(state > 0);

                            states.replaceAll((level, v) -> level.ordinal() < state);
                            draw.setStates(states);

                            icon.setState(state);
                        });
                    }
                }}
        );
        vbox.getStyleClass().add("modz-vbox-btns");
        vbox.widthProperty().addListener(prop(false, width -> {
            pane.setTriggerDistance(width.doubleValue());
            setMinWidth(width.doubleValue());
        }));
        pane.setLeft(vbox);

        getChildren().add(pane);

        addEventFilter(ANY, event -> {
            var type = event.getEventType();
            var button = event.getButton();

            var prevX = content.getTranslateX();
            var prevY = content.getTranslateY();

            if (type == MOUSE_MOVED || type == MOUSE_DRAGGED) {
                var nextX = event.getX() - content.getWidth() / 2.0;
                var nextY = event.getY() - content.getHeight() / 2.0;

                content.setTranslateX(nextX);
                content.setTranslateY(nextY);

                if (button != SECONDARY) {
                    prevX = (nextX - prevX) / content.getScaleX();
                    prevY = (nextY - prevY) / content.getScaleY();

                    view.setTranslateX(view.getTranslateX() - prevX);
                    view.setTranslateY(view.getTranslateY() - prevY);
                }
            } else if (type == MOUSE_CLICKED && button == MIDDLE) {
                if (event.isShiftDown()) {
                    if (event.isSecondaryButtonDown()) {
                        view.setTranslateX(0.0);
                        view.setTranslateY(0.0);
                    } else {
                        view.setTranslateX(0.0 - prevX / content.getScaleX());
                        view.setTranslateY(0.0 - prevY / content.getScaleY());
                    }
                } else if (event.isControlDown()) {
                    content.setScaleX(DEFAULT_SCALE);
                    content.setScaleY(DEFAULT_SCALE);
                } else if (event.isAltDown()) {
                    view.setRotate(0.0);
                }
            }
        });

        addEventFilter(SCROLL, event -> {
            var delta = event.getDeltaX();
            if (delta == 0.0) {
                delta = event.getDeltaY();
                if (delta == 0.0) {
                    return;
                }
            }

            var scale = min(content.getScaleX() * (delta > 0.0 ? 1.1 : 1 / 1.1), 1.0);

            if (event.isControlDown()) {
                if (0.1 < scale && scale <= 1.0) {
                    content.setScaleX(scale);
                    content.setScaleY(scale);
                }
            } else if (event.isShiftDown()) {
                view.setTranslateX(view.getTranslateX() + delta / scale);
            } else if (event.isAltDown()) {
                view.setRotate(view.getRotate() - delta / 8);
            } else {
                view.setTranslateY(view.getTranslateY() + delta / scale);
            }
        });

        getMainStage().addEventFilter(RESET, event -> {
            content.setScaleX(DEFAULT_SCALE);
            content.setScaleY(DEFAULT_SCALE);

            view.setTranslateX(0.0 - content.getTranslateX() / content.getScaleX());
            view.setTranslateY(0.0 - content.getTranslateY() / content.getScaleY());
            view.setRotate(0.0);
        });
    }
}
