package ru.wkov.modz.control;

import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.apache.commons.lang3.ArrayUtils;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.data.MapzItem;
import ru.wkov.modz.data.ModzItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Comparator.comparing;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.collections.FXCollections.sort;
import static javafx.geometry.Pos.TOP_RIGHT;
import static javafx.scene.Cursor.CLOSED_HAND;
import static javafx.scene.Cursor.OPEN_HAND;
import static javafx.scene.control.OverrunStyle.CLIP;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.layout.HBox.setHgrow;
import static javafx.scene.layout.Priority.ALWAYS;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignC.CHECKBOX_BLANK_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignC.CHECKBOX_INTERMEDIATE;
import static ru.wkov.modz.ModzUtil.*;
import static ru.wkov.modz.event.ModzColor.COLOR;
import static ru.wkov.modz.event.ModzRandom.RANDOM;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzCell extends ListCell<ModzItem> implements ModzBean {

    private final ObservableList<ModzType> tags;

    private final StackPane line;

    private final ModzIcon flag;

    private final ModzPage page;

    private final ModzRate rate;

    private final ModzMore more;

    private final ModzMenu menu;

    private final TextArea desc;

    private final MapzView maps;

    private final LinkView reqs;

    private final LinkView used;

    private final Label numb;

    private final Label text;

    public ModzCell() {
        addStyleClasses("modz-cell");
        setGraphic(null);

        numb = new Label();
        text = new Label();

        flag = new ModzIcon(CHECKBOX_INTERMEDIATE, CHECKBOX_BLANK_OUTLINE);
        flag.addEventFilter(MOUSE_PRESSED, event -> {
            if (event.getButton() == PRIMARY) {
                var item = getItem();
                if (item != null) {
                    var included = !item.isIncluded();

                    var model = getListView().getSelectionModel();
                    if (item.isSelected()) {
                        new ArrayList<>(model.getSelectedItems())
                                .forEach(i -> i.setIncluded(included));
                    } else {
                        item.setIncluded(included);
                    }
                }
            }
            event.consume();
        });

        page = new ModzPage(300.0, 300.0);
        rate = new ModzRate();

        menu = new ModzMenu();
        menu.detailedProperty().addListener(prop(false, detailed -> {
            var item = getItem();
            if (item != null) {
                item.setDetailed(detailed);
            }
        }));

        setHgrow(menu, ALWAYS);

        more = new ModzMore();
        more.setTitles(flag, numb, rate, text);
        more.setContents(page, menu);
        more.getContent().visibleProperty()
                .addListener(prop(false, page::setExpanded));
        more.expandedProperty()
                .addListener(prop(false, expanded -> {
                    var item = getItem();
                    if (item != null) {
                        item.setExpanded(expanded);
                    }
                }));

        desc = new TextArea();
        desc.getStyleClass().add("modz-menu-desc");
        desc.setEditable(false);
        desc.setWrapText(true);

        menu.setDesc(desc);

        maps = new MapzView();
        menu.setMaps(maps);

        reqs = new LinkView();
        menu.setReqs(reqs);

        used = new LinkView();
        menu.setUsed(used);

        numb.setMinWidth(51);
        text.setTextOverrun(CLIP);

        more.setMinWidth(450);
        widthProperty().addListener(prop(false, width ->
                more.setMaxWidth(Double.max(width.doubleValue(), more.getMinWidth()))));

        var hbox = new HBox();
        hbox.getStyleClass().add("modz-cell-tags");
        hbox.getChildren().addAll(ModzType.createAll(21));
        hbox.setMouseTransparent(true);
        hbox.setAlignment(TOP_RIGHT);

        // noinspection all
        tags = (ObservableList) hbox.getChildren();

        line = new StackPane(more, hbox);
        line.getStyleClass().add("modz-cell-line");
    }

    public void setChecked(boolean checked) {
        pseudoClassStateChanged(CHECKED, checked);
        flag.setState(checked);
    }

    public void setInvalid(boolean invalid) {
        pseudoClassStateChanged(INVALID, invalid);
    }

    public void setNumber(int number) {
        numb.setText(leftPad((number + 1) + ":", 6));
    }

    public void setImgs(List<? extends String> urls) {
        page.setUrls(urls);
    }

    public void setTags(Set<? extends String> keys) {
        tags.forEach(tag ->
                tag.setVisible(keys.contains(tag.getId())));

        sort(tags, comparing(ModzType::isVisible).thenComparing(ModzType::getOrder));
    }

    @Override
    protected void updateItem(ModzItem item, boolean empty) {
        super.updateItem(item, empty);

        if (item == null) {
            setChecked(false);
            setInvalid(false);
            setGraphic(null);
            return;
        }

        setChecked(item.isIncluded());
        setInvalid(item.isDisabled());
        setNumber(item.getPriority());
        setImgs(item.imgs());
        setTags(item.tags());

        rate.setScore(item.getScore());
        text.setText(item.toString());

        var coords = new StringBuilder();
        if (!item.maps().isEmpty()) {
            for (var map : item.maps()) {
                coords.append("\nMap Folder: ").append(map.id());
                if (!map.isEmpty()) {
                    coords.append("\n[ ");
                    for (var tile : map.tiles()) {
                        coords.append(tile.x()).append('x').append(tile.y()).append(' ');
                    }
                    coords.append("]");
                }
            }
            coords.append("\n");
        }

        var data = item.dataProperty().get();
        if (data == null) {
            rate.pseudoClassStateChanged(UNUSED, true);
            desc.setText(String.format("""
                            Workshop ID: %s
                            Mod ID: %s
                            %s
                            %s
                            """,
                    item.workshop(),
                    item.id(),
                    coords,
                    item.description()
            ));
        } else {
            rate.pseudoClassStateChanged(UNUSED, false);
            desc.setText(String.format("""
                            Workshop ID: %s
                            Mod ID: %s
                            %s
                            Created: %s
                            Updated: %s
                            
                            🔔 %,d 💛 %,d 👍 %,d 👎 %,d
                            
                            %s
                            """,
                    item.workshop(),
                    item.id(),
                    coords,
                    leftPad(data.getCreatedAt().format(DTF), 21),
                    leftPad(data.getUpdatedAt().format(DTF), 21),
                    data.getSubscriptions(),
                    data.getFavorites(),
                    data.getRate().getUp(),
                    data.getRate().getDown(),
                    data.getDescription()
            ));
        }

        maps.accept(item.maps());
        reqs.accept(item.reqs());
        used.accept(item.used());

        menu.setDetailed(item.getDetailed());
        more.setExpanded(item.isExpanded());

        sort(menu.getPanes(), comparing(TitledPane::isVisible).reversed().thenComparing(TitledPane::getId));

        setGraphic(line);
    }

    private static class MapzView extends ListView<MapzItem>
            implements ModzBean, Consumer<ObservableList<MapzItem>> {

        MapzView() {
            addStyleClasses("modz-menu-list");
            setCellFactory(unused -> new ModzMapz());
            setPrefHeight(0);
            setCursor(OPEN_HAND);

            var dragged = new AtomicInteger(-1);

            setOnDragDetected(event -> {
                if (event.isPrimaryButtonDown()) {
                    dragged.set(getSelectionModel().getSelectedIndex());
                }
            });

            setOnMouseDragged(event -> {
                var prev = dragged.get();
                if (prev != -1) {
                    var cell = find(event.getPickResult(), ModzMapz.class);
                    if (cell != null) {
                        var items = getItems();
                        var next = cell.getIndex();
                        if (next != prev && next > -1 && next < items.size()) {
                            items.get(next).setPriority(prev);
                            items.get(prev).setPriority(next);
                            items.sort(Comparator.comparingInt(MapzItem::getPriority));
                            getSelectionModel().clearAndSelect(next);
                            dragged.set(next);
                        }
                    }
                }
            });

            setOnMousePressed(event -> {
                setCursor(CLOSED_HAND);
            });

            setOnMouseReleased(event -> {
                setCursor(OPEN_HAND);
                dragged.set(-1);
            });

            getMainStage().addEventFilter(MOUSE_PRESSED, event -> {
                var view = find(event.getPickResult(), MapzView.class);
                if (view != null && view != this) {
                    getSelectionModel().clearSelection();
                }
            });

            getMainStage().addEventFilter(COLOR, event -> {
                var item = getSelectionModel().getSelectedItem();
                if (item != null && !item.isEmpty()) {
                    item.setColor(event.getColor());
                }
            });

            getMainStage().addEventFilter(RANDOM, event -> {
                getItems().forEach(item -> {
                    if (!item.isEmpty()) {
                        item.setColor(color(0.28, 0.88));
                    }
                });
            });
        }

        @Override
        public void accept(ObservableList<MapzItem> maps) {
            if (maps.isEmpty()) {
                setDisable(true);
            } else {
                setDisable(false);
                setItems(maps);
            }
        }
    }

    private static class LinkView extends ListView<ModzItem>
            implements ModzBean, Consumer<ObservableMap<String, ModzItem>> {

        final ObservableList<ModzItem> items;

        Runnable unbind;

        LinkView() {
            items = observableArrayList(item -> ArrayUtils.toArray(item.priorityProperty()));
            setItems(new SortedList<>(items, Comparator.comparingInt(ModzItem::getPriority)));

            addStyleClasses("modz-menu-list");
            setCellFactory(unused -> new ModzLink());
            setPrefHeight(0);
        }

        @Override
        public void accept(ObservableMap<String, ModzItem> links) {
            if (unbind != null) {
                unbind.run();
            }

            if (links.isEmpty()) {
                setDisable(true);
            } else {
                setDisable(false);

                var listener = map(() -> items.setAll(links.values()));
                listener.onChanged(null);

                links.addListener(listener);
                unbind = () -> links.removeListener(listener);
            }
        }
    }
}
