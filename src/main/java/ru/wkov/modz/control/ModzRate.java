package ru.wkov.modz.control;

import javafx.scene.layout.HBox;
import ru.wkov.modz.ModzBean;

import java.util.List;

import static org.kordamp.ikonli.materialdesign2.MaterialDesignS.*;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzRate extends HBox implements ModzBean {

    public ModzRate() {
        addStyleClasses("modz-rate");
        setMouseTransparent(true);
        for (int i = 0; i < 5; i++) {
            getChildren().add(new ModzIcon(STAR_OUTLINE, STAR_HALF_FULL, STAR));
        }
    }

    public void setScore(double score) {
        score = score * 5.0;

        for (int i = 0; i < 5; i++) {
            var star = (ModzIcon) getChildren().get(i);
            if (i + 1.0 < score) {
                star.setState(2);
            } else if (i + 0.5 < score) {
                star.setState(1);
            } else {
                star.setState(0);
            }
        }
    }
}
