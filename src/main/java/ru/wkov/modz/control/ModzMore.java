package ru.wkov.modz.control;

import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Skin;
import javafx.scene.control.TitledPane;
import javafx.scene.control.skin.TitledPaneSkin;
import javafx.scene.layout.HBox;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.ModzUtil;

import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.ANY;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignM.MENU_DOWN_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignM.MENU_RIGHT_OUTLINE;
import static ru.wkov.modz.ModzUtil.prop;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzMore extends TitledPane implements ModzBean {

    private final ModzIcon fold;

    private final HBox head;

    private final HBox body;

    public ModzMore() {
        fold = new ModzIcon(MENU_DOWN_OUTLINE, MENU_RIGHT_OUTLINE);

        head = new HBox(fold);
        head.getStyleClass().add("modz-more-head");

        body = new HBox();
        body.getStyleClass().add("modz-more-body");

        addEventHandler(ANY, event -> {
            var target = event.getTarget();
            if (target instanceof ModzIcon) {
                if (target == fold) {
                    if (event.getEventType() == MOUSE_PRESSED && event.getButton() == PRIMARY) {
                        var expanded = !isExpanded();

                        if (getParent() instanceof Accordion menu) {
                            menu.getPanes().forEach(more -> more.setAnimated(true));
                            setExpanded(expanded);
                            menu.getPanes().forEach(more -> more.setAnimated(false));
                        } else {
                            setAnimated(true);
                            setExpanded(expanded);
                            setAnimated(false);
                        }
                    }
                }
            } else if (target instanceof Node node) {
                if (ModzUtil.includes(node, head.getParent())) {
                    getParent().fireEvent(event);
                }
            }
        });

        expandedProperty().addListener(prop(false, fold::setState));
        addStyleClasses("modz-more");
        setAnimated(false);
        setContent(body);
        setGraphic(head);
    }

    public void setTitles(Node... nodes) {
        head.getChildren().setAll(fold);
        head.getChildren().addAll(nodes);
    }

    public void setContents(Node... nodes) {
        body.getChildren().setAll(nodes);
        body.visibleProperty().addListener(prop(false, visible -> {
            if (visible) {
                body.getChildren().setAll(nodes);
            } else {
                body.getChildren().clear();
            }
        }));
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        var skin = (TitledPaneSkin) super.createDefaultSkin();
        skin.getChildren().get(1).setOnMouseReleased(null);

        return skin;
    }
}
