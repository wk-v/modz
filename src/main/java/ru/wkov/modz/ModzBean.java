package ru.wkov.modz;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;

import static javafx.stage.Modality.APPLICATION_MODAL;
import static javafx.stage.StageStyle.UNDECORATED;
import static ru.wkov.modz.ModzMain.*;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public interface ModzBean {

    default Logger getLogger() {
        return logger;
    }

    default Stage getMainStage() {
        return mainStage;
    }

    default Color getMainColor() {
        return mainColor;
    }

    default Color getDarkColor() {
        return darkColor;
    }

    default Color getFailColor() {
        return failColor;
    }

    default Image getAboutLogo() {
        return aboutLogo;
    }

    default Image getBlackLogo() {
        return blackIcon;
    }

    default Image getColorLogo() {
        return colorIcon;
    }

    default String getText(String name, Charset charset) {
        try (var stream = getResource(name).openStream()) {
            return new String(stream.readAllBytes(), charset);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    default ImageView getColorIcon() {
        var view = new ImageView();
        view.setPreserveRatio(true);
        view.setFitWidth(32.0);
        view.setImage(getColorLogo());

        return view;
    }

    default <R, T extends Dialog<R>> T initDialog(T dialog) {
        dialog.initModality(APPLICATION_MODAL);
        dialog.initStyle(UNDECORATED);

        var property = mainStage.showingProperty();
        if (property.get()) {
            dialog.initOwner(mainStage);
        } else {
            var listener = new InvalidationListener() {

                @Override
                public void invalidated(Observable observable) {
                    if (property.get()) {
                        property.removeListener(this);
                        dialog.initOwner(mainStage);
                    }
                }
            };

            property.addListener(listener);
        }

        return dialog;
    }

    default ObservableList<String> getStyleClass() {
        throw new UnsupportedOperationException();
    }

    default void addStyleClasses(String... names) {
        getStyleClass().addAll(names);
    }

    default void removeStyleClass(String... names) {
        getStyleClass().removeAll(names);
    }
}
