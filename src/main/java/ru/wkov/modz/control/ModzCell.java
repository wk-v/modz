package ru.wkov.modz.control;

import javafx.beans.value.ChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.data.ModzItem;

import java.util.ArrayList;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparingInt;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.layout.HBox.setHgrow;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.scene.paint.Color.BLACK;
import static org.apache.commons.lang3.ArrayUtils.toArray;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignC.*;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignF.FOLDER_OPEN_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignS.STEAM;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignW.WEB;
import static ru.wkov.modz.ModzUtil.*;
import static ru.wkov.modz.data.ModzType.MAPZ;
import static ru.wkov.modz.data.ModzType.NULL;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzCell extends ListCell<ModzItem> implements ModzBean {

    private final ChangeListener<Object> listener;

    private final ModzListNested reqs;

    private final ModzListNested used;

    private final ModzMenu menu;

    private final ModzMore more;

    private final ModzPage page;

    private final TextArea desc;

    private final ModzIcon icon;

    private final Label numb;

    private final Label name;

    private ModzItem item;

    public ModzCell() {
        addStyleClasses("modz-cell");

        setGraphic(null);

        listener = prop(this::updateStyle);

        icon = new ModzIcon();
        icon.setEffect(ICON_EFFECT);
        icon.addEventFilter(MOUSE_PRESSED, event -> {
            if (item != null && event.getButton() == PRIMARY) {
                var model = getListView().getSelectionModel();
                var checked = !item.isChecked();
                if (item.isSelected()) {
                    new ArrayList<>(model.getSelectedItems())
                            .forEach(i -> i.setChecked(checked));
                } else {
                    item.setChecked(checked);
                }
            }
            event.consume();
        });

        numb = new Label();
        name = new Label();

        var btns = new HBox(
                new Button() {{
                    setGraphic(new FontIcon(FOLDER_OPEN_OUTLINE));
                    setOnAction(event -> explore(getItem().getModPath()));
                }},
                new Button() {{
                    setGraphic(new FontIcon(STEAM));
                    setOnAction(event -> explore(STEAM_OPENURL + STEAM_WORKSHOP_URI + getItem().getWorkshopId()));
                }},
                new Button() {{
                    setGraphic(new FontIcon(WEB));
                    setOnAction(event -> explore(STEAM_WORKSHOP_URI + getItem().getWorkshopId()));
                }}
        );
        btns.getStyleClass().add("modz-exec-btns");

        desc = new TextArea();
        desc.getStyleClass().add("modz-cell-desc");
        desc.setEditable(false);
        desc.setWrapText(true);

        var vbox = new VBox(desc, btns);
        VBox.setVgrow(desc, ALWAYS);

        reqs = new ModzListNested();
        used = new ModzListNested();

        menu = new ModzMenu();
        menu.setMaxHeight(250.0);
        menu.setDesc(vbox);
        menu.setReqs(reqs);
        menu.setUsed(used);

        page = new ModzPage(250.0, 250.0);

        more = new ModzMore();
        more.setTitles(icon, numb, name);
        more.setContents();

        var content = (HBox) more.getContent();
        var nodes = content.getChildren();
        var array = toArray(page, menu);

        content.visibleProperty().addListener(prop(false, visible -> {
            if (visible) {
                nodes.setAll(array);
            } else {
                nodes.clear();
            }
        }));

        setHgrow(menu, ALWAYS);
        setHgrow(vbox, ALWAYS);
        setHgrow(reqs, ALWAYS);
        setHgrow(used, ALWAYS);
    }

    @Override
    protected void updateItem(ModzItem next, boolean empty) {
        super.updateItem(next, empty);

        if (item != null) {
            item.colorProperty().removeListener(listener);
            item.checkedProperty().removeListener(listener);
            item.invalidProperty().removeListener(listener);
            item.priorityProperty().removeListener(listener);
            item.selectedProperty().removeListener(listener);

            item.detailedProperty().unbind();
            item.expandedProperty().unbind();
        }

        if (empty) {
            pseudoClassStateChanged(CHECKED, false);
            pseudoClassStateChanged(INVALID, false);

            setGraphic(null);
            item = null;

            return;
        } else {
            setGraphic(more);
            item = next;
        }

        name.setText(item.getTitle());
        desc.setText(item.toString());
        page.setImages(item.getImages());
        menu.setDetailed(item.getDetailed());
        more.setExpanded(item.isExpanded());

        reqs.setLinks(item.getReqs());
        used.setLinks(item.getUsed());

        item.colorProperty().addListener(listener);
        item.checkedProperty().addListener(listener);
        item.invalidProperty().addListener(listener);
        item.priorityProperty().addListener(listener);
        item.selectedProperty().addListener(listener);

        item.detailedProperty().bind(menu.detailedProperty());
        item.expandedProperty().bind(more.expandedProperty());

        updateStyle();
    }

    private void updateStyle() {
        if (item == null) {
            return;
        }

        var checked = item.isChecked();
        var invalid = item.isInvalid();
        var selected = item.isSelected();

        pseudoClassStateChanged(CHECKED, checked);
        pseudoClassStateChanged(INVALID, invalid);

        var type = item.getType();
        switch (type) {
            case CARZ -> icon.setCodes(CAR, CAR_OFF);
            case MAPZ -> icon.setCodes(CHECKBOX_INTERMEDIATE, CHECKBOX_BLANK_OFF_OUTLINE);
            case MODZ -> icon.setCodes(COG_OUTLINE, COG_OFF_OUTLINE);
        }

        var color = selected ? getMainColor() : BLACK;
        if (checked) {
            if (invalid) {
                color = selected ? getFailColor() : BLACK;
            } else if (type == MAPZ) {
                color = item.getColor();
            }
        }

        icon.setIconColor(color);
        icon.setState(checked);

        numb.setText(item.getNum() + ':');
    }

    private static class ModzListNested extends ListView<ModzItem> implements ModzBean {

        final MapChangeListener<String, ModzItem> listener;

        final ObservableList<ModzItem> items;

        ObservableMap<String, ModzItem> links;

        ModzListNested() {
            items = observableArrayList(item -> toArray(item.priorityProperty()));
            setItems(new SortedList<>(items, comparingInt(ModzItem::getPriority)));
            setCellFactory(ModzCellNested::new);
            setOnKeyPressed(event -> {
                if (event.getCode() == ESCAPE) {
                    getSelectionModel().clearSelection();
                }
            });

            listener = change -> items.setAll(links.values());
        }

        void setLinks(ObservableMap<String, ModzItem> links) {
            if (this.links != null) {
                this.links.removeListener(listener);
            }
            this.links = links;

            links.addListener(listener);
            items.setAll(links.values());
        }

        static class ModzCellNested extends ListCell<ModzItem> implements ModzBean {

            final ChangeListener<Object> listener;

            final ModzIcon icon;

            final Label numb;

            final Label name;

            final HBox head;

            ModzItem item;

            ModzCellNested(ListView<ModzItem> view) {
                addStyleClasses("modz-cell-nested");

                listener = prop(this::updateStyle);
                selectedProperty()
                        .addListener(listener);

                var menu = new ContextMenu(
                        new MenuItem("open in browser", new FontIcon(WEB)) {{
                            setOnAction(event -> {
                                if (item.getType() == NULL) {
                                    explore(STEAM_SEARCH_URI + encode(item.getModId(), UTF_8));
                                } else {
                                    explore(STEAM_WORKSHOP_URI + getItem().getWorkshopId());
                                }
                            });
                        }},
                        new MenuItem("open in steam", new FontIcon(STEAM)) {{
                            setOnAction(event -> {
                                if (item.getType() == NULL) {
                                    explore(STEAM_OPENURL + STEAM_SEARCH_URI + encode(item.getModId(), UTF_8));
                                } else {
                                    explore(STEAM_OPENURL + STEAM_WORKSHOP_URI + getItem().getWorkshopId());
                                }
                            });
                        }},
                        new MenuItem("open folder", new FontIcon(FOLDER_OPEN_OUTLINE)) {{
                            setOnAction(event -> {
                                if (item.getType() != NULL) {
                                    explore(getItem().getModPath());
                                }
                            });
                        }}
                );
                menu.setOnShowing(event -> menu.getItems().get(2).setDisable(item.getType() == NULL));
                menu.getStyleClass().add("modz-cell-nested-context-menu");

                setContextMenu(menu);

                icon = new ModzIcon();
                icon.setEffect(ICON_EFFECT);
                icon.addEventFilter(MOUSE_PRESSED, event -> {
                    if (item != null && item.getType() != NULL && event.getButton() == PRIMARY) {
                        item.setChecked(!item.isChecked());
                    }
                    event.consume();
                });

                numb = new Label();
                name = new Label();

                head = new HBox(icon, numb, name);
                head.setAlignment(CENTER_LEFT);
                head.getStyleClass().add("modz-cell-head");
            }

            @Override
            protected void updateItem(ModzItem next, boolean empty) {
                super.updateItem(next, empty);

                if (item != null) {
                    item.colorProperty().removeListener(listener);
                    item.checkedProperty().removeListener(listener);
                    item.invalidProperty().removeListener(listener);
                    item.priorityProperty().removeListener(listener);
                }

                if (empty) {
                    pseudoClassStateChanged(CHECKED, false);
                    pseudoClassStateChanged(INVALID, false);

                    setGraphic(null);
                    item = null;

                    return;
                } else {
                    setGraphic(head);
                    item = next;
                }

                name.setText(item.getTitle());

                item.colorProperty().addListener(listener);
                item.checkedProperty().addListener(listener);
                item.invalidProperty().addListener(listener);
                item.priorityProperty().addListener(listener);

                updateStyle();
            }

            void updateStyle() {
                if (item == null) {
                    return;
                }

                var checked = item.isChecked();
                var invalid = item.isInvalid();
                var selected = isSelected();
                var priority = item.getPriority();

                pseudoClassStateChanged(CHECKED, checked);
                pseudoClassStateChanged(INVALID, invalid);

                var type = item.getType();
                switch (type) {
                    case CARZ -> icon.setCodes(CAR, CAR_OFF);
                    case MAPZ -> icon.setCodes(CHECKBOX_INTERMEDIATE, CHECKBOX_BLANK_OFF_OUTLINE);
                    case MODZ -> icon.setCodes(COG_OUTLINE, COG_OFF_OUTLINE);
                    case NULL -> icon.setCodes(CLOSE_OUTLINE, CLOSE_OUTLINE);
                }

                var color = selected ? getMainColor() : BLACK;
                if (checked) {
                    if (invalid) {
                        color = selected ? getFailColor() : BLACK;
                    } else if (type == MAPZ) {
                        color = item.getColor();
                    }
                }

                icon.setIconColor(color);
                icon.setState(checked);

                numb.setText(priority < 0 ? "" : item.getNum() + ':');
            }
        }
    }
}
