package ru.wkov.modz.layout;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import org.apache.commons.lang3.ArrayUtils;
import org.controlsfx.control.textfield.TextFields;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.ModzUtil;
import ru.wkov.modz.control.*;
import ru.wkov.modz.data.ModzItem;
import ru.wkov.modz.event.ModzScroll;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static impl.org.controlsfx.autocompletion.SuggestionProvider.create;
import static java.lang.Math.max;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Comparator.comparing;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.WARNING;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.scene.Cursor.HAND;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.MouseEvent.*;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.stage.StageStyle.UNDECORATED;
import static javafx.stage.WindowEvent.WINDOW_HIDING;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignC.*;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignD.DOWNLOAD;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignF.FILTER_CHECK_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignF.FOLDER_OPEN_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignN.*;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignS.*;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignT.TRASH_CAN_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignW.WEB;
import static ru.wkov.modz.ModzUtil.*;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzList extends StackPane implements Consumer<Collection<ModzItem>>, ModzBean {

    private final MultipleSelectionModel<ModzItem> model;

    private final ObservableList<ModzItem> uploaded;

    private final ObservableList<ModzItem> selected;

    private final FilteredList<ModzItem> filtered;

    private final ModzView view;

    private final Set<ModzItem> uniques;

    private final ScheduledExecutorService saver;

    private final AtomicBoolean changed;

    public ModzList(ModzView view) {
        this.view = view;

        addStyleClasses("modz-list");

        changed = new AtomicBoolean(false);
        saver = Executors.newSingleThreadScheduledExecutor();

        var task = saver.scheduleWithFixedDelay(this::backup, 10, 10, SECONDS);
        getMainStage().addEventFilter(WINDOW_HIDING, event -> {
            try {
                task.cancel(false);
                saver.submit(this::backup);
            } finally {
                saver.shutdown();
            }
        });

        var btns = new VBox();
        btns.getStyleClass().add("modz-list-btns");

        var list = new ListView<ModzItem>();
        list.getStyleClass().add("modz-list-view");

        widthProperty().addListener(ModzUtil.prop(false, width ->
                list.setOpacity(width.doubleValue() > btns.getWidth() + 35.0 ? 1.0 : 0.0)));

        model = list.getSelectionModel();
        model.setSelectionMode(MULTIPLE);

        uploaded = FXCollections.observableArrayList(item ->
                ArrayUtils.toArray(item.disabledProperty(), item.includedProperty(), item.tags()));

        uploaded.addListener(ModzUtil.list(items -> {
            for (int i = 0; i < items.size(); i++) {
                items.get(i).setPriority(i);
            }
            changed.set(true);
        }));

        selected = model.getSelectedItems();
        selected.addListener(ModzUtil.list((selected, item) -> item.setSelected(selected)));

        filtered = new FilteredList<>(uploaded);

        list.itemsProperty()
                .bind(new SimpleObjectProperty<>(filtered));

        uniques = new HashSet<>(1500);

        var pane = new Pane(btns);
        pane.getStyleClass().add("modz-list-pane");

        var hbox = new HBox(pane, list);
        HBox.setHgrow(list, ALWAYS);

        getChildren().add(hbox);
        setCursor(HAND);

        init(list);

        var loader = new ModzLoad(this);

        var remover = new Button("", new ModzIcon(TRASH_CAN_OUTLINE));
        remover.setOnAction(event -> removeSelected());

        var checks = new HashSet<String>();
        var filtering = (Runnable) () -> filtered.setPredicate(item -> {
            var used = checks.isEmpty() || item.tags().containsAll(checks);
            if (item.isSelected() && !used) {
                model.clearSelection(filtered.indexOf(item));
            }
            return used;
        });

        var filter = new MenuButton("", new FontIcon(FILTER_CHECK_OUTLINE));
        filter.getItems().addAll(ModzType.createAll(18).stream().map(type -> {
            type.setState(false);

            var item = new CustomMenuItem(type, false);
            item.setOnAction(event -> {
                if (checks.add(type.getId())) {
                    type.setState(true);
                } else {
                    type.setState(false);
                    checks.remove(type.getId());
                }
                filtering.run();
            });
            return item;
        }).toList());

        var predicate = new BiPredicate<ModzItem, ModzItem>() {

            @Override
            public boolean test(ModzItem item1, ModzItem item2) {
                return selected.size() < 2 || item1.isSelected() && item2.isSelected();
            }
        };

        var sorter = new MenuButton("", new FontIcon(SORT),
                new ModzIconMenuItem(state -> uploaded.sort(sorting(predicate, comparing(ModzItem::name, CASE_INSENSITIVE_ORDER))), /*      */ SORT_ALPHABETICAL_ASCENDING),
                new ModzIconMenuItem(state -> uploaded.sort(sorting(predicate, comparing(ModzItem::name, CASE_INSENSITIVE_ORDER).reversed())), SORT_ALPHABETICAL_DESCENDING),
                new ModzIconMenuItem(state -> uploaded.sort(sorting(predicate, comparing(ModzItem::isIncluded))), /*                        */ SORT_BOOL_ASCENDING_VARIANT),
                new ModzIconMenuItem(state -> uploaded.sort(sorting(predicate, comparing(ModzItem::isIncluded).reversed())), /*             */ SORT_BOOL_DESCENDING_VARIANT),
                new ModzIconMenuItem(state -> uploaded.sort(sorting(predicate, comparing(ModzItem::isDisabled))), /*                        */ SORT_BOOL_ASCENDING),
                new ModzIconMenuItem(state -> uploaded.sort(sorting(predicate, comparing(ModzItem::isDisabled).reversed())), /*             */ SORT_BOOL_DESCENDING),
                new ModzIconMenuItem(state -> uploaded.sort(sorting(predicate, comparing(ModzItem::getScore))), /*                          */ SORT_NUMERIC_ASCENDING),
                new ModzIconMenuItem(state -> uploaded.sort(sorting(predicate, comparing(ModzItem::getScore).reversed())), /*               */ SORT_NUMERIC_DESCENDING),

                new ModzIconMenuItem(state -> uploaded.sort(sorting(predicate, comparing(ModzItem::getAuthor))), /*                         */ SORT_ASCENDING),
                new ModzIconMenuItem(state -> uploaded.sort(sorting(predicate, comparing(ModzItem::getAuthor).reversed())), /*              */ SORT_DESCENDING),
                new ModzIconMenuItem(state -> uploaded.sort(sorting(predicate, comparing(ModzItem::getUpdated))), /*                        */ SORT_CLOCK_ASCENDING_OUTLINE),
                new ModzIconMenuItem(state -> uploaded.sort(sorting(predicate, comparing(ModzItem::getUpdated).reversed())), /*             */ SORT_CLOCK_DESCENDING_OUTLINE));

        var folder = new Button("", new ModzIcon(FOLDER_OPEN_OUTLINE));
        folder.setOnAction(event -> explore(model.getSelectedItem().path()));

        var steam = new Button("", new ModzIcon(STEAM));
        steam.setOnAction(event -> explore(STEAM_OPENURL + STEAM_WORKSHOP_URI + model.getSelectedItem().workshop()));

        var web = new Button("", new ModzIcon(WEB));
        web.setOnAction(event -> explore(STEAM_WORKSHOP_URI + model.getSelectedItem().workshop()));

        var replacing = replacing();
        var scrolling = scrolling(list);

        var tmover = new Button("", new FontIcon(CHEVRON_DOUBLE_UP));
        tmover.setOnAction(event -> {
            var items = clearSelection();
            uploaded.removeAll(items);
            uploaded.addAll(filtered.isEmpty() ? 0 : filtered.getSourceIndex(0), items);
            model.selectRange(0, items.size());
            list.scrollTo(0);
        });

        var umover = new ModzHold(new FontIcon(CHEVRON_UP));
        umover.setOnHold(() -> {
            var i1 = model.getSelectedIndex();
            if (i1 > 0) {
                var i2 = i1 - 1;

                model.clearSelection();
                replacing.accept(i1, i2);
                scrolling.accept(i1, -1);
                model.select(i2);
            }
        });

        var dmover = new ModzHold(new FontIcon(CHEVRON_DOWN));
        dmover.setOnHold(() -> {
            var i1 = model.getSelectedIndex();
            if (i1 < filtered.size() - 1) {
                var i2 = i1 + 1;

                model.clearSelection();
                replacing.accept(i1, i2);
                scrolling.accept(i1, 1);
                model.select(i2);
            }
        });

        var bmover = new Button("", new FontIcon(CHEVRON_DOUBLE_DOWN));
        bmover.setOnAction(event -> {
            var items = clearSelection();
            uploaded.removeAll(items);
            uploaded.addAll(items);
            model.selectRange(filtered.size() - items.size(), filtered.size());
            list.scrollTo(uploaded.size());
        });

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

        var relay = (Consumer<Integer>) state -> view.setLayer(icon.setState(state) - 1);
        relay.accept(1);

        var layer = new MenuButton("", icon,
                new ModzLvlMenuItem(0, relay, CLOSE_BOX_OUTLINE),
                new ModzLvlMenuItem(1, relay, NUMERIC_0_BOX_OUTLINE),
                new ModzLvlMenuItem(2, relay, NUMERIC_1_BOX_MULTIPLE_OUTLINE),
                new ModzLvlMenuItem(3, relay, NUMERIC_2_BOX_MULTIPLE_OUTLINE),
                new ModzLvlMenuItem(4, relay, NUMERIC_3_BOX_MULTIPLE_OUTLINE),
                new ModzLvlMenuItem(5, relay, NUMERIC_4_BOX_MULTIPLE_OUTLINE),
                new ModzLvlMenuItem(6, relay, NUMERIC_5_BOX_MULTIPLE_OUTLINE),
                new ModzLvlMenuItem(7, relay, NUMERIC_6_BOX_MULTIPLE_OUTLINE),
                new ModzLvlMenuItem(8, relay, NUMERIC_7_BOX_MULTIPLE_OUTLINE)
        );

        var solver = new Button() {{
            var icon = new ModzIcon(CHECKBOX_BLANK_OUTLINE, CHECKBOX_BLANK_OFF_OUTLINE);
            setOnAction(event -> view.setOverflow(icon.nextState() == 0));
            setGraphic(icon);
        }};

        var picker = new ModzPick();

        var saver = new Button("", new ModzIcon(DOWNLOAD));
        saver.setOnAction(event -> save());

        btns.getChildren()
                .addAll(loader, remover,
                        new Separator(HORIZONTAL),
                        filter, sorter,
                        new Separator(HORIZONTAL),
                        folder, steam, web,
                        new Separator(HORIZONTAL),
                        tmover, umover, dmover, bmover,
                        new Separator(HORIZONTAL),
                        layer, solver, picker,
                        new Separator(HORIZONTAL),
                        saver);

        btns.getChildren().forEach(btn -> {
            btn.setFocusTraversable(false);
            if (btn != loader && btn != picker && btn != layer && btn != solver) {
                btn.setDisable(true);
            }
        });

        uploaded.addListener(list(items -> {
            var disabled = items.isEmpty();

            filter.setDisable(disabled);
            sorter.setDisable(disabled);
            saver.setDisable(disabled);
        }));

        selected.addListener(list(items -> {
            var disabled = items.isEmpty();

            remover.setDisable(disabled);
            tmover.setDisable(disabled);
            bmover.setDisable(disabled);

            disabled = items.size() != 1;

            umover.setDisable(disabled);
            dmover.setDisable(disabled);
            folder.setDisable(disabled);
            steam.setDisable(disabled);
            web.setDisable(disabled);
        }));
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
        var selected = clearSelection();
        if (uploaded.removeAll(selected)) {
            selected.forEach(s -> {
                view.remove(s);
                uploaded.forEach(u -> {
                    u.remove(s);
                    s.remove(u);
                });
                if (!uniques.remove(s)) {
                    throw new IllegalStateException();
                }
            });
        } else {
            throw new IllegalStateException();
        }
        return selected;
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
                view.add(added);
            } else {
                getLogger().log(WARNING, "Item was skipped because it already exists: " + added.path());
            }
        });
    }

    private void init(ListView<ModzItem> view) {
        var robot = new Robot();
        var drag = new ModzDrag();

        view.setCellFactory(unused -> {
            var cell = new ModzCell();

            cell.layoutYProperty().addListener(ModzUtil.prop(() -> {
                if (cell.getItem() != null && drag.isShowing()) {
                    if (cell.contains(cell.screenToLocal(robot.getMousePosition()))) {
                        model.clearAndSelect(cell.getIndex());
                    }
                }
            }));

            return cell;
        });

        view.addEventFilter(DRAG_DETECTED, event -> {
            if (event.isPrimaryButtonDown() && event.isAltDown()) {
                var items = clearSelection();
                uploaded.removeAll(items);

                drag.drag(items);
            }
        });

        view.addEventFilter(MOUSE_DRAGGED, event -> {
            if (drag.isShowing()) {
                model.clearSelection();
                var cell = find(event.getPickResult(), ModzCell.class);
                if (cell != null) {
                    model.select(cell.getItem());
                }

                drag.setX(robot.getMouseX() + 1.0);
                drag.setY(robot.getMouseY());
            }
        });

        var hints = create(List.<ModzItem>of());
        hints.setShowAllIfEmpty(true);

        filtered.addListener(ModzUtil.list(items -> {
            hints.clearSuggestions();
            // noinspection unchecked
            hints.addPossibleSuggestions((List<ModzItem>) items);
        }));

        var search = new TextField();
        search.setPrefWidth(600.0);

        var scene = new Scene(new StackPane(search));
        scene.getStylesheets().add("style.css");

        var find = new Stage();
        find.setAlwaysOnTop(true);
        find.initOwner(getMainStage());
        find.initStyle(UNDECORATED);
        find.setScene(scene);

        var binding = TextFields.bindAutoCompletion(search, hints);
        binding.getAutoCompletionPopup().setPrefWidth(search.getPrefWidth());
        binding.setOnAutoCompleted(event -> {
            var item = event.getCompletion();

            model.clearSelection();
            model.select(item);

            view.scrollTo(item);
            find.hide();
        });

        getMainStage().addEventFilter(MOUSE_PRESSED, event -> find.hide());

        getMainStage().addEventFilter(MOUSE_RELEASED, event -> {
            if (drag.isShowing()) {
                var index = filtered.isEmpty() ? 0 : filtered.getSourceIndex(max(0, model.getSelectedIndex()));
                var dropped = drag.drop();
                if (uploaded.addAll(index, dropped)) {
                    model.clearSelection();
                    model.selectRange(index, index + dropped.size());
                    if (index == 0) {
                        view.scrollTo(index);
                    }
                }
            }
        });

        getMainStage().addEventFilter(KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case DELETE -> removeSelected();
                case ESCAPE -> clearSelection();
                case C -> {
                    if (event.isControlDown()) {
                        clipboard(toINI(selected));
                    }
                }
                case F -> {
                    find.show();
                    binding.setUserInput("");
                }
            }
        });

        getMainStage().addEventFilter(ModzScroll.SCROLL, event -> {
            var index = event.getPriority();
            index = filtered.getViewIndex(index);
            if (index > -1) {
                view.scrollTo(index);
                if (event.isSelected()) {
                    if (!event.isMultiple()) {
                        model.clearSelection();
                    }
                    model.select(index);
                }
            }
        });
    }

    public void backup() {
        if (changed.getAndSet(false)) {
            var path = getRootPath().resolve("list.bin");
            try {
                if (Files.exists(path)) {
                    Files.copy(path, getRootPath().resolve("list.bak"), REPLACE_EXISTING);
                }
                try (var stream = new ObjectOutputStream(new FileOutputStream(path.toFile()))) {
                    stream.writeObject(uploaded.stream().map(ModzItem::export).toArray());
                    stream.flush();
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    public void load() {
        new Thread(() -> {
            try {
                load(getRootPath().resolve("list.bin"));
            } catch (Exception ex) {
                try {
                    load(getRootPath().resolve("list.bak"));
                } catch (Exception suppressed) {
                    if (ex instanceof FileNotFoundException &&
                            suppressed instanceof FileNotFoundException) {
                        return;
                    }
                    ex.addSuppressed(suppressed);
                    getLogger().log(WARNING, "Application could not restore states:", ex);
                }
            }
        }).start();
    }

    public void load(Path path) throws FileNotFoundException {
        if (!Files.exists(path)) {
            throw new FileNotFoundException(path.toString());
        }

        try (var stream = new ObjectInputStream(new FileInputStream(path.toFile()))) {
            var items = Arrays.stream((Object[]) stream.readObject()).map(ModzItem::valueOf).toList();
            Platform.runLater(() -> accept(items));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void save() {
        var path = getRootPath().resolve("server.ini");
        try (var stream = new FileOutputStream(path.toFile())) {
            stream.write(ModzUtil.toINI(uploaded, true).getBytes(UTF_8));
            stream.flush();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        path = getRootPath().resolve("pzmap2dzi.txt");
        try (var stream = new FileOutputStream(path.toFile())) {
            stream.write(ModzUtil.toTXT(uploaded).getBytes(UTF_8));
            stream.flush();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        ModzUtil.explore(getRootPath(), false);
    }

    private BiConsumer<Integer, Integer> replacing() {
        return (i1, i2) -> {
            var min = filtered.getSourceIndex(Math.min(i1, i2));
            var max = filtered.getSourceIndex(Math.max(i1, i2));

            uploaded.add(max, uploaded.set(min, uploaded.remove(max)));
        };
    }

    private BiConsumer<Integer, Integer> scrolling(ListView<ModzItem> list) {
        return (index, steps) -> {
            @SuppressWarnings("unchecked")
            var flow = (VirtualFlow<ModzCell>) list.lookup(".virtual-flow");

            var first = flow.getFirstVisibleCell().getIndex();
            var last = flow.getLastVisibleCell().getIndex();

            if (steps < 0) {
                var delta = 2 + first - index;
                if (0 <= delta) {
                    index = first + steps - delta;
                    list.scrollTo(index);
                    list.scrollTo(index);
                }
            } else if (steps > 0) {
                var delta = 3 + index - last;
                if (0 <= delta) {
                    if (filtered.size() - index > 3) {
                        index = first + steps + delta;
                    } else {
                        index = last;
                    }
                    list.scrollTo(index);
                    list.scrollTo(index);
                }
            }
        };
    }

    private static class ModzIconMenuItem extends CustomMenuItem {

        ModzIconMenuItem(Consumer<Integer> consumer, Ikon... codes) {
            this(consumer, new ModzIcon(codes));
        }

        ModzIconMenuItem(Consumer<Integer> consumer, ModzIcon icon) {
            super(icon, false);
            setOnAction(event -> consumer.accept(icon.nextState()));
        }
    }

    private static class ModzLvlMenuItem extends CustomMenuItem {

        ModzLvlMenuItem(int lvl, Consumer<Integer> consumer, Ikon... codes) {
            this(lvl, consumer, new ModzIcon(codes));
        }

        ModzLvlMenuItem(int lvl, Consumer<Integer> consumer, ModzIcon icon) {
            super(icon, false);
            setOnAction(event -> consumer.accept(lvl));
        }
    }
}
