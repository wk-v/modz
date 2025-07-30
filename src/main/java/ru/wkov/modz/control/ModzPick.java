package ru.wkov.modz.control;

import javafx.application.Platform;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Labeled;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.ColorPickerSkin;
import org.kordamp.ikonli.javafx.FontIcon;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.event.ModzColor;

import static org.kordamp.ikonli.materialdesign2.MaterialDesignF.FORMAT_PAINT;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzPick extends ColorPicker implements ModzBean {

    public ModzPick() {
        addStyleClasses("button");
        setOnAction(event ->
                getMainStage().fireEvent(new ModzColor(getValue())));
    }

    @Override
    public void hide() {
        Platform.runLater(super::hide); // [JDK-8260024]
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        var skin = new ColorPickerSkin(this);

        var text = (Labeled) skin.getDisplayNode();
        text.setGraphic(FontIcon.of(FORMAT_PAINT, getDarkColor()));

        return skin;
    }
}
