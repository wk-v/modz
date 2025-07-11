package ru.wkov.modz.control;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import org.controlsfx.dialog.ProgressDialog;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.ModzInfo;
import ru.wkov.modz.data.ModzItem;
import ru.wkov.modz.data.ModzTile;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.FileVisitResult.*;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.walkFileTree;
import static javafx.scene.control.Alert.AlertType.NONE;
import static javafx.scene.control.ButtonType.*;
import static javafx.scene.control.ContentDisplay.RIGHT;
import static javafx.scene.control.OverrunStyle.CLIP;
import static org.apache.commons.lang3.StringUtils.abbreviateMiddle;
import static org.apache.commons.lang3.StringUtils.firstNonBlank;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignF.FOLDER_OPEN_OUTLINE;
import static ru.wkov.modz.data.ModzType.*;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzLoad extends Button implements ModzBean {

    private static final Logger logger = LoggerFactory.getLogger(ModzLoad.class);

    private static final List<String> DEFAULT_CONTENT_DIRS = List.of(
            "C:/Program Files (x86)/Steam/steamapps/workshop/content/108600/",
            "C:/Program Files/Steam/steamapps/workshop/content/108600/",
            "D:/SteamLibrary/steamapps/workshop/content/108600/"
    );

    public ModzLoad(Consumer<Collection<ModzItem>> consumer) {
        var chooser = new DirectoryChooser();
        chooser.setTitle("Please select directory of installed mod(s)");

        DEFAULT_CONTENT_DIRS
                .stream()
                .map(File::new)
                .filter(File::exists)
                .findFirst()
                .ifPresent(chooser::setInitialDirectory);

        var service = getService(chooser);

        service.setExecutor(Executors.newSingleThreadExecutor(task -> {
            var thread = new Thread(task);
            thread.setDaemon(true);

            return thread;
        }));

        service.setOnFailed(event ->
                logger.error("failed due to unexpected error", service.getException()));

        var search = (Runnable) () -> {
            var file = chooser.showDialog(getMainStage());
            if (file != null) {
                chooser.setInitialDirectory(file);
                if (service.isRunning()) {
                    service.restart();
                } else {
                    service.reset();
                    service.start();
                }
            }
        };

        var alert = initDialog(new Alert(NONE));
        alert.getButtonTypes().addAll(YES, NO);
        alert.setGraphic(getColorIcon());

        var progress = initDialog(new ProgressDialog(service));

        progress.setOnHidden(event -> {
            var items = service.getValue();
            if (items == null) {
                return;
            }

            if (items.isEmpty()) {
                alert.setContentText("No mods found.\n Do you want to repeat the search?");
            } else {
                alert.setContentText(items.size() + " mod(s) found.\n Do you want to add new ones?");
            }

            alert.showAndWait().ifPresent(type -> {
                if (type == YES) {
                    if (items.isEmpty()) {
                        search.run();
                    } else {
                        consumer.accept(items);
                    }
                }
            });
        });

        var pane = progress.getDialogPane();
        pane.setPrefHeight(125.0);
        pane.setPrefWidth(575.0);
        pane.getButtonTypes().add(CANCEL);
        pane.lookupButton(CANCEL)
                .setOnMouseClicked(event -> service.cancel());

        pane.setHeader(new Label("Please wait while mod files is being processed...................", getColorIcon()) {{
            setContentDisplay(RIGHT);
            setGraphicTextGap(100);
            setTextOverrun(CLIP);
            setWrapText(false);
            setWidth(600.0);
        }});

        progress.setOnShown(event -> {
            progress.setX(getMainStage().getX() + (getMainStage().getWidth() - pane.getWidth()) / 2);
            progress.setY(getMainStage().getY() + (getMainStage().getHeight() - pane.getHeight()) / 2);
        });

        setGraphic(new FontIcon(FOLDER_OPEN_OUTLINE));
        setOnMouseClicked(event -> search.run());
    }

    private Service<List<ModzItem>> getService(DirectoryChooser chooser) {
        return new Service<>() {

            @Override
            protected Task<List<ModzItem>> createTask() {
                return new Task<>() {

                    static final PathMatcher PATH_MATCHER = // TODO only B41 is supported for now
                            getDefault().getPathMatcher("glob:**/content/108600/*/mods/*");

                    @Override
                    protected List<ModzItem> call() throws Exception {
                        var items = new LinkedList<ModzItem>();

                        walkFileTree(chooser.getInitialDirectory().toPath(), new ModzVisitor() {

                            @Override
                            public FileVisitResult preVisitDirectory(Path modPath, BasicFileAttributes modAttrs) {
                                try {
                                    updateMessage(abbreviateMiddle(modPath.toString(), "...", 65));
                                    if (modAttrs.isDirectory() && PATH_MATCHER.matches(modPath)) {
                                        var path = modPath.resolve("mod.info");
                                        if (exists(path)) {
                                            var info = new ModzInfo();
                                            info.load(path);

                                            var maps = new HashMap<Path, Set<ModzTile>>();

                                            path = modPath
                                                    .resolve("media")
                                                    .resolve("maps");

                                            if (exists(path)) {
                                                walkFileTree(path, new ModzVisitor() {

                                                    @Override
                                                    public FileVisitResult preVisitDirectory(Path mapPath, BasicFileAttributes mapAttrs) {
                                                        updateMessage(abbreviateMiddle(mapPath.toString(), "...", 65));
                                                        if (mapPath.getParent().endsWith("maps")) {
                                                            maps.put(mapPath, new HashSet<>());
                                                        }

                                                        return CONTINUE;
                                                    }

                                                    @Override
                                                    public FileVisitResult visitFile(Path mapPath, BasicFileAttributes mapAttrs) {
                                                        updateMessage(abbreviateMiddle(mapPath.toString(), "...", 65));
                                                        if (mapAttrs.isRegularFile()) {
                                                            var name = mapPath.getFileName().toString();
                                                            var d = name.indexOf(".lotheader");
                                                            if (d != -1) {
                                                                var u = name.indexOf('_');
                                                                var x = name.substring(0, u);
                                                                var y = name.substring(u + 1, d);

                                                                maps.get(mapPath.getParent()).add(new ModzTile(x, y));
                                                            }
                                                        }

                                                        return CONTINUE;
                                                    }
                                                });
                                            }

                                            var type = MODZ;

                                            path = modPath.resolve("media");
                                            if (exists(path.resolve("texturepacks")) || exists(path.resolve("textures"))) {
                                                type = TXTR;
                                            } else if (exists(path.resolve("scripts").resolve("vehicles"))) {
                                                type = CARZ;
                                            }

                                            var modz = new ModzItem(
                                                    modPath,
                                                    null,
                                                    info.val("id"),
                                                    info.val("name"),
                                                    null,
                                                    modPath.getParent().getParent().getFileName().toString(),
                                                    firstNonBlank(info.val("description"), "[no description]"),
                                                    type,
                                                    null,
                                                    info.vals("require"),
                                                    info.vals("poster")
                                            );

                                            items.add(modz);

                                            maps.forEach((mapPath, tiles) -> {
                                                var item = new ModzItem(
                                                        modz.getModPath(),
                                                        mapPath,
                                                        modz.getModId(),
                                                        modz.getModName(),
                                                        mapPath.getFileName().toString(),
                                                        modz.getWorkshopId(),
                                                        modz.getDescription(),
                                                        MAPZ,
                                                        tiles,
                                                        List.of(modz.getModId()),
                                                        null
                                                ) {

                                                    @Override
                                                    public List<Image> getImages() {
                                                        return modz.getImages();
                                                    }
                                                };

                                                items.add(item);
                                            });
                                        }

                                        if (isCancelled()) {
                                            return TERMINATE;
                                        }

                                        return SKIP_SUBTREE;
                                    }
                                } catch (Exception ex) {
                                    logger.error("failed to parse content: {}", modPath, ex);
                                }

                                return CONTINUE;
                            }
                        });

                        if (isCancelled()) {
                            return null;
                        }

                        return items;
                    }
                };
            }
        };
    }

    private interface ModzVisitor extends FileVisitor<Path> {

        @Override
        default FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) {
            return CONTINUE;
        }

        @Override
        default FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
            return CONTINUE;
        }

        @Override
        default FileVisitResult visitFileFailed(Path path, IOException error) {
            return CONTINUE;
        }

        @Override
        default FileVisitResult postVisitDirectory(Path path, IOException error) {
            return CONTINUE;
        }
    }
}