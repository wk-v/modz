package ru.wkov.modz.control;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import org.apache.commons.lang3.StringUtils;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.ModzUtil;
import ru.wkov.modz.data.ModzItem;
import ru.wkov.modz.event.ModzScroll;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignC.*;
import static ru.wkov.modz.ModzUtil.STEAM_OPENURL;
import static ru.wkov.modz.ModzUtil.STEAM_SEARCH_URI;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzLink extends ListCell<ModzItem> implements ModzBean {

    private final ChangeListener<Boolean> including;

    private final ModzIcon icon;

    private final Label numb;

    private final Label text;

    private final HBox head;

    public ModzLink() {
        icon = new ModzIcon(CHECKBOX_INTERMEDIATE, CHECKBOX_BLANK_OUTLINE);
        icon.addEventFilter(MOUSE_PRESSED, event -> {
            if (event.getButton() == PRIMARY) {
                var item = getItem();
                if (item != null && item.path() != null) {
                    var included = !item.isIncluded();
                    item.setIncluded(included);
                    icon.setState(included);
                }
            }
        });

        numb = new Label();
        text = new Label();

        head = new HBox(icon, numb, text);
        head.getStyleClass().addAll("modz-cell-head");

        including = ModzUtil.prop(icon::setState);

        addStyleClasses("modz-link");
        setGraphic(null);

        var item1 = new MenuItem();
        item1.setOnAction(event -> {
            var item = getItem();
            if (item.path() == null) {
                ModzUtil.explore(STEAM_OPENURL + STEAM_SEARCH_URI + encode(item.id(), UTF_8));
            } else {
                getMainStage().fireEvent(new ModzScroll(item.getPriority(), false, false));
            }
        });

        var menu = new ContextMenu(item1);
        menu.setOnShowing(event -> {
            item1.setText(getItem().path() == null ? "find in steam" : "scroll to mod");
        });

        setContextMenu(menu);
    }

    @Override
    protected void updateItem(ModzItem next, boolean empty) {
        var prev = getItem();
        if (prev != null) {
            prev.includedProperty().removeListener(including);
        }

        super.updateItem(next, empty);

        if (next == null) {
            setGraphic(null);
            return;
        }

        icon.setState(next.isIncluded());
        if (next.path() == null) {
            icon.setCodes(CHECKBOX_BLANK_OFF, CHECKBOX_BLANK_OFF_OUTLINE);
        } else {
            icon.setCodes(CHECKBOX_INTERMEDIATE, CHECKBOX_BLANK_OUTLINE);
        }

        var n = next.getPriority() + 1;

        numb.setText(StringUtils.leftPad((n > 0 ? n : "x") + ":", 6));
        text.setText(next.toString());

        next.includedProperty().addListener(including);

        setGraphic(head);
    }
}
