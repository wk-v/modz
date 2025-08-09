package ru.wkov.modz;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.SetChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import ru.wkov.modz.data.ModzItem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static java.lang.Math.round;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.ThreadLocalRandom.current;
import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.scene.input.Clipboard.getSystemClipboard;
import static javafx.scene.input.DataFormat.PLAIN_TEXT;
import static org.apache.commons.lang3.StringUtils.wrap;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzUtil {

    public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("d MMM, yyyy 'at' HH:mm");

    public static final String STEAM_OPENURL = "steam://openurl/"; // openurl_external

    public static final String STEAM_WORKSHOP_URI = "https://steamcommunity.com/sharedfiles/filedetails/?id=";

    public static final String STEAM_SEARCH_URI = "https://steamcommunity.com/workshop/browse/?appid=108600&searchtext=";

    public static final PseudoClass CHECKED = getPseudoClass("checked");

    public static final PseudoClass INVALID = getPseudoClass("invalid");

    public static final PseudoClass PRESSED = getPseudoClass("pressed");

    public static final PseudoClass UNUSED = getPseudoClass("unused");

    public static final PseudoClass ODD = getPseudoClass("odd");

    public static void clipboard(String value) {
        getSystemClipboard().setContent(Map.of(PLAIN_TEXT, value));
    }

    public static void explore(Object object) {
        explore(object, false);
    }

    public static void explore(Object object, boolean select) {
        try {
            var builder = new ProcessBuilder();
            var command = wrap(object.toString(), '"');
            if (select) {
                builder.command("explorer", "/select,", command);
            } else {
                builder.command("explorer", command);
            }
            builder.start();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static boolean includes(Node node, Node parent) {
        return node != null && parent != null &&
                (node == parent || includes(node.getParent(), parent));
    }

    public static <T extends Node> T find(Object object, Class<T> type) {
        if (object instanceof Node node) {
            if (type.isInstance(node)) {
                return type.cast(node);
            } else {
                return find(node.getParent(), type);
            }
        }
        return null;
    }

    public static <T extends Node> T find(PickResult result, Class<T> type) {
        return find(result.getIntersectedNode(), type);
    }

    public static <T> Comparator<T> sorting(BiPredicate<T, T> predicate, Comparator<T> comparator) {
        return (o1, o2) -> {
            if (predicate.test(o1, o2)) {
                return comparator.compare(o1, o2);
            }
            return 0;
        };
    }

    public static <T> ChangeListener<T> prop(Runnable runnable) {
        return prop(true, runnable);
    }

    public static <T> ChangeListener<T> prop(boolean nullable, Runnable runnable) {
        return (prop, prev, next) -> {
            if (nullable || next != null) {
                runnable.run();
            }
        };
    }

    public static <T> ChangeListener<T> prop(Consumer<T> consumer) {
        return prop(true, consumer);
    }

    public static <T> ChangeListener<T> prop(boolean nullable, Consumer<T> consumer) {
        return (prop, prev, next) -> {
            if (nullable || next != null) {
                consumer.accept(next);
            }
        };
    }

    public static <T> ListChangeListener<T> list(Runnable runnable) {
        return change -> runnable.run();
    }

    public static <T> ListChangeListener<T> list(Consumer<List<? extends T>> consumer) {
        return change -> consumer.accept(change.getList());
    }

    public static <T> ListChangeListener<T> list(BiConsumer<Boolean, T> consumer) {
        return change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(item -> consumer.accept(true, item));
                change.getRemoved().forEach(item -> consumer.accept(false, item));
//                if (change.wasPermutated()) {
//                    for (int i = change.getFrom(); i < change.getTo(); ++i) {
//                        consumer.accept(null, change.getList().get(i));
//                    }
//                }
            }
        };
    }

    public static <K, V> MapChangeListener<K, V> map(Runnable runnable) {
        return change -> runnable.run();
    }

    public static <T> SetChangeListener<T> set(Consumer<Set<? extends T>> consumer) {
        return change -> consumer.accept(change.getSet());
    }

    public static <T> SetChangeListener<T> set(BiConsumer<Boolean, T> consumer) {
        return change -> consumer.accept(change.wasAdded(),
                change.wasAdded() ? change.getElementAdded() : change.getElementRemoved());
    }

    public static Color color(double origin, double bound) {
        return Color.color(
                current().nextDouble(origin, bound),
                current().nextDouble(origin, bound),
                current().nextDouble(origin, bound)
        );
    }

    public static String web(Color color) {
        int r = ((int) round(color.getRed() * 255)) << 24;
        int g = ((int) round(color.getGreen() * 255)) << 16;
        int b = ((int) round(color.getBlue() * 255)) << 8;
        int a = ((int) round(color.getOpacity() * 255));

        return format("#%08X", (r + g + b + a));
    }

    public static String md5Hex(String string) {
        try {
            var bytes = string.getBytes(UTF_8);
            bytes = MessageDigest.getInstance("MD5").digest(bytes);
            return String.format("%032x", new BigInteger(1, bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static String toINI(Iterable<ModzItem> items) {
        return toINI(items, false);
    }

    public static String toINI(Iterable<ModzItem> items, boolean filter) {
        var mods = new LinkedHashSet<String>();
        var maps = new LinkedHashSet<String>();
        var work = new LinkedHashSet<String>();

        items.forEach(item -> {
            if (filter && (!item.isIncluded() || item.isDisabled())) {
                return;
            }

            work.add(item.workshop());
            mods.add(item.id());

            item.maps().forEach(map -> {
                if (!filter || map.isIncluded()) {
                    maps.add(map.id());
                }
            });
        });

        return "Mods=" + join(";", mods) + "\n\nMap=" + join(";", maps) + (maps.isEmpty() ? "" : ";") +
                "Muldraugh, KY" + "\n\nWorkshopItems=" + join(";", work) + "\n";
    }

    public static String toTXT(Iterable<ModzItem> items) {
        var txts = new StringBuilder();
        var maps = new StringBuilder();

        items.forEach(mod -> {
            var texture = Files.exists(mod.path().resolve(Path.of("media", "texturepacks")));

            if (mod.maps().isEmpty()) {
                if (texture) {
                    txts.append(String.format("""
                            %s:
                              mod_name: %s
                              steam_id: '%s'
                              texture: true
                            
                            """, mod.id(), mod.path().getFileName(), mod.workshop()));
                }
            } else {
                for (var map : mod.maps()) {
                    if (!map.isEmpty()) {
                        maps.append(String.format("""
                                %s:
                                  map_name: %s
                                  mod_name: %s
                                  steam_id: '%s'
                                  texture: %s
                                
                                """, map.hash(), map.path().getFileName(), mod.path().getFileName(), mod.workshop(), texture));
                    }
                }
            }
        });

        return txts.append(maps).toString();
    }
}
