package ru.wkov.modz.data;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableMap;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.lang.String.join;
import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;
import static javafx.collections.FXCollections.observableHashMap;
import static javafx.scene.paint.Color.BLACK;
import static javafx.scene.paint.Color.GRAY;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static ru.wkov.modz.ModzUtil.color;
import static ru.wkov.modz.ModzUtil.prop;
import static ru.wkov.modz.data.ModzType.MAPZ;
import static ru.wkov.modz.data.ModzType.NULL;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzItem {

    private final Path modPath;

    private final Path mapPath;

    private final String title;

    private final String modId;

    private final String modName;

    private final String mapFolder;

    private final String workshopId;

    private final String description;

    private final ModzType type;

    private final List<Image> images;

    private final Set<ModzTile> tiles;

    private final ObservableMap<String, ModzItem> reqs;

    private final ObservableMap<String, ModzItem> used;

    private final ObjectProperty<Color> color;

    private final BooleanProperty checked;

    private final BooleanProperty invalid;

    private final BooleanProperty expanded;

    private final BooleanProperty selected;

    private final IntegerProperty priority;

    private final IntegerProperty detailed;

    private final ChangeListener<Boolean> listener;

    public ModzItem(String modId) {
        modPath = null;
        mapPath = null;

        title = "<unknown> " + modId;
        this.modId = modId;
        modName = null;
        mapFolder = null;
        workshopId = null;
        description = null;

        type = NULL;
        tiles = null;
        reqs = null;
        used = null;
        images = null;

        color = new SimpleObjectProperty<>(GRAY);
        checked = new SimpleBooleanProperty(true);
        invalid = new SimpleBooleanProperty(true);
        expanded = null;
        selected = null;
        priority = new SimpleIntegerProperty(-1);
        detailed = null;

        listener = null;
    }

    public ModzItem(Path modPath,
                    Path mapPath,

                    String modId,
                    String modName,
                    String mapFolder,
                    String workshopId,
                    String description,

                    ModzType type,
                    Collection<ModzTile> tiles,

                    Collection<String> requires,
                    Collection<String> images) {

        this.modPath = requireNonNull(modPath);
        this.mapPath = mapPath;

        this.modId = requireNonNull(modId);
        this.modName = requireNonNull(modName);
        this.mapFolder = mapFolder;
        this.workshopId = requireNonNull(workshopId);
        this.description = requireNonNull(description);

        this.type = requireNonNull(type);

        this.tiles = (tiles == null || tiles.isEmpty())
                ? Set.of()
                : new HashSet<>(tiles);

        this.images = (images == null || images.isEmpty())
                ? List.of()
                : images
                .stream()
                .map(modPath::resolve)
                .filter(Files::exists)
                .map(p -> new Image("file:" + p, true))
                .toList();

        reqs = observableHashMap();
        if (requires != null) {
            requires.forEach(key ->
                    reqs.put(key, new ModzItem(key)));
        }

        used = observableHashMap();

        title = modName + (type == MAPZ ? " | " + mapFolder : "");
        color = new SimpleObjectProperty<>(type == MAPZ ? color() : BLACK);

        checked = new SimpleBooleanProperty(true);
        invalid = new SimpleBooleanProperty(false);
        expanded = new SimpleBooleanProperty(false);
        selected = new SimpleBooleanProperty(false);
        priority = new SimpleIntegerProperty(-1);
        detailed = new SimpleIntegerProperty(0);

        listener = prop(() -> invalid.set(this.reqs.values().stream()
                .anyMatch(item -> item == ModzItem.this || !item.isChecked() || item.isInvalid())));
    }

    public void add(ModzItem next) {
        if (next.getType() == MAPZ) {
            return;
        }

        reqs.computeIfPresent(next.getModId(), (modId, prev) -> {
            if (prev.getType() != NULL) {
                throw new IllegalStateException();
            }

            next.checkedProperty().addListener(listener);
            next.invalidProperty().addListener(listener);

            next.used.put(this.modId, this);

            return next;
        });

        listener.changed(null, null, null);
    }

    public void remove(ModzItem next) {
        if (next.getType() == MAPZ) {
            return;
        }

        reqs.computeIfPresent(next.getModId(), (modId, prev) -> {
            if (prev != next) {
                throw new IllegalStateException();
            }

            next.checkedProperty().removeListener(listener);
            next.invalidProperty().removeListener(listener);

            next.used.remove(this.modId);

            return new ModzItem(modId);
        });

        listener.changed(null, null, null);
    }

    public Path getModPath() {
        return modPath;
    }

    public Path getMapPath() {
        return mapPath;
    }

    public String getNum() {
        return leftPad(String.valueOf(priority.get() + 1), 4);
    }

    public String getTitle() {
        return title;
    }

    public String getModId() {
        return modId;
    }

    public String getModName() {
        return modName;
    }

    public String getMapFolder() {
        return mapFolder;
    }

    public String getWorkshopId() {
        return workshopId;
    }

    public String getDescription() {
        return description;
    }

    public ModzType getType() {
        return type;
    }

    public List<Image> getImages() {
        return images;
    }

    public Set<ModzTile> getTiles() {
        return tiles;
    }

    public ObservableMap<String, ModzItem> getReqs() {
        return reqs;
    }

    public ObservableMap<String, ModzItem> getUsed() {
        return used;
    }

    public ObjectProperty<Color> colorProperty() {
        return color;
    }

    public BooleanProperty checkedProperty() {
        return checked;
    }

    public BooleanProperty invalidProperty() {
        return invalid;
    }

    public BooleanProperty expandedProperty() {
        return expanded;
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public IntegerProperty priorityProperty() {
        return priority;
    }

    public IntegerProperty detailedProperty() {
        return detailed;
    }

    public Color getColor() {
        return color.get();
    }

    public void setColor(Color color) {
        this.color.set(color);
    }

    public boolean isChecked() {
        return checked.get();
    }

    public void setChecked(boolean checked) {
        this.checked.set(checked);
    }

    public boolean isInvalid() {
        return invalid.get();
    }

    public void setInvalid(boolean invalid) {
        this.invalid.set(invalid);
    }

    public boolean isExpanded() {
        return expanded.get();
    }

    public void setExpanded(boolean expanded) {
        this.expanded.set(expanded);
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public int getPriority() {
        return priority.get();
    }

    public void setPriority(int priority) {
        this.priority.set(priority);
    }

    public int getDetailed() {
        return detailed.get();
    }

    public void setDetailed(int detailed) {
        this.detailed.set(detailed);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof ModzItem that &&
                Objects.equals(this.modId, that.modId) &&
                Objects.equals(this.mapFolder, that.mapFolder);
    }

    @Override
    public int hashCode() {
        return hash(modId, mapFolder, workshopId);
    }

    @Override
    public String toString() {
        return "Workshop ID: " + workshopId + "\n" +
                "Mod ID: " + modId + "\n" +
                (type == MAPZ ? ("Map Folder: " + mapFolder + "\n") : "") + "\n" +
                description;
    }

    public static String toINI(Iterable<ModzItem> items) {
        return toINI(items, false);
    }

    public static String toINI(Iterable<ModzItem> items, boolean filter) {
        var mods = new LinkedHashSet<String>();
        var maps = new LinkedHashSet<String>();
        var work = new LinkedHashSet<String>();

        items.forEach(item -> {
            if (filter && (!item.isChecked() || item.isInvalid())) {
                return;
            }

            if (item.type == MAPZ) {
                maps.add(item.mapFolder);
            } else {
                mods.add(item.modId);
                work.add(item.workshopId);
            }
        });

        return "Mods=" + join(";", mods) + "\nMap=" + join(";", maps) + "\nWorkshopItems=" + join(";", work) + "\n";
    }
}
