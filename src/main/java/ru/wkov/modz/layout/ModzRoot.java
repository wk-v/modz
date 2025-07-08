package ru.wkov.modz.layout;

import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ru.wkov.modz.ModzBean;

import java.util.List;

import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Pos.*;
import static javafx.scene.Cursor.*;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.*;
import static javafx.scene.layout.Priority.ALWAYS;
import static org.apache.commons.lang3.StringUtils.replaceChars;
import static ru.wkov.modz.ModzUtil.*;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzRoot extends StackPane implements ModzBean {

    public ModzRoot() {
        addStyleClasses("modz-root");

        var head = new ModzHead();
        var body = new ModzBody();
        var help = new ModzHelp();

        var vbox = new VBox(head, body);

        VBox.setVgrow(body, ALWAYS);
        getChildren().addAll(vbox, help);

        initResizable();
    }

    private void initResizable() {
        var handler = new EventHandler<MouseEvent>() {

            double screenX, screenY, stageW, stageH, stageX, stageY;

            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() != PRIMARY || getMainStage().isFullScreen()) {
                    return;
                }

                var type = event.getEventType();

                if (type == MOUSE_PRESSED) {
                    screenY = event.getScreenY();
                    screenX = event.getScreenX();

                    stageH = getMainStage().getHeight();
                    stageW = getMainStage().getWidth();

                    stageY = getMainStage().getY();
                    stageX = getMainStage().getX();

                    return;
                }

                if (type != MOUSE_DRAGGED) {
                    return;
                }

                var pos = getAlignment((Node) event.getSource());

                var offsetY = event.getScreenY() - screenY;
                var offsetX = event.getScreenX() - screenX;

                var minH = getMainStage().getMinHeight();
                var minW = getMainStage().getMinWidth();

                var h = stageH;
                var w = stageW;
                var y = stageY;
                var x = stageX;

                switch (pos.getHpos()) {
                    case RIGHT -> w += Math.max(offsetX, minW - w);
                    case LEFT -> {
                        offsetX = Math.min(offsetX, w - minW);
                        w -= offsetX;
                        x += offsetX;
                    }
                }

                switch (pos.getVpos()) {
                    case BOTTOM -> h += Math.max(offsetY, minH - h);
                    case TOP -> {
                        offsetY = Math.min(offsetY, h - minH);
                        h -= offsetY;
                        y += offsetY;
                    }
                }

                getMainStage().setHeight(h);
                getMainStage().setWidth(w);

                getMainStage().setY(y);
                getMainStage().setX(x);
            }
        };

        var borders = List.of(
                new ModzBorder(handler, ModzCompass.N),
                new ModzBorder(handler, ModzCompass.S),
                new ModzBorder(handler, ModzCompass.E),
                new ModzBorder(handler, ModzCompass.W)
        );

        getChildren()
                .addAll(borders);

        var corners = List.of(
                new ModzCorner(handler, ModzCompass.NE, borders),
                new ModzCorner(handler, ModzCompass.NW, borders),
                new ModzCorner(handler, ModzCompass.SE, borders),
                new ModzCorner(handler, ModzCompass.SW, borders)
        );

        getChildren()
                .addAll(corners);
    }

    private static class ModzBorder extends Separator implements ModzBean {

        final ModzCompass compass;

        ModzBorder(EventHandler<MouseEvent> handler, ModzCompass compass) {
            addStyleClasses("modz-root-border");

            getMainStage()
                    .fullScreenProperty()
                    .addListener(prop(false, this::setMouseTransparent));

            setCursor(compass.cursor);
            addEventFilter(ANY, handler);
            setOrientation(compass.orientation);
            pseudoClassStateChanged(compass.pseudo, true);

            this.compass = compass;
            setAlignment(this, compass.pos);
        }
    }

    private static class ModzCorner extends Label implements ModzBean {

        public ModzCorner(EventHandler<MouseEvent> handler, ModzCompass compass, List<ModzBorder> borders) {
            addStyleClasses("modz-root-corner");

            getMainStage()
                    .fullScreenProperty()
                    .addListener(prop(false, this::setMouseTransparent));

            setCursor(compass.cursor);
            addEventFilter(ANY, handler);
            pseudoClassStateChanged(compass.pseudo, true);

            getPseudoClassStates().addListener(set((active, pseudo) -> {
                if (PRESSED.equals(pseudo)) {
                    for (var border : borders) {
                        if (compass.relates(border.compass)) {
                            border.pseudoClassStateChanged(pseudo, active);
                        }
                    }
                }
            }));

            StackPane.setAlignment(this, compass.pos);
        }
    }

    private enum ModzCompass {

        N(HORIZONTAL, N_RESIZE, TOP_CENTER),
        S(HORIZONTAL, S_RESIZE, BOTTOM_CENTER),
        E(VERTICAL, E_RESIZE, CENTER_RIGHT),
        W(VERTICAL, W_RESIZE, CENTER_LEFT),

        NE(null, NE_RESIZE, TOP_RIGHT),
        NW(null, NW_RESIZE, TOP_LEFT),
        SE(null, SE_RESIZE, BOTTOM_RIGHT),
        SW(null, SW_RESIZE, BOTTOM_LEFT);

        final Orientation orientation;
        final PseudoClass pseudo;
        final Cursor cursor;
        final Pos pos;

        ModzCompass(Orientation orientation, Cursor cursor, Pos pos) {
            this.orientation = orientation;
            this.cursor = cursor;
            this.pos = pos;

            pseudo = getPseudoClass(replaceChars(pos.name().toLowerCase(), '_', '-'));
        }

        boolean relates(ModzCompass that) {
            return this.pos.getHpos() == that.pos.getHpos() ||
                    this.pos.getVpos() == that.pos.getVpos();
        }
    }
}
