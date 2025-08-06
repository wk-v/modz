package ru.wkov.modz.control;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.dialog.ProgressDialog;
import org.kordamp.ikonli.javafx.FontIcon;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.data.ModzItem;
import ru.wkov.modz.http.ModzHttp;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static java.lang.Character.toUpperCase;
import static java.lang.Integer.min;
import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.FileVisitResult.*;
import static java.time.ZoneId.systemDefault;
import static java.util.logging.Level.WARNING;
import static javafx.scene.control.Alert.AlertType.NONE;
import static javafx.scene.control.ButtonType.*;
import static javafx.scene.control.ContentDisplay.RIGHT;
import static javafx.scene.control.OverrunStyle.CLIP;
import static org.apache.commons.lang3.CharUtils.isAsciiPrintable;
import static org.apache.commons.lang3.StringUtils.abbreviateMiddle;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignF.FILE_DOCUMENT_PLUS_OUTLINE;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzLoad extends Button implements ModzBean {

    private static final List<String> DEFAULT_CONTENT_DIRS = List.of(
            "C:/Program Files (x86)/Steam/steamapps/workshop/content/108600/",
            "C:/Program Files/Steam/steamapps/workshop/content/108600/",
            "D:/SteamLibrary/steamapps/workshop/content/108600/"
    );

    private final ModzHttp http;

    public ModzLoad(Consumer<Collection<ModzItem>> consumer) {
        http = new ModzHttp();

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
                getLogger().log(WARNING, "Loading was failed due to unexpected error:", service.getException()));

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

        setGraphic(new FontIcon(FILE_DOCUMENT_PLUS_OUTLINE));
        setOnMouseClicked(event -> search.run());
    }

    private Service<Collection<ModzItem>> getService(DirectoryChooser chooser) {
        return new Service<>() {
            @Override
            protected Task<Collection<ModzItem>> createTask() {
                return new Task<>() {

                    static final PathMatcher PATH_MATCHER = // TODO only B41 is supported for now
                            getDefault().getPathMatcher("glob:**/content/108600/*/mods/*");

                    @Override
                    protected List<ModzItem> call() throws Exception {
                        var paths = new LinkedList<Path>();

                        Files.walkFileTree(chooser.getInitialDirectory().toPath(), new ModzVisitor() {

                            @Override
                            public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) {
                                if (isCancelled()) {
                                    return TERMINATE;
                                }

                                updateMessage(StringUtils.abbreviateMiddle(path.toString(), "...", 65));

                                if (attrs.isDirectory() && PATH_MATCHER.matches(path)) {
                                    paths.add(path);

                                    return SKIP_SUBTREE;
                                }

                                return CONTINUE;
                            }
                        });

                        var maps = getRootPath().resolve("maps");
                        var items = new LinkedHashMap<String, List<ModzItem>>(paths.size());
                        var unknowns = new LinkedList<String>();

                        var progress = 1;
                        for (var path : paths) {
                            if (isCancelled()) {
                                updateMessage("canceling...");
                                return null;
                            }

                            updateMessage(abbreviateMiddle(path.toString(), "...", 65));
                            updateProgress(progress++, paths.size());

                            var item = ModzItem.valueOf(path);
                            if (item != null) {
                                items.computeIfAbsent(item.workshop(), id -> new LinkedList<>()).add(item);
                                for (var map : item.maps()) {
                                    if (!map.isEmpty()) {
                                        item.tags().add("Mapz");
                                        if (!Files.exists(maps.resolve(map.hash()))) {
                                            unknowns.add(map.hash());
                                        }
                                    }
                                }
                            }
                        }

                        if (isCancelled()) {
                            updateMessage("canceling...");
                            return null;
                        }

                        updateMessage("Waiting response from Steam...");
                        updateProgress(-1, -1);

                        var ids = new ArrayList<>(items.keySet());

                        progress = 1;
                        for (int i = 0; i < ids.size(); ) {
                            for (var data : http.getDetails(ids.subList(i, min(i += 200, ids.size())))) {
                                updateMessage("Loading details from workshop: " + data.getWorkshop());
                                updateProgress(progress++, ids.size());

                                data.setCreatedAt(data.getCreatedAt().withZoneSameInstant(systemDefault()));
                                data.setUpdatedAt(data.getUpdatedAt().withZoneSameInstant(systemDefault()));

                                var previews = data.getPreviews();
                                var latch = new CountDownLatch(previews.size());
                                var imgs = new LinkedList<String>();
                                for (var preview : previews) {
                                    var name = preview.getName();
                                    if (isNoneBlank(name, preview.getUrl())) {
                                        var builder = new StringBuilder();
                                        for (var n = 0; n < name.length(); n++) {
                                            var ch = name.charAt(n);
                                            if (isAsciiPrintable(ch)) {
                                                builder.append(toUpperCase(ch));
                                            } else {
                                                builder.append(name.codePointAt(n));
                                            }
                                        }
                                        name = builder.toString();

                                        var path = getRootPath().resolve(Path.of("imgs", data.getWorkshop(), name));
                                        imgs.add("file:" + path.toAbsolutePath());
                                        if (!Files.exists(path)) {
                                            new Thread(() -> {
                                                try (var stream = URI.create(preview.getUrl()).toURL().openStream()) {
                                                    Files.createDirectories(path.getParent());
                                                    Files.copy(stream, path);
                                                } catch (IOException ex) {
                                                    throw new UncheckedIOException(ex);
                                                } finally {
                                                    latch.countDown();
                                                }
                                            }).start();
                                            continue;
                                        }
                                    }
                                    latch.countDown();
                                }
                                latch.await();

                                for (var item : items.get(data.getWorkshop())) {
                                    item.dataProperty().set(data);
                                    for (var tag : data.getTags()) {
                                        item.tags().add(tag.getName());
                                    }
                                    if (!imgs.isEmpty()) {
                                        item.imgs().addAll(0, imgs);
                                    }
                                }
                            }
                        }


                        if (!unknowns.isEmpty()) {
                            var delimiter = "\n    - ";
                            getLogger().log(WARNING, "Following maps don't have generated tiles:"
                                    + delimiter + String.join(delimiter, unknowns));
                        }

                        return items.values().stream().flatMap(List::stream).toList();
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
