package ru.wkov.modz.control;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Popup;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.data.ModzItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.scene.paint.Color.BLACK;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignC.*;
import static ru.wkov.modz.ModzUtil.*;
import static ru.wkov.modz.data.ModzType.MAPZ;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzDrag extends Popup implements ModzBean {

    private static final int MAX_SIZE = 20;

    private final List<ModzItem> dragged;

    private final List<ModzCell> cells;

    private final ModzCell cell;

    private final Robot robot;

    public ModzDrag() {
        dragged = new ArrayList<>();

        cells = new ArrayList<>(MAX_SIZE);
        robot = new Robot();

        var vbox = new VBox();
        vbox.setMouseTransparent(true);
        vbox.setOpacity(0.75);

        getContent().add(vbox);

        for (var i = 0; i < MAX_SIZE; i++) {
            cells.add(new ModzCell(i % 2 > 0));
        }

        vbox.getChildren().addAll(cells);

        cell = new ModzCell(false);
        cell.addStyleClasses("modz-cell-over");
        cell.head.setAlignment(CENTER_RIGHT);
        cell.icon.setVisible(false);
        cell.numb.setVisible(false);

        vbox.getChildren().add(cell);

        setConsumeAutoHidingEvents(false);
        setHideOnEscape(false);
        setAutoHide(false);
        setAutoFix(false);
    }

    public List<ModzItem> getDragged() {
        return dragged;
    }

    public void drag() {
        if (!isShowing() && !dragged.isEmpty()) {

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
                cell.name.setText("and " + over + " more...");
                cell.setVisible(true);
            } else {
                cell.setVisible(false);
            }

            show(getMainStage());
        }

        setX(robot.getMouseX() + 1.0);
        setY(robot.getMouseY());
    }

    public void drop() {
        hide();
    }

    private static class ModzCell extends StackPane implements ModzBean, Consumer<ModzItem> {

        static final String[] STYLE_CLASSES = new ListCell<>().getStyleClass().toArray(String[]::new);

        final ModzIcon icon;

        final Label numb;

        final Label name;

        final HBox head;

        ModzCell(boolean odd) {
            addStyleClasses(STYLE_CLASSES);
            addStyleClasses("modz-cell-nested", "modz-cell-dragged");

            pseudoClassStateChanged(ODD, odd);

            setVisible(false);

            icon = new ModzIcon();
            icon.setEffect(ICON_EFFECT);

            numb = new Label();
            name = new Label();

            head = new HBox(icon, numb, name);
            head.setAlignment(CENTER_LEFT);
            head.getStyleClass().add("modz-cell-head");

            getChildren().add(head);
        }

        @Override
        public void accept(ModzItem item) {
            var checked = item.isChecked();
            var invalid = item.isInvalid();
            var selected = item.isSelected();
            var priority = item.getPriority();

            pseudoClassStateChanged(CHECKED, checked);
            pseudoClassStateChanged(INVALID, invalid);

            var type = item.getType();
            switch (type) {
                case CARZ -> icon.setCodes(CAR, CAR_OFF);
                case MAPZ -> icon.setCodes(CHECKBOX_INTERMEDIATE, CHECKBOX_BLANK_OFF_OUTLINE);
                case MODZ -> icon.setCodes(COG_OUTLINE, COG_OFF_OUTLINE);
            }

            var color = selected ? getMainColor() : BLACK;
            if (checked) {
                if (invalid) {
                    color = selected ? getFailColor() : BLACK;
                } else if (type == MAPZ) {
                    color = item.getColor();
                }
            }

            icon.setIconColor(color);
            icon.setState(checked);

            name.setText(item.getTitle());
            numb.setText(leftPad(String.valueOf(priority + 1), 4) + ":");
        }
    }
}
