package ru.wkov.modz.control;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.ColorPickerSkin;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;
import ru.wkov.modz.ModzBean;
import ru.wkov.modz.data.ModzItem;

import java.util.HashSet;
import java.util.List;

import static javafx.scene.text.TextAlignment.CENTER;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignF.FORMAT_PAINT;
import static ru.wkov.modz.ModzUtil.*;
import static ru.wkov.modz.data.ModzType.MAPZ;
import static ru.wkov.modz.event.ModzRandom.RANDOM;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzPick extends ColorPicker implements ModzBean {

    public ModzPick(ObservableList<ModzItem> uploaded,
                    ObservableList<ModzItem> selected) {

        selected.addListener(list(items -> refresh(items)));
        setOnAction(event -> selected.forEach(this::setColor));

        getMainStage().addEventFilter(RANDOM, event -> {
            uploaded.forEach(item -> item.setColor(color()));
            refresh(selected);
        });

        setValue(getDarkColor());
    }

    @Override
    public void hide() {
        Platform.runLater(super::hide); // [JDK-8260024]
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        var skin = (ColorPickerSkin) super.createDefaultSkin();
        var text = (Label) skin.getDisplayNode();

        var tooltip = new Tooltip();
        tooltip.setGraphic(text.getGraphic());
        tooltip.setTextAlignment(CENTER);
        tooltip.setGraphicTextGap(10.0);
        setTooltip(tooltip);

        var graphic = new FontIcon(FORMAT_PAINT);
        graphic.setIconColor(getDarkColor());
        text.setGraphic(graphic);

        valueProperty().addListener(prop(false, color -> {
            text.setTextFill(color);
            graphic.setIconColor(color);
            tooltip.setText(web(color));
        }));

        return skin;
    }

    private void refresh(List<? extends ModzItem> items) {
        if (items.isEmpty()) {
            setDisable(true);
            setValue(getDarkColor());
        } else {
            var uniqs = new HashSet<Color>();
            for (var item : items) {
                if (item == null) {
                    continue; // "Ctrl+A" includes empty items
                }

                uniqs.add(item.getColor());

                if (uniqs.size() > 1 || isNotMap(item)) {
                    uniqs.clear();
                    uniqs.add(getDarkColor());

                    break;
                }
            }

            setDisable(items.stream().anyMatch(this::isNotMap));
            setValue(uniqs.iterator().next());
        }
    }

    private boolean isNotMap(ModzItem item) {
        return item.getType() != MAPZ;
    }

    private void setColor(ModzItem item) {
        item.setColor(getValue());
    }
}
