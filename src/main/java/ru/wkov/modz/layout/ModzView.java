package ru.wkov.modz.layout;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;
import org.kordamp.ikonli.javafx.FontIcon;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.control.ModzHint;
import ru.wkov.modz.data.MapzItem;
import ru.wkov.modz.data.ModzItem;
import ru.wkov.modz.event.ModzScroll;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Double.max;
import static java.lang.Double.min;
import static javafx.geometry.Pos.TOP_LEFT;
import static javafx.scene.input.MouseButton.*;
import static javafx.scene.input.MouseEvent.*;
import static javafx.scene.input.ScrollEvent.SCROLL;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignB.BALLOT_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignC.CONTENT_COPY;
import static ru.wkov.modz.ModzUtil.clipboard;
import static ru.wkov.modz.ModzUtil.prop;
import static ru.wkov.modz.event.ModzReset.RESET;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzView extends StackPane implements ModzBean {

    private static final double DEFAULT_SCALE = 0.07291326290219395;

    private final ObservableList<Node> areas;

    private final BooleanProperty overflow;

    private final ModzArea base;

    private final ModzOver over;

    public ModzView() {
        base = new ModzArea(null, MapzItem.DEFAULT);
        over = new ModzOver();

        overflow = over.visibleProperty();

        var view = new StackPane(over, base);
        view.setAlignment(TOP_LEFT);
        view.setLayoutX(600 - base.getPrefWidth() / 2);
        view.setLayoutY(400 - base.getPrefHeight() / 2);
        view.setManaged(false);

        areas = view.getChildren();

        var content = new StackPane(view);
        content.setScaleX(DEFAULT_SCALE);
        content.setScaleY(DEFAULT_SCALE);

        getChildren().add(content);

        var hint = new ModzHint(this);

        var coords = new MenuItem("", new FontIcon(CONTENT_COPY));
        coords.setOnAction(event -> clipboard(hint.getCoords()));

        var cell = new MenuItem("", new FontIcon(CONTENT_COPY));
        cell.setOnAction(event -> clipboard(hint.getCell()));

        var menu = new Menu("scroll to", new FontIcon(BALLOT_OUTLINE));
        var items = new ArrayList<MenuItem>(10);

        var ctxm = new ContextMenu(coords, cell, menu);

        addEventFilter(ANY, event -> {
            var type = event.getEventType();
            var button = event.getButton();

            if (type == MOUSE_PRESSED && button == PRIMARY) {
                var maps = hint.getMaps();
                if (!maps.isEmpty()) {
                    var priority = (int) maps.get(0).getUserData();
                    getMainStage()
                            .fireEvent(new ModzScroll(priority, true, event.isControlDown()));
                }
            }

            if (type == MOUSE_RELEASED && button == SECONDARY && event.isDragDetect() && !ctxm.isShowing()) {
                coords.setText(hint.getCoords());
                cell.setText(hint.getCell());

                var i = 0;
                for (var map : hint.getMaps()) {
                    if (items.size() == i) {
                        var item = new MenuItem();
                        item.setGraphic(new Label());
                        item.setOnAction(e -> getMainStage()
                                .fireEvent(new ModzScroll((int) item.getUserData(), false, false)));
                        items.add(item);
                    }
                    var item = items.get(i++);
                    // item.setText(map.getText()); shows incorrect text
                    ((Label) item.getGraphic()).setText(map.getText());
                    item.setUserData(map.getUserData());
                }
                menu.getItems().setAll(items.subList(0, i));
                menu.setVisible(!menu.getItems().isEmpty());

                ctxm.show(getMainStage(), event.getScreenX(), event.getScreenY());
                return;
            }

            hint.refresh(view.screenToLocal(event.getScreenX(), event.getScreenY()), getAreas(2));

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
                    view.getTransforms().clear();
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

            var scale = max(min(content.getScaleX() * (delta > 0.0 ? 1.1 : 1 / 1.1), 3.0), 0.07);

            if (event.isControlDown()) {
                if (0.07 < scale && scale <= 3.0) {
                    content.setScaleX(scale);
                    content.setScaleY(scale);
                }
            } else if (event.isShiftDown()) {
                view.setTranslateX(view.getTranslateX() + delta / scale);
            } else if (event.isAltDown()) {
                var rotate = view.getRotate() - delta / 8;
                if (rotate < 0)
                    rotate += 360;
                if (rotate >= 360)
                    rotate = 0;

                var bounds = view.sceneToLocal(event.getSceneX(), event.getSceneY());
                view.getTransforms().add(new Rotate(rotate, bounds.getX(), bounds.getY()));
            } else {
                view.setTranslateY(view.getTranslateY() + delta / scale);
            }
        });

        getMainStage().addEventFilter(RESET, event -> {
            content.setScaleX(DEFAULT_SCALE);
            content.setScaleY(DEFAULT_SCALE);

            view.setTranslateX(0.0 - content.getTranslateX() / content.getScaleX());
            view.setTranslateY(0.0 - content.getTranslateY() / content.getScaleY());
            view.getTransforms().clear();
        });
    }

    public void setOverflow(boolean overflow) {
        this.overflow.set(overflow);
    }

    public void setLayer(int layer) {
        getAreas(1).forEach(area -> area.setLayer(layer));
    }

    public void add(ModzItem mod) {
        for (var map : mod.maps()) {
            if (!map.isEmpty()) {
                var area = new ModzArea(mod, map);
                area.setLayer(base.getLayer());
                areas.add(area);

                var including = prop(() -> {
                    if (mod.isIncluded() && map.isIncluded()) {
                        map.tiles().forEach(over::add);
                    } else {
                        map.tiles().forEach(over::remove);
                    }
                });

                area.setUserData(including);

                if (mod.isIncluded() && map.isIncluded()) {
                    map.tiles().forEach(over::add);
                }

                mod.includedProperty().addListener(including);
                map.includedProperty().addListener(including);
            }
        }
    }

    public void remove(ModzItem mod) {
        for (var map : mod.maps()) {
            if (!map.isEmpty()) {
                getAreas(2).removeIf(area -> {
                    if (area.getMap().equals(map)) {
                        @SuppressWarnings("unchecked")
                        var including = (ChangeListener<Object>) area.getUserData();
                        area.setUserData(null);

                        mod.includedProperty().removeListener(including);
                        map.includedProperty().removeListener(including);

                        if (mod.isIncluded() && map.isIncluded()) {
                            map.tiles().forEach(over::remove);
                        }

                        area.close();
                        return true;
                    }
                    return false;
                });
            }
        }
    }

    @SuppressWarnings("all")
    private List<ModzArea> getAreas(int from) {
        return (List) areas.subList(from, areas.size());
    }
}
