package ru.wkov.modz.layout;

import javafx.scene.layout.StackPane;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.control.ModzIcon;
import ru.wkov.modz.data.MapzTile;

import java.util.HashMap;
import java.util.Map;

import static javafx.geometry.Pos.TOP_LEFT;
import static javafx.scene.input.MouseButton.MIDDLE;
import static javafx.scene.paint.Color.*;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignN.*;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzOver extends StackPane implements ModzBean {

    private final Map<MapzTile, MapzIcon> icons;

    public ModzOver() {
        icons = new HashMap<>();
        setAlignment(TOP_LEFT);
    }

    public void add(MapzTile tile) {
        var icon = icons.get(tile);
        if (icon == null) {
            icon = new MapzIcon(tile);

            getChildren().add(icon);
            icons.put(tile, icon);
        }

        icon.nextState();
    }

    public void remove(MapzTile tile) {
        var icon = icons.get(tile);
        if (icon != null) {
            if (icon.prevState() == 0) {
                getChildren().remove(icon);
                icons.remove(tile);
            }
        }
    }

    private static class MapzIcon extends ModzIcon {

        public MapzIcon(MapzTile tile) {
            super(
                    NUMERIC_0_BOX,
                    NUMERIC_1_BOX,
                    NUMERIC_2_BOX,
                    NUMERIC_3_BOX,
                    NUMERIC_4_BOX,
                    NUMERIC_5_BOX,
                    NUMERIC_6_BOX,
                    NUMERIC_7_BOX,
                    NUMERIC_8_BOX,
                    NUMERIC_9_BOX,
                    NUMERIC_9_PLUS_BOX
            );

            int size = getTileSize();

            setTranslateX(tile.x() * size);
            setTranslateY(tile.y() * size);
            setIconSize(size);

            setIconColor(BLACK);
            setStroke(RED);

            setOnMousePressed(event -> {
                if (event.getButton() == MIDDLE && !event.isShiftDown() && !event.isControlDown() && !event.isAltDown()) {
                    if (getIconColor() == BLACK) {
                        setIconColor(WHITE);
                        setStroke(GREEN);
                        setOpacity(0.1);
                    } else {
                        setIconColor(BLACK);
                        setStroke(RED);
                        setOpacity(1.0);
                    }
                }
            });
        }

        public int setState(int state) {
            var max = codes.length - 1;
            var min = 0;

            if (state < min || state > max) {
                return this.state;
            }

            this.state = state;
            return refresh();
        }

        @Override
        protected int refresh() {
            setVisible(state > 1);
            return super.refresh();
        }
    }
}
