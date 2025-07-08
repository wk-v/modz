package ru.wkov.modz;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.SetChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.lang.Math.round;
import static java.lang.String.format;
import static java.util.concurrent.ThreadLocalRandom.current;
import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.scene.paint.Color.BLACK;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzUtil {

    public static final Effect ICON_EFFECT = new DropShadow(3.0, BLACK.brighter().brighter());

    public static final String STEAM_OPENURL = "steam://openurl/"; // openurl_external

    public static final String STEAM_WORKSHOP_URI = "https://steamcommunity.com/sharedfiles/filedetails/?id=";

    public static final String STEAM_SEARCH_URI = "https://steamcommunity.com/workshop/browse/?appid=108600&searchtext=";

    public static final PseudoClass CHECKED = getPseudoClass("checked");

    public static final PseudoClass INVALID = getPseudoClass("invalid");

    public static final PseudoClass PRESSED = getPseudoClass("pressed");

    public static final PseudoClass ODD = getPseudoClass("odd");

    public static void explore(Object object) {
        explore(object, false);
    }

    public static void explore(Object object, boolean select) {
        try {
            Runtime.getRuntime().exec("explorer " + (select ? "/select, \"" : "\"") + object + "\"");
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

    public static <T> ListChangeListener<T> list(Consumer<List<? extends T>> consumer) {
        return change -> consumer.accept(change.getList());
    }

    public static <T> ListChangeListener<T> list(BiConsumer<Boolean, T> consumer) {
        return change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(item -> consumer.accept(true, item));
                change.getRemoved().forEach(item -> consumer.accept(false, item));
            }
        };
    }

    public static <K, V> MapChangeListener<K, V> map(Runnable runnable) {
        return change -> runnable.run();
    }

    public static <T> SetChangeListener<T> set(BiConsumer<Boolean, T> consumer) {
        return change -> consumer.accept(change.wasAdded(),
                change.wasAdded() ? change.getElementAdded() : change.getElementRemoved());
    }

    public static Color color() {
        return Color.color(
                current().nextDouble(0.28, 0.88),
                current().nextDouble(0.28, 0.88),
                current().nextDouble(0.28, 0.88)
        );
    }

    public static String web(Color color) {
        int r = ((int) round(color.getRed() * 255)) << 24;
        int g = ((int) round(color.getGreen() * 255)) << 16;
        int b = ((int) round(color.getBlue() * 255)) << 8;
        int a = ((int) round(color.getOpacity() * 255));

        return format("#%08X", (r + g + b + a));
    }
}
