package ru.wkov.modz.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import ru.wkov.modz.ModzBean;

import static javafx.scene.control.ContentDisplay.GRAPHIC_ONLY;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.*;
import static javafx.stage.WindowEvent.WINDOW_SHOWING;
import static javafx.util.Duration.INDEFINITE;
import static javafx.util.Duration.ZERO;
import static ru.wkov.modz.ModzUtil.prop;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzHint extends Tooltip implements ModzBean {

    private static final double CORR_SIZE = 12.0;

    private final VBox vbox;

    public ModzHint(Node node) {
        visible = new SimpleBooleanProperty(false);
        visible.addListener(prop(value -> setOpacity(value ? 1.0 : 0.0)));

        vbox = new VBox();

        setConsumeAutoHidingEvents(false);
        setContentDisplay(GRAPHIC_ONLY);
        setHideOnEscape(false);
        setShowDuration(INDEFINITE);
        setShowDelay(ZERO);
        setHideDelay(ZERO);
        setGraphic(vbox);
        setOpacity(0.0);

        addEventFilter(WINDOW_SHOWING, e -> {
            setY(getY() + CORR_SIZE);
            setX(getX() + CORR_SIZE);
        });

        var moved = new SimpleObjectProperty<MouseEvent>();
        var robot = new Robot();

        node.addEventFilter(ANY, event -> {
            setY(robot.getMouseY() + CORR_SIZE);
            setX(robot.getMouseX() + CORR_SIZE);

            var type = event.getEventType();
            var btn = event.getButton();

            if (type == MOUSE_MOVED) {
                moved.set(event);
            } else if (type == MOUSE_CLICKED && btn == PRIMARY) {
                var saved = moved.get();
                if (saved != null) {
                    node.fireEvent(saved.copyFor(event.getSource(), event.getTarget()));
                }
            }
        });

        install(node, this);
    }

    public ObservableList<Node> getChildren() {
        return vbox.getChildren();
    }

    private final BooleanProperty visible;

    public BooleanProperty visibleProperty() {
        return visible;
    }

    public Boolean isVisible() {
        return visibleProperty().get();
    }

    public void setVisible(Boolean visible) {
        visibleProperty().set(visible != null && visible);
    }
}
