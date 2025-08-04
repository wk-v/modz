package ru.wkov.modz.data;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import ru.wkov.modz.control.ModzCell;
import ru.wkov.modz.http.ModzData;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;
import static javafx.collections.FXCollections.*;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static ru.wkov.modz.ModzUtil.*;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public record ModzItem(Path path,
                       String id,
                       String name,
                       String workshop,
                       String description,
                       ObservableSet<String> tags,
                       ObservableList<String> imgs,
                       ObservableList<MapzItem> maps,
                       ObservableMap<String, ModzItem> reqs,
                       ObservableMap<String, ModzItem> used,
                       BooleanProperty disabledProperty,
                       BooleanProperty expandedProperty,
                       BooleanProperty includedProperty,
                       BooleanProperty selectedProperty,
                       IntegerProperty priorityProperty,
                       StringProperty detailedProperty,
                       ObjectProperty<ModzCell> cellProperty,
                       ObjectProperty<ModzData> dataProperty,
                       ChangeListener<Boolean> listener) {

    public ModzItem {
        listener = prop(() -> disabledProperty.set(reqs.values().stream()
                .anyMatch(item -> !item.isIncluded() || item.isDisabled())));

        disabledProperty.addListener(prop(invalid -> {
            var cell = cellProperty.get();
            if (cell != null) cell.setInvalid(invalid);
        }));

        includedProperty.addListener(prop(checked -> {
            var cell = cellProperty.get();
            if (cell != null) cell.setChecked(checked);
        }));

        priorityProperty.addListener(prop(number -> {
            var cell = cellProperty.get();
            if (cell != null) cell.setNumber(number.intValue());
        }));

        imgs.addListener(list(urls -> {
            var cell = cellProperty.get();
            if (cell != null) cell.setImgs(urls);
        }));

        tags.addListener(set(keys -> {
            var cell = cellProperty.get();
            if (cell != null) cell.setTags(keys);
        }));
    }

    public static ModzItem valueOf(String id) {
        return new ModzItem(
                null,
                id,
                "[no name]",
                null,
                "[no description]",
                emptyObservableSet(),
                emptyObservableList(),
                emptyObservableList(),
                emptyObservableMap(),
                emptyObservableMap(),
                new SimpleBooleanProperty(true),
                new SimpleBooleanProperty(false),
                new SimpleBooleanProperty(false),
                new SimpleBooleanProperty(false),
                new SimpleIntegerProperty(-1),
                new SimpleStringProperty(),
                new SimpleObjectProperty<>(),
                new SimpleObjectProperty<>(),
                null
        );
    }

    public static ModzItem valueOf(Path path) {
        var temp = path.resolve("mod.info");
        if (!Files.exists(temp)) {
            return null;
        }

        var info = new ModzInfo();
        try {
            info.load(temp);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        var maps = new ArrayList<MapzItem>();
        temp = path.resolve(Path.of("media", "maps"));
        if (Files.exists(temp)) {
            try (var stream = Files.list(temp)) {
                var priority = new AtomicInteger(0);
                stream.filter(Files::isDirectory).map(MapzItem::valueOf).forEach(map -> {
                    maps.add(priority.get(), map);
                    map.setPriority(priority.getAndIncrement());
                });
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        var imgs = new ArrayList<String>();
        for (var poster : info.<String>all("poster")) {
            imgs.add("file:" + path.resolve(poster));
        }

        var reqs = new HashMap<String, ModzItem>();
        for (var id : info.<String>all("require")) {
            reqs.put(id, ModzItem.valueOf(id));
        }

        return new ModzItem(
                path,
                info.one("id"),
                info.one("name"),
                path.getParent().getParent().getFileName().toString(),
                defaultIfBlank(info.one("description"), "[no description]"),
                observableSet(),
                observableList(imgs),
                observableList(maps),
                observableMap(reqs),
                observableHashMap(),
                new SimpleBooleanProperty(!reqs.isEmpty()),
                new SimpleBooleanProperty(false),
                new SimpleBooleanProperty(true),
                new SimpleBooleanProperty(false),
                new SimpleIntegerProperty(0),
                new SimpleStringProperty(),
                new SimpleObjectProperty<>(),
                new SimpleObjectProperty<>(),
                null
        );
    }

    public void add(ModzItem next) {
        reqs.computeIfPresent(next.id, (id, prev) -> {
            if (prev == next) {
                throw new IllegalStateException();
            }

            next.disabledProperty.addListener(listener);
            next.includedProperty.addListener(listener);

            next.used.put(this.id, this);

            return next;
        });

        listener.changed(null, null, null);
    }

    public void remove(ModzItem next) {
        reqs.computeIfPresent(next.id, (modId, prev) -> {
            if (prev != next) {
                throw new IllegalStateException();
            }

            next.disabledProperty.removeListener(listener);
            next.includedProperty.removeListener(listener);

            next.used.remove(this.id);

            return ModzItem.valueOf(modId);
        });

        listener.changed(null, null, null);
    }

    public boolean isDisabled() {
        return disabledProperty.get();
    }

    public void setDisabled(boolean disabled) {
        disabledProperty.set(disabled);
    }

    public boolean isExpanded() {
        return expandedProperty.get();
    }

    public void setExpanded(boolean expanded) {
        expandedProperty.set(expanded);
    }

    public boolean isIncluded() {
        return includedProperty.get();
    }

    public void setIncluded(boolean included) {
        includedProperty.set(included);
    }

    public boolean isSelected() {
        return selectedProperty.get();
    }

    public void setSelected(boolean selected) {
        selectedProperty.set(selected);
    }

    public int getPriority() {
        return priorityProperty.get();
    }

    public void setPriority(int priority) {
        priorityProperty.set(priority);
    }

    public String getDetailed() {
        return detailedProperty.get();
    }

    public void setDetailed(String detailed) {
        detailedProperty.set(detailed);
    }

    public double getScore() {
        var data = dataProperty.get();
        if (data == null) {
            return 0.0;
        }
        return data.getRate().getScore();
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof ModzItem that &&
                Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return name + " <" + id + ">";
    }

    public Object[] export() {
        return new Object[]{
                path.toString(),
                id,
                name,
                workshop,
                description,
                tags.toArray(String[]::new),
                imgs.toArray(String[]::new),
                maps.stream().map(MapzItem::export).toArray(),
                reqs.keySet().toArray(String[]::new),
                isIncluded(),
                dataProperty.get()
        };
    }

    public static ModzItem valueOf(Object export) {
        return valueOf((Object[]) export);
    }

    public static ModzItem valueOf(Object[] export) {
        var maps = Arrays.stream((Object[]) export[7]).map(MapzItem::valueOf).toList();
        var reqs = Arrays.stream((String[]) export[8]).collect(toMap(Function.identity(), ModzItem::valueOf));

        var item = new ModzItem(
                Path.of(export[0].toString()),
                export[1].toString(),
                export[2].toString(),
                export[3].toString(),
                export[4].toString(),
                observableSet((String[]) export[5]),
                observableArrayList((String[]) export[6]),
                observableArrayList(maps),
                observableMap(reqs),
                observableHashMap(),
                new SimpleBooleanProperty(!reqs.isEmpty()),
                new SimpleBooleanProperty(false),
                new SimpleBooleanProperty((Boolean) export[9]),
                new SimpleBooleanProperty(false),
                new SimpleIntegerProperty(0),
                new SimpleStringProperty(),
                new SimpleObjectProperty<>(),
                new SimpleObjectProperty<>(),
                null
        );

        item.dataProperty.set((ModzData) export[10]);

        return item;
    }
}
