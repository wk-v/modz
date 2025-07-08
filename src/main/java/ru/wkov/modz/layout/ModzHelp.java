package ru.wkov.modz.layout;

import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import ru.wkov.modz.ModzBean;

import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static ru.wkov.modz.ModzUtil.prop;
import static ru.wkov.modz.event.ModzAbout.ABOUT;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzHelp extends StackPane implements ModzBean {

    public ModzHelp() {
        addStyleClasses("modz-help");

        var view = new ImageView(getAboutLogo());
        view.setPreserveRatio(true);

        getChildren().add(view);
        setVisible(false);

        getMainStage().widthProperty().addListener(prop(false,
                width -> view.setFitWidth(width.doubleValue())));

        getMainStage().addEventFilter(ABOUT, event -> setVisible(true));
        addEventFilter(MOUSE_CLICKED, event -> setVisible(false));
    }
}
