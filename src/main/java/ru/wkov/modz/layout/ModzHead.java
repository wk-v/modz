package ru.wkov.modz.layout;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.event.ModzAbout;
import ru.wkov.modz.event.ModzRandom;
import ru.wkov.modz.event.ModzReset;

import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Pos.*;
import static javafx.scene.Cursor.DEFAULT;
import static javafx.scene.Cursor.MOVE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.*;
import static javafx.scene.paint.Color.BLACK;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignI.INFORMATION_BOX_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignM.MENU;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignP.*;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignR.ROTATE_3D;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignW.*;
import static ru.wkov.modz.ModzUtil.prop;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzHead extends StackPane implements ModzBean {

    public ModzHead() {
        addStyleClasses("modz-head");
        var stage = getMainStage();

        var menu = new MenuButton("MODZ", new FontIcon(MENU),
                new CustomMenuItem(new Label("Reset Map View", new FontIcon(ROTATE_3D)), true) {{
                    setOnAction(event -> stage.fireEvent(new ModzReset()));
                }},
                new CustomMenuItem(new Label("Random Colors", new FontIcon(PALETTE_OUTLINE)), false) {{
                    getStyleClass().add("menu-item");
                    getStyleClass().remove("custom-menu-item");
                    setOnAction(event -> stage.fireEvent(new ModzRandom()));
                }},
                new SeparatorMenuItem(),
                new CustomMenuItem(new Label("About", new FontIcon(INFORMATION_BOX_OUTLINE)), true) {{
                    setOnAction(event -> stage.fireEvent(new ModzAbout()));
                }});

        var wpin = new Button();
        {
            var icon = new FontIcon(PIN_OFF);
            wpin.setGraphic(icon);
            wpin.setOnAction(event -> {
                if (stage.isAlwaysOnTop()) {
                    stage.setAlwaysOnTop(false);
                    icon.setIconColor(BLACK);
                    icon.setIconCode(PIN_OFF);
                } else {
                    stage.setAlwaysOnTop(true);
                    icon.setIconColor(getMainColor());
                    icon.setIconCode(PIN);
                }
            });
        }

        var wmin = new Button();
        {
            wmin.setGraphic(new FontIcon(WINDOW_MINIMIZE));
            wmin.setOnAction(event -> stage.setIconified(true));
        }

        var wmax = new Button();
        {
            var icon = new FontIcon(WINDOW_MAXIMIZE);
            wmax.setGraphic(icon);
            wmax.setOnAction(event -> {
                if (stage.isMaximized()) {
                    stage.setMaximized(false);
                    icon.setIconCode(WINDOW_MAXIMIZE);
                } else {
                    stage.setMaximized(true);
                    icon.setIconCode(WINDOW_RESTORE);
                }
            });
        }

        var exit = new Button();
        {
            exit.setGraphic(new FontIcon(WINDOW_CLOSE));
            exit.setOnAction(event -> Platform.exit());
        }

        var hbox = new HBox(wpin, new Separator(VERTICAL), wmin, wmax, exit, new Separator(VERTICAL));
        hbox.setMaxWidth(USE_PREF_SIZE);

        var drag = new HBox();
        drag.setAlignment(CENTER);
        drag.setCursor(MOVE);

        getChildren().addAll(drag, menu, hbox);

        setAlignment(drag, CENTER);
        setAlignment(menu, CENTER_LEFT);
        setAlignment(hbox, CENTER_RIGHT);

        initMovable(drag);

        var listener = prop(unused -> {
            if (stage.isMaximized()) {
                stage.setX(0);
                stage.setY(0);

                stage.setMinHeight(stage.getMaxHeight());
                stage.setMinWidth(stage.getMaxWidth());

                drag.setCursor(DEFAULT);
            } else {
                stage.setMinHeight(250);
                stage.setMinWidth(250);

                drag.setCursor(MOVE);
            }
        });

        stage.iconifiedProperty().addListener(listener);
        stage.maximizedProperty().addListener(listener);
    }

    private void initMovable(Node node) {
        var offset = new double[2];
        var stage = getMainStage();

        node.addEventFilter(ANY, event -> {
            if (!(event.getTarget() == node) ||
                    event.getButton() != PRIMARY ||
                    stage.isMaximized()) {
                return;
            }

            var type = event.getEventType();
            if (type == MOUSE_PRESSED) {
                offset[0] = event.getSceneX();
                offset[1] = event.getSceneY();
            } else if (type == MOUSE_DRAGGED) {
                stage.setX(event.getScreenX() - offset[0]);
                stage.setY(event.getScreenY() - offset[1]);
            }
        });
    }
}
