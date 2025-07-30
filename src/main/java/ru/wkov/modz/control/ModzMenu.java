package ru.wkov.modz.control;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import ru.wkov.modz.ModzBean;

import static ru.wkov.modz.ModzUtil.prop;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzMenu extends Accordion implements ModzBean {

    private final IntegerProperty detailed;

    private final ModzMore desc;

    private final ModzMore maps;

    private final ModzMore reqs;

    private final ModzMore used;

    public ModzMenu() {
        addStyleClasses("modz-menu");

        detailed = new SimpleIntegerProperty();
        expandedPaneProperty().addListener(prop(pane ->
                detailed.set(getPanes().indexOf(pane))));

        desc = new ModzMore();
        desc.setTitles(new Label("Details:"));

        maps = new ModzMore();
        maps.setTitles(new Label("Mapping:"));

        reqs = new ModzMore();
        reqs.setTitles(new Label("Require:"));

        used = new ModzMore();
        used.setTitles(new Label("Used By:"));

        getPanes().addAll(desc, maps, reqs, used);
        setExpandedPane(desc);
    }

    public ReadOnlyIntegerProperty detailedProperty() {
        return detailed;
    }

    public void setDetailed(int detailed) {
        setExpandedPane(detailed == -1 ? null : getPanes().get(detailed));
    }

    public void setDesc(Node desc) {
        this.desc.setContent(desc);
    }

    public void setMaps(Node maps) {
        this.maps.setContent(maps);
        this.maps.visibleProperty()
                .bind(maps.disableProperty().map(disable -> !disable));
    }

    public void setReqs(Node reqs) {
        this.reqs.setContent(reqs);
        this.reqs.visibleProperty()
                .bind(reqs.disableProperty().map(disable -> !disable));
    }

    public void setUsed(Node used) {
        this.used.setContent(used);
        this.used.visibleProperty()
                .bind(used.disableProperty().map(disable -> !disable));
    }
}
