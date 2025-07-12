package ru.wkov.modz.layout;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import org.kordamp.ikonli.javafx.FontIcon;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.control.*;
import ru.wkov.modz.data.ModzItem;
import ru.wkov.modz.data.ModzTile;
import ru.wkov.modz.data.ModzType;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static impl.org.controlsfx.autocompletion.SuggestionProvider.create;
import static java.lang.Math.max;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.exists;
import static java.util.Collections.swap;
import static java.util.Comparator.comparing;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Side.LEFT;
import static javafx.scene.Cursor.HAND;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.input.Clipboard.getSystemClipboard;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.stage.WindowEvent.WINDOW_HIDING;
import static org.apache.commons.lang3.ArrayUtils.toArray;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.controlsfx.control.textfield.TextFields.bindAutoCompletion;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignC.*;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignF.FILTER_CHECK_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignI.IMAGE_OFF_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignI.IMAGE_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignM.MAGNIFY;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignP.*;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignS.*;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignT.TRASH_CAN_OUTLINE;
import static ru.wkov.modz.ModzUtil.*;
import static ru.wkov.modz.data.ModzItem.toINI;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzList extends StackPane implements Consumer<Collection<ModzItem>>, ModzBean {

    private final MultipleSelectionModel<ModzItem> model;

    private final ObservableList<ModzItem> uploaded;

    private final ObservableList<ModzItem> selected;

    private final FilteredList<ModzItem> filtered;

    private final ListView<ModzItem> view;

    private final Set<ModzItem> uniques;

    private final ScheduledExecutorService saver;

    private final AtomicBoolean changed;

    public ModzList() {
        changed = new AtomicBoolean(false);

        addStyleClasses("modz-list");

        var btns = new VBox();
        btns.getStyleClass().add("modz-vbox-btns");

        view = new ListView<>();
        view.getStyleClass().add("modz-list-view");

        widthProperty().addListener(prop(false, width ->
                view.setOpacity(width.doubleValue() > btns.getWidth() + 35.0 ? 1.0 : 0.0)));

        model = view.getSelectionModel();
        model.setSelectionMode(MULTIPLE);

        var hints = create(List.<String>of());
        hints.setShowAllIfEmpty(true);

        var search = new TextField();
        search.setPrefWidth(248.0);

        bindAutoCompletion(search, hints);

        uploaded = observableArrayList(item -> toArray(item.checkedProperty(), item.invalidProperty()));
        uploaded.addListener(list(items -> {
            for (int i = 0; i < items.size(); i++) {
                items.get(i).setPriority(i);
            }
            changed.set(true);

            hints.clearSuggestions();
            hints.addPossibleSuggestions(items
                    .stream().map(ModzItem::getTitle).toList());
        }));

        selected = model.getSelectedItems();
        selected.addListener(list((selected, item) -> item.setSelected(selected)));

        filtered = new FilteredList<>(uploaded);

        view.itemsProperty()
                .bind(new SimpleObjectProperty<>(filtered));

        uniques = new HashSet<>(1000);

        var hbox = new HBox(btns, view);
        HBox.setHgrow(view, ALWAYS);

        getChildren().add(hbox);
        setCursor(HAND);

        initListView(view);

        var loader = new ModzLoad(this);

        var remover = new Button("", new ModzIcon(TRASH_CAN_OUTLINE));
        remover.setOnAction(event -> removeSelected());

        var finder = new MenuButton("", new FontIcon(MAGNIFY), new CustomMenuItem() {{
            setContent(search);
            setHideOnClick(false);
        }});
        finder.getStyleClass().add("modz-search");
        finder.setPopupSide(LEFT);

        var states = observableArrayList(0, 0, 0, 0, 0, 0, 0, (Object) "");
        var filter = new MenuButton("", new FontIcon(FILTER_CHECK_OUTLINE),
                new CustomMenuItem() {{
                    var icon = new ModzIcon(CHECKBOX_INTERMEDIATE_VARIANT, CHECKBOX_INTERMEDIATE, CHECKBOX_BLANK_OUTLINE);
                    setContent(icon);
                    setOnAction(event -> states.set(6, icon.nextState()));
                    setHideOnClick(false);
                }},
                new CustomMenuItem() {{
                    var icon = new ModzIcon(PROGRESS_ALERT, PROGRESS_CHECK, PROGRESS_CLOSE);
                    setContent(icon);
                    setOnAction(event -> states.set(5, icon.nextState()));
                    setHideOnClick(false);
                }},
                new CustomMenuItem() {{
                    var icon = new ModzIcon(STEERING, STEERING_OFF);
                    setContent(icon);
                    setOnAction(event -> states.set(1, icon.nextState()));
                    setHideOnClick(false);
                }},
                new CustomMenuItem() {{
                    var icon = new ModzIcon(CHECKBOX_INTERMEDIATE, CHECKBOX_BLANK_OFF_OUTLINE);
                    setContent(icon);
                    setOnAction(event -> states.set(2, icon.nextState()));
                    setHideOnClick(false);
                }},
                new CustomMenuItem() {{
                    var icon = new ModzIcon(IMAGE_OUTLINE, IMAGE_OFF_OUTLINE);
                    setContent(icon);
                    setOnAction(event -> states.set(4, icon.nextState()));
                    setHideOnClick(false);
                }},
                new CustomMenuItem() {{
                    var icon = new ModzIcon(COG_OUTLINE, COG_OFF_OUTLINE);
                    setContent(icon);
                    setOnAction(event -> states.set(3, icon.nextState()));
                    setHideOnClick(false);
                }}
        );

        search.textProperty()
                .addListener(prop(state -> states.set(7, state)));

        states.addListener(list(values ->
                filtered.setPredicate(item -> {
                    var title = (String) values.get(7);
                    var visible = switch ((int) values.get(6)) {
                        case 0 -> true;
                        case 1 -> item.isChecked();
                        case 2 -> !item.isChecked();
                        default -> false;
                    } && switch ((int) values.get(5)) {
                        case 0 -> true;
                        case 1 -> !item.isInvalid();
                        case 2 -> item.isInvalid();
                        default -> false;
                    } && ((int) values.get(item.getType().ordinal())) == 0 &&
                            (isBlank(title) || item.getTitle().toLowerCase().contains(title.toLowerCase()));
                    if (!visible && item.isSelected()) {
                        model.clearSelection(filtered.indexOf(item));
                    }
                    return visible;
                })
        ));

        var sorter = new MenuButton("", new FontIcon(SORT),
                new CustomMenuItem(new ModzIcon(SORT_ALPHABETICAL_ASCENDING), false) {{
                    setOnAction(event -> uploaded.sort(comparing(ModzItem::getTitle, CASE_INSENSITIVE_ORDER)));
                }},
                new CustomMenuItem(new ModzIcon(SORT_ALPHABETICAL_DESCENDING), false) {{
                    setOnAction(event -> uploaded.sort(comparing(ModzItem::getTitle, CASE_INSENSITIVE_ORDER).reversed()));
                }},
                new CustomMenuItem(new ModzIcon(SORT_BOOL_ASCENDING_VARIANT), false) {{
                    setOnAction(event -> uploaded.sort(comparing(ModzItem::isChecked)));
                }},
                new CustomMenuItem(new ModzIcon(SORT_BOOL_DESCENDING_VARIANT), false) {{
                    setOnAction(event -> uploaded.sort(comparing(ModzItem::isChecked).reversed()));
                }},
                new CustomMenuItem(new ModzIcon(SORT_BOOL_ASCENDING), false) {{
                    setOnAction(event -> uploaded.sort(comparing(ModzItem::isInvalid)));
                }},
                new CustomMenuItem(new ModzIcon(SORT_BOOL_DESCENDING), false) {{
                    setOnAction(event -> uploaded.sort(comparing(ModzItem::isInvalid).reversed()));
                }},
                new CustomMenuItem(new ModzIcon(SORT_ASCENDING), false) {{
                    setOnAction(event -> uploaded.sort(comparing(ModzItem::getType)));
                }},
                new CustomMenuItem(new ModzIcon(SORT_DESCENDING), false) {{
                    setOnAction(event -> uploaded.sort(comparing(ModzItem::getType).reversed()));
                }}
        );

        var picker = new ModzPick(uploaded, selected);

        var scrolling = (BiConsumer<Integer, Integer>) (index, steps) -> {
            @SuppressWarnings("unchecked")
            var flow = (VirtualFlow<ModzCell>) view.lookup(".virtual-flow");

            var first = flow.getFirstVisibleCell().getIndex();
            var last = flow.getLastVisibleCell().getIndex();

            if (steps < 0) {
                if (first + 2 > index) {
                    index = first + steps;
                    view.scrollTo(index);
                    view.scrollTo(index);
                }
            } else if (steps > 0) {
                if (last < index + 3) {
                    if (filtered.size() - index > 3) {
                        index = first + steps;
                    } else {
                        index = last;
                    }
                    view.scrollTo(index);
                    view.scrollTo(index);
                }
            }
        };

        var swapping = (BiConsumer<Integer, Integer>) (prev, next) -> {
            swap(uploaded, filtered.getSourceIndex(prev), filtered.getSourceIndex(next));
            model.clearAndSelect(next);
        };

        var tmover = new Button("", new FontIcon(CHEVRON_DOUBLE_UP));
        tmover.setOnAction(event -> {
            var items = clearSelection();
            uploaded.removeAll(items);
            uploaded.addAll(filtered.getSourceIndex(0), items);
            model.selectRange(0, items.size());
            view.scrollTo(0);
        });

        var umover = new Button("", new FontIcon(CHEVRON_UP));
        umover.setOnAction(event -> {
            var index = model.getSelectedIndex();
            if (index > 0) {
                swapping.accept(index, index - 1);
                scrolling.accept(index, -1);
            }
        });

        var dmover = new Button("", new FontIcon(CHEVRON_DOWN));
        dmover.setOnAction(event -> {
            var index = model.getSelectedIndex();
            if (index < filtered.size() - 1) {
                swapping.accept(index, index + 1);
                scrolling.accept(index, 1);
            }
        });

        var bmover = new Button("", new FontIcon(CHEVRON_DOUBLE_DOWN));
        bmover.setOnAction(event -> {
            var items = clearSelection();
            uploaded.removeAll(items);
            uploaded.addAll(items);
            model.selectRange(filtered.size() - items.size(), filtered.size());
            view.scrollTo(filtered.size() - 1);
        });

        var filer = new Button("", new ModzIcon(CONTENT_SAVE_COG_OUTLINE));
        filer.setOnAction(event -> file());

        btns.getChildren()
                .addAll(loader, remover,
                        new Separator(HORIZONTAL),
                        finder, filter, sorter, picker,
                        new Separator(HORIZONTAL),
                        tmover, umover, dmover, bmover,
                        new Separator(HORIZONTAL),
                        filer);

        btns.getChildren()
                .forEach(btn -> btn.setDisable(true));

        loader.setDisable(false);

        uploaded.addListener(list(items -> {
            var disabled = items.isEmpty();

            finder.setDisable(disabled);
            filter.setDisable(disabled);
            sorter.setDisable(disabled);
            filer.setDisable(disabled);
        }));

        selected.addListener(list(items -> {
            var disabled = items.isEmpty();

            remover.setDisable(disabled);
            tmover.setDisable(disabled);
            bmover.setDisable(disabled);

            disabled = items.size() != 1;

            umover.setDisable(disabled);
            dmover.setDisable(disabled);
        }));

        saver = Executors.newSingleThreadScheduledExecutor();
        var task = saver.scheduleWithFixedDelay(this::save, 10, 10, SECONDS);

        getMainStage().addEventFilter(WINDOW_HIDING, event -> {
            try {
                task.cancel(false);
                saver.submit(this::save);
            } finally {
                saver.shutdown();
            }
        });
    }

    public MultipleSelectionModel<ModzItem> getModel() {
        return model;
    }

    public ObservableList<ModzItem> getUploaded() {
        return uploaded;
    }

    public ObservableList<ModzItem> getSelected() {
        return selected;
    }

    public FilteredList<ModzItem> getFiltered() {
        return filtered;
    }

    public List<ModzItem> clearSelection() {
        var items = new ArrayList<>(selected);
        model.clearSelection();

        return items;
    }

    public List<ModzItem> removeSelected() {
        var items = clearSelection();
        items.forEach(item1 -> {
            if (uniques.remove(item1) && uploaded.remove(item1)) {
                uploaded.forEach(item2 -> {
                    item2.remove(item1);
                    item1.remove(item2);
                });
            } else {
                throw new IllegalStateException();
            }
        });

        return items;
    }

    public void scrollSelected() {
        view.scrollTo(model.getSelectedIndex());
    }

    @Override
    public void accept(Collection<ModzItem> items) {
        items.forEach(added -> {
            if (uniques.add(added)) {
                uploaded.forEach(item -> {
                    item.add(added);
                    added.add(item);
                });
                uploaded.add(added);
            } else {
                getLogger().warn("failed to add item because it already exists: {}", added.getModPath());
            }
        });
    }

    private void initListView(ListView<ModzItem> view) {
        var drag = new ModzDrag();
        var dragged = drag.getDragged();

        view.setOnDragDetected(event -> {
            if (event.isPrimaryButtonDown() && event.isAltDown()) {
                var items = clearSelection();
                uploaded.removeAll(items);
                dragged.addAll(items);
                drag.drag();
            }
        });

        view.setOnMouseDragged(event -> {
            if (!dragged.isEmpty()) {
                model.clearSelection();
                var cell = find(event.getPickResult(), ListCell.class);
                if (cell != null) {
                    model.select((ModzItem) cell.getItem());
                }
                drag.drag();
            }
        });

        view.setOnMouseReleased(event -> {
            if (!dragged.isEmpty()) {
                var index = filtered.getSourceIndex(max(0, model.getSelectedIndex()));
                if (uploaded.addAll(index, dragged)) {
                    model.clearSelection();
                    model.selectRange(index, index + dragged.size());
                    if (index == 0) {
                        view.scrollTo(index);
                    }
                    drag.drop();
                    dragged.clear();
                }
            }
        });

//      var cells = new ArrayList<ListCell<ModzItem>>();
        var robot = new Robot();

        view.setCellFactory(unused -> {
            var cell = new ModzCell();
            cell.layoutYProperty().addListener((prop, prev, next) -> {
                if (cell.getItem() != null && !dragged.isEmpty()) {
                    if (cell.contains(cell.screenToLocal(robot.getMousePosition()))) {
                        model.clearAndSelect(cell.getIndex());
                    }
                }
            });

//          cells.add(cell);
            return cell;
        });

        getMainStage().addEventFilter(KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case DELETE -> removeSelected();
                case ESCAPE -> clearSelection();
                case C -> {
                    if (event.isControlDown()) {
                        var content = new ClipboardContent();
                        content.putString(toINI(selected));
                        var clipboard = getSystemClipboard();
                        clipboard.setContent(content);
                    }
                }
            }
        });
    }

    private void file() {
        var path = Path.of("modz.ini");
        try (var stream = new FileOutputStream(path.toFile())) {
            stream.write(toINI(uploaded, true).getBytes(UTF_8));
            stream.flush();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        explore(path, true);
    }

    private void save() {
        if (changed.getAndSet(false)) {
            try (var stream = new ObjectOutputStream(new FileOutputStream("modz.bin"))) {
                var saves = new ArrayList<ModzSave>(uploaded.size());
                for (var item : uploaded) {
                    saves.add(new ModzSave(
                            item.getModPath().toString(),
                            Objects.toString(item.getMapPath(), null),

                            item.getModId(),
                            item.getModName(),
                            Objects.toString(item.getMapFolder(), null),
                            item.getWorkshopId(),
                            item.getDescription(),

                            item.getType(),
                            item.getTiles(),

                            new ArrayList<>(item.getReqs().keySet()),
                            item.getImages().stream().map(img -> img.getUrl().substring(5)).toList(),

                            web(item.getColor()),
                            item.isChecked()
                    ));
                }

                stream.writeObject(saves);
                stream.flush();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    public void load() {
        var path = Path.of("modz.bin");

        if (!exists(path)) {
            return;
        }

        try (var stream = new ObjectInputStream(new FileInputStream(path.toFile()))) {
            @SuppressWarnings("unchecked")
            var saves = (List<ModzSave>) stream.readObject();
            var items = new ArrayList<ModzItem>(saves.size());

            for (var save : saves) {
                var item = new ModzItem(
                        Path.of(save.modPath()),
                        save.mapPath() == null ? null : Path.of(save.mapPath()),

                        save.modId(),
                        save.modName(),
                        save.mapFolder(),
                        save.workshopId(),
                        save.description(),

                        save.type(),
                        save.tiles(),

                        save.requires(),
                        save.images()
                );

                item.setColor(Color.web(save.color()));
                item.setChecked(save.checked());

                items.add(item);
            }

            accept(items);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record ModzSave(String modPath,
                            String mapPath,

                            String modId,
                            String modName,
                            String mapFolder,
                            String workshopId,
                            String description,

                            ModzType type,
                            Collection<ModzTile> tiles,

                            Collection<String> requires,
                            Collection<String> images,

                            String color,
                            boolean checked) implements Serializable {
    }
}
