package ru.wkov.modz.control;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import ru.wkov.modz.ModzBean;

import java.util.List;

import static javafx.scene.paint.Color.*;
import static javafx.util.Duration.millis;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignA.ACCOUNT_MULTIPLE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignB.*;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignC.*;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignD.DRAMA_MASKS;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignE.EARTH_BOX;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignF.FOREST_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignG.GOOGLE_TRANSLATE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignH.HAMMER_SCREWDRIVER;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignL.LAMP_OUTLINE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignM.*;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignP.*;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignS.*;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignT.*;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignU.UPDATE;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignW.WIDGETS_OUTLINE;
import static ru.wkov.modz.ModzUtil.UNUSED;
import static ru.wkov.modz.ModzUtil.prop;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzType extends FontIcon implements ModzBean {

    private final BooleanProperty state;

    private final int order;

    public ModzType(int order, String name, int size, Ikon code, Color color) {
        this(order, name, size, code, color, BLACK);
    }

    public ModzType(int order, String name, int size, Ikon code, Color color, Color color2) {
        super(code);
        this.order = order;

        setId(name);
        setIconSize(size);
        addStyleClasses("modz-type");

        var tooltip = new Tooltip(name);
        tooltip.setShowDelay(millis(300));
        tooltip.getStyleClass().add("modz-type-hint");
        Tooltip.install(this, tooltip);

        var effect = new DropShadow();
        effect.setRadius(5.0);
        effect.setHeight(10.0);
        effect.setColor(color2);

        state = new SimpleBooleanProperty(false);
        state.addListener(prop(value -> {
            if (value) {
                pseudoClassStateChanged(UNUSED, false);
                setIconColor(color);
                setEffect(effect);
            } else {
                var parent = getParent();

                pseudoClassStateChanged(UNUSED, true);
                setIconColor(parent == null || !parent.isFocused() ? getDarkColor() : BLACK);
                setEffect(null);
            }
        }));

        parentProperty().addListener(new InvalidationListener() {

            @Override
            public void invalidated(Observable unused) {
                parentProperty().removeListener(this);
                getParent().focusedProperty().addListener(prop(false, focused -> {
                    if (!state.get()) {
                        setIconColor(focused ? BLACK : getDarkColor());
                    }
                }));
            }
        });

        setState(true);
    }

    public int getOrder() {
        return order;
    }

    public void setState(boolean state) {
        this.state.set(state);
    }

    public static List<ModzType> createAll(int size) {
        return List.of(
                new ModzType(0, "Animals", size, PAW, BISQUE),
                new ModzType(1, "Audio", size, MUSIC, BLACK, WHITE),
                new ModzType(2, "Balance", size, SCALE_BALANCE, DARKKHAKI),
                new ModzType(3, "Building", size, HAMMER_SCREWDRIVER, ROSYBROWN),
                new ModzType(4, "Clothing/Armor", size, TSHIRT_CREW_OUTLINE, CORAL),
                new ModzType(5, "Farming", size, BARLEY, DARKORANGE.brighter()),
                new ModzType(6, "Food", size, SILVERWARE_FORK_KNIFE, SILVER.darker()),
                new ModzType(7, "Framework", size, COG_OUTLINE, SANDYBROWN.darker()),
                new ModzType(8, "Hardmode", size, SKULL_CROSSBONES, DARKRED, GAINSBORO),
                new ModzType(9, "Interface", size, TELEVISION_GUIDE, ROSYBROWN),
                new ModzType(10, "Items", size, LAMP_OUTLINE, MEDIUMSEAGREEN),
                new ModzType(11, "Language/Translation", size, GOOGLE_TRANSLATE, DODGERBLUE),
                new ModzType(12, "Literature", size, BOOK_OPEN_VARIANT_OUTLINE, SALMON),
                new ModzType(13, "Map", size, MAP_MARKER_MULTIPLE_OUTLINE, SEAGREEN),
                new ModzType(14, "Military", size, PARACHUTE_OUTLINE, CADETBLUE),
                new ModzType(15, "Misc", size, WIDGETS_OUTLINE, LIGHTSTEELBLUE),
                new ModzType(16, "Models", size, PACKAGE_VARIANT, GREENYELLOW.darker()),
                new ModzType(17, "Multiplayer", size, ACCOUNT_MULTIPLE, DARKVIOLET.darker(), GAINSBORO),
                new ModzType(18, "Pop Culture", size, STAR_BOX_OUTLINE, YELLOW),
                new ModzType(19, "QoL", size, SEAL_VARIANT, GOLDENROD),
                new ModzType(20, "Realistic", size, CAMPFIRE, ORANGERED),
                new ModzType(21, "Silly/Fun", size, BALLOON, HOTPINK),
                new ModzType(22, "Skills", size, SCHOOL_OUTLINE, STEELBLUE),
                new ModzType(23, "Textures", size, FOREST_OUTLINE, FORESTGREEN),
                new ModzType(24, "Traits", size, DRAMA_MASKS, WHITE),
                new ModzType(25, "Vehicles", size, CAR_MULTIPLE, DEEPSKYBLUE),
                new ModzType(26, "Weapons", size, TARGET, ORANGERED),
                new ModzType(27, "WIP", size, UPDATE, GRAY),
                new ModzType(28, "Mapz", size, MAP_OUTLINE, GREEN, DARKBLUE.darker())
        );
    }
}
