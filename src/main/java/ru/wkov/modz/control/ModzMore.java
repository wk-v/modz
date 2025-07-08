package ru.wkov.modz.control;

import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.control.skin.TitledPaneSkin;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import ru.wkov.modz.ModzBean;

import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.ANY;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignM.MENU_DOWN_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignM.MENU_RIGHT_OUTLINE;
import static ru.wkov.modz.ModzUtil.includes;
import static ru.wkov.modz.ModzUtil.prop;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzMore extends TitledPane implements ModzBean {

    private final ModzIcon icon;

    private TitledPaneSkin skin;

    public ModzMore() {
        addStyleClasses("modz-more");

        icon = new ModzIcon(MENU_DOWN_OUTLINE, MENU_RIGHT_OUTLINE);
        icon.getStyleClass().add("modz-more-icon");

        addEventHandler(ANY, event -> {
            var type = event.getEventType();
            var target = event.getTarget();

            if (target instanceof ModzIcon) {
                var btn = event.getButton();
                if (target == icon && type == MOUSE_PRESSED && btn == PRIMARY) {
                    if (getParent() instanceof Accordion menu) {
                        menu.getPanes().forEach(more -> more.setAnimated(true));
                        setExpanded(!isExpanded());
                        menu.getPanes().forEach(more -> more.setAnimated(false));
                    } else {
                        setAnimated(true);
                        setExpanded(!isExpanded());
                        setAnimated(false);
                    }
                }
            } else if (includes((Node) target, getTitlePane())) {
                getParent().fireEvent(event);
            }
        });

        expandedProperty()
                .addListener(prop(false, icon::setState));

        setAnimated(false);
    }

    public void setTitles(Node... nodes) {
        var head = new HBox(icon);
        head.setAlignment(CENTER_LEFT);
        head.getChildren().addAll(nodes);
        head.getStyleClass().add("modz-more-head");

        setGraphic(head);
    }

    public void setContents(Node... nodes) {
        var body = new HBox(nodes);
        body.getStyleClass().add("modz-more-body");

        setContent(body);
    }

    public StackPane getTitlePane() {
        return (StackPane) createDefaultSkin().getChildren().get(1);
    }

    public StackPane getContentPane() {
        return (StackPane) createDefaultSkin().getChildren().get(0);
    }

    @Override
    protected TitledPaneSkin createDefaultSkin() {
        if (skin == null) {
            skin = new TitledPaneSkin(this);
            getTitlePane().setOnMouseReleased(null);
        }
        return skin;
    }
}
