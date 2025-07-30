package ru.wkov.modz.layout;

import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import ru.wkov.modz.ModzBean;

import static javafx.geometry.Orientation.HORIZONTAL;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzBody extends StackPane implements ModzBean {

    public ModzBody() {
        addStyleClasses("modz-body");

        var view = new ModzView();
        var list = new ModzList(view);

        var pane = new SplitPane(view, list);
        pane.setDividerPositions(0.5);
        pane.setOrientation(HORIZONTAL);

        getChildren().add(pane);

        list.load();
    }
}
