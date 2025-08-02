package ru.wkov.modz.control;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import ru.wkov.modz.ModzBean;

import static ru.wkov.modz.ModzUtil.prop;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzMenu extends Accordion implements ModzBean {

    private final StringProperty detailed;

    private final ModzMore desc;

    private final ModzMore maps;

    private final ModzMore reqs;

    private final ModzMore used;

    public ModzMenu() {
        addStyleClasses("modz-menu");

        desc = new ModzMore();
        desc.setId("details");
        desc.setTitles(new Label("Details:"));

        maps = new ModzMore();
        maps.setId("mapping");
        maps.setTitles(new Label("Mapping:"));

        reqs = new ModzMore();
        reqs.setId("require");
        reqs.setTitles(new Label("Require:"));

        used = new ModzMore();
        used.setId("used_by");
        used.setTitles(new Label("Used By:"));

        getPanes().addAll(desc, maps, reqs, used);
        setExpandedPane(desc);

        detailed = new SimpleStringProperty();
        expandedPaneProperty()
                .addListener(prop(pane -> detailed.set(pane == null ? desc.getId() : pane.getId())));
    }

    public ReadOnlyStringProperty detailedProperty() {
        return detailed;
    }

    public void setDetailed(String detailed) {
        if (detailed != null) {
            for (var pane : getPanes()) {
                if (detailed.equalsIgnoreCase(pane.getId())) {
                    setExpandedPane(pane);
                    return;
                }
            }
        }
        setExpandedPane(desc);
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
