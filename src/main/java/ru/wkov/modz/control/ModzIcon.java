package ru.wkov.modz.control;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import ru.wkov.modz.ModzBean;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.kordamp.ikonli.materialdesign2.MaterialDesignC.CLOSE_OUTLINE;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzIcon extends FontIcon implements ModzBean {

    protected Ikon[] codes;

    protected int state;

    public ModzIcon(Ikon... codes) {
        addStyleClasses("modz-icon");
        setCodes(codes);
    }

    public int setCodes(Ikon... codes) {
        this.codes = codes;
        return refresh();
    }

    public int setState(boolean state) {
        return setState(state ? 0 : 1);
    }

    public int setState(int state) {
        var max = codes.length - 1;
        var min = 0;

        if (state < min) state = max; else
        if (state > max) state = min;

        this.state = state;
        return refresh();
    }

    public int prevState() {
        return setState(state - 1);
    }

    public int nextState() {
        return setState(state + 1);
    }

    protected int refresh() {
        setIconCode(isEmpty(codes) ? CLOSE_OUTLINE : codes[state]);
        return state;
    }
}
