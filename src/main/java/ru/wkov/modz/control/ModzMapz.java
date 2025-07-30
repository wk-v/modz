package ru.wkov.modz.control;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.ModzUtil;
import ru.wkov.modz.data.MapzItem;

import static javafx.scene.Cursor.HAND;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.paint.Color.BLACK;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignC.CHECKBOX_BLANK_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignC.CHECKBOX_INTERMEDIATE;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzMapz extends ListCell<MapzItem> implements ModzBean {

    private final ChangeListener<Color> coloring;

    private final ModzIcon icon;

    private final Label text;

    private final HBox head;

    public ModzMapz() {
        icon = new ModzIcon(CHECKBOX_INTERMEDIATE, CHECKBOX_BLANK_OUTLINE);
        icon.addEventFilter(MOUSE_PRESSED, event -> {
            if (event.getButton() == PRIMARY) {
                var item = getItem();
                if (item != null) {
                    var included = !item.isIncluded();

                    item.setIncluded(included);
                    icon.setState(included);
                }
            }
        });
        icon.setCursor(HAND);
        icon.setEffect(new DropShadow(3.0, BLACK.brighter()));
        icon.setIconSize(20);

        text = new Label();

        head = new HBox(new StackPane(icon), text);
        head.getStyleClass().add("modz-cell-head");

        coloring = ModzUtil.prop(icon::setIconColor);

        addStyleClasses("modz-mapz");
        setGraphic(null);
    }

    @Override
    protected void updateItem(MapzItem next, boolean empty) {
        var prev = getItem();
        if (prev != null) {
            prev.colorProperty().removeListener(coloring);
        }

        super.updateItem(next, empty);

        if (next == null) {
            setGraphic(null);
            return;
        }

        icon.setState(next.isIncluded());
        icon.setIconColor(next.getColor());

        text.setText(next.toString());

        next.colorProperty().addListener(coloring);

        setGraphic(head);
    }
}
