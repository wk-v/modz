package ru.wkov.modz.control;

import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.apache.commons.lang3.StringUtils;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.data.ModzItem;
import ru.wkov.modz.http.ModzData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static org.kordamp.ikonli.materialdesign2.MaterialDesignC.CHECKBOX_BLANK_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignC.CHECKBOX_INTERMEDIATE;
import static ru.wkov.modz.ModzUtil.*;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzDrag extends Popup implements ModzBean {

    private static final int MAX_VISIBLE_CELL = 20;

    private final List<ModzCell> cells;

    private final ModzCell cell;

    private Collection<ModzItem> dragged;

    public ModzDrag() {
        cells = new ArrayList<>(MAX_VISIBLE_CELL);
        for (var i = 0; i < MAX_VISIBLE_CELL; i++) {
            cells.add(new ModzCell(i % 2 > 0));
        }

        cell = new ModzCell(false);
        cell.pseudoClassStateChanged(CHECKED, true);
        cell.head.getStyleClass().add("modz-cell-over");
        cell.rate.setVisible(false);
        cell.icon.setVisible(false);
        cell.numb.setVisible(false);

        var vbox = new VBox();
        vbox.setMouseTransparent(true);
        vbox.setOpacity(0.88);

        vbox.getChildren().addAll(cells);
        vbox.getChildren().add(cell);

        getContent().add(vbox);

        setConsumeAutoHidingEvents(false);
        setHideOnEscape(false);
        setAutoHide(false);
        setAutoFix(false);
    }

    public void drag(Collection<ModzItem> items) {
        if (dragged != null || items == null) {
            return;
        }

        dragged = items;

        var iterator = dragged.iterator();
        var over = dragged.size() - cells.size();

        for (var cell : cells) {
            if (iterator.hasNext()) {
                var item = iterator.next();

                cell.accept(item);
                cell.setVisible(true);
            } else {
                cell.setVisible(false);
            }
        }

        if (over > 0) {
            cell.text.setText("and " + over + " more...");
            cell.setVisible(true);
        } else {
            cell.setVisible(false);
        }

        show(getMainStage());
    }

    public Collection<ModzItem> drop() {
        hide();

        var items = dragged;
        dragged = null;

        return items;
    }

    private static class ModzCell extends StackPane implements ModzBean, Consumer<ModzItem> {

        private static final String[] STYLE_CLASSES =
                new ru.wkov.modz.control.ModzCell().getStyleClass().toArray(String[]::new);

        final ModzIcon icon;

        final ModzRate rate;

        final Label numb;

        final Label text;

        final HBox head;

        public ModzCell(boolean odd) {
            addStyleClasses(STYLE_CLASSES);
            pseudoClassStateChanged(ODD, odd);

            icon = new ModzIcon(CHECKBOX_INTERMEDIATE, CHECKBOX_BLANK_OUTLINE);
            rate = new ModzRate();
            numb = new Label();
            text = new Label();

            head = new HBox(icon, numb, rate, text);
            head.getStyleClass().add("modz-more-head");

            var pane = new StackPane(head);
            pane.getStyleClass().add("title");

            var view = new StackPane(pane);
            view.getStyleClass().add("modz-more");

            getChildren().add(view);
            setVisible(false);
        }

        @Override
        public void accept(ModzItem item) {
            pseudoClassStateChanged(CHECKED, item.isIncluded());
            pseudoClassStateChanged(INVALID, item.isDisabled());

            var data = item.dataProperty().get();
            if (data == null) {
                rate.setScore(0.0);
            } else {
                rate.setScore(data.getRate().getScore());
            }

            icon.setState(item.isIncluded());
            numb.setText(StringUtils.leftPad((item.getPriority() + 1) + ":", 6));
            text.setText(item + " ");
        }
    }
}
