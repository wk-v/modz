package ru.wkov.modz.control;

import javafx.scene.control.Pagination;
import javafx.scene.control.Skin;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.PaginationSkin;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import ru.wkov.modz.ModzBean;

import java.util.List;

import static javafx.scene.control.ContentDisplay.GRAPHIC_ONLY;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.stage.Screen.getPrimary;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzPage extends Pagination implements ModzBean {

    private final List<Image> empty;

    private final ImageView preview;

    private List<Image> pages;

    public ModzPage(double width, double height) {
        addStyleClasses("modz-page");

        setMaxHeight(height);
        setMinHeight(height);
        setMaxWidth(width);
        setMinWidth(width);

        preview = new ImageView();
        preview.setPreserveRatio(true);

        empty = List.of(getBlackLogo());
        pages = empty;

        setPageFactory(index -> {
            var page = pages.get(index);
            preview.setImage(page);

            if (page.getHeight() > page.getWidth()) {
                preview.setFitWidth(0.0);
                preview.setFitHeight(height - 2.0);
            } else {
                preview.setFitWidth(width - 2.0);
                preview.setFitHeight(0.0);
            }

            return preview;
        });

        var view = new ImageView();
        view.imageProperty()
                .bind(preview.imageProperty());

        var tooltip = new Tooltip();
        tooltip.setContentDisplay(GRAPHIC_ONLY);
        tooltip.setGraphic(new StackPane(view));
        tooltip.setAutoFix(true);
        tooltip.setAutoHide(true);

        tooltip.addEventFilter(MOUSE_PRESSED, event -> tooltip.hide());
        preview.addEventFilter(MOUSE_PRESSED, event -> {
            var bounds = getPrimary().getBounds();

            tooltip.setMinHeight(bounds.getMaxY());
            tooltip.setY(bounds.getMinY());

            tooltip.setMinWidth(bounds.getMaxX());
            tooltip.setX(bounds.getMinX());

            tooltip.show(getMainStage());
        });
    }

    public void setImages(List<Image> images) {
        pages = (images == null || images.isEmpty()) ? empty : images;

        preview.setImage(pages.get(0));
        setPageCount(pages.size());
        setCurrentPageIndex(0);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        var skin = new PaginationSkin(this);

        ((StackPane) skin.getChildren().get(2))
                .getChildren().remove(1); // remove "page-information"

        return skin;
    }
}
