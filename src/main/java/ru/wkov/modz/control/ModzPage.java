package ru.wkov.modz.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Pagination;
import javafx.scene.control.Skin;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.PaginationSkin;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import ru.wkov.modz.ModzBean;

import java.util.List;

import static java.util.Collections.emptyList;
import static javafx.scene.control.ContentDisplay.GRAPHIC_ONLY;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.stage.Screen.getPrimary;
import static ru.wkov.modz.ModzUtil.prop;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzPage extends Pagination implements ModzBean {

    private final Callback<Integer, Node> paging;

    private final BooleanProperty expanded;

    private final ImageView preview;

    private List<? extends String> urls;

    public ModzPage(double width, double height) {
        addStyleClasses("modz-page");

        setMaxHeight(height);
        setMinHeight(height);
        setMaxWidth(width);
        setMinWidth(width);

        preview = new ImageView();
        preview.setPreserveRatio(true);

        urls = emptyList();

        expanded = new SimpleBooleanProperty(false);

        paging = index -> {
            var img = getBlackLogo();

            var size = expanded.get() ? urls.size() : 0;
            if (size > 0) {
                img = new Image(urls.get(index), true);
            }

            setPageCount(Math.max(size, 1));
            preview.setImage(img);

            if (img.getHeight() > img.getWidth()) {
                preview.setFitWidth(0.0);
                preview.setFitHeight(height - 2.0);
            } else {
                preview.setFitWidth(width - 2.0);
                preview.setFitHeight(0.0);
            }

            return preview;
        };

        expanded.addListener(prop(() -> paging.call(0)));

        setPageFactory(paging);

        var view = new ImageView();
        view.setPreserveRatio(true);

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

        preview.imageProperty().addListener(prop(false, img -> {
            view.setImage(img);

            var bounds = getPrimary().getBounds();

            if (img.getHeight() < img.getWidth()) {
                view.setFitWidth(bounds.getMaxX() - 2.0);
                view.setFitHeight(0.0);
            } else {
                view.setFitWidth(0.0);
                view.setFitHeight(bounds.getMaxY() - 2.0);
            }
        }));
    }

    public void setExpanded(boolean expanded) {
        this.expanded.set(expanded);
    }

    public void setUrls(List<? extends String> urls) {
        this.urls = urls;

        paging.call(0);
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
