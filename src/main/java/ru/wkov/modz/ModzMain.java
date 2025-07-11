package ru.wkov.modz;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import ru.wkov.modz.layout.ModzRoot;

import java.net.URL;

import static javafx.scene.input.KeyCombination.NO_MATCH;
import static javafx.stage.StageStyle.UNDECORATED;
import static javafx.stage.WindowEvent.WINDOW_SHOWN;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzMain extends Application {

    static final Logger logger = getLogger(ModzMain.class);

    static Stage mainStage;
    static Color mainColor;
    static Color darkColor;
    static Color failColor;
    static Image aboutLogo;
    static Image blackIcon;
    static Image colorIcon;

    public static void main(String[] args) {
        try {
            loadFonts(
                    "Liberation_Mono.ttf",
                    "Liberation_Mono_Bold.ttf",
                    "Liberation_Mono_Bold_Italic.ttf",
                    "Liberation_Mono_Italic.ttf"
            );

            launch(args);
        } catch (Exception ex) {
            logger.error("crashed due to unexpected error", ex);
        }
    }

    public static URL getResource(String name) {
        if (!name.startsWith("/")) name = "/" + name;
        return ModzMain.class.getResource(name);
    }

    private static void loadFonts(String... names) throws Exception {
        for (var name : names) {
            try (var stream = getResource("fonts/" + name).openStream()) {
                Font.loadFont(stream, 13);
            }
        }
    }

    @Override
    public void start(Stage stage) {
        mainStage = stage;
        mainColor = Color.web("#146478");
        darkColor = Color.web("#0D404C");
        failColor = Color.web("#E82828");
        aboutLogo = new Image("about.png", true);
        blackIcon = new Image("black.png", false);
        colorIcon = new Image("color.png", false);

        var scene = new Scene(new ModzRoot(), 800.0, 600.0);
        scene.getStylesheets().add("style.css");

        stage.addEventFilter(WINDOW_SHOWN, event -> {
            stage.setMinHeight(stage.getHeight());
            stage.setMinWidth(stage.getWidth());
        });

        stage.setFullScreenExitKeyCombination(NO_MATCH);
        stage.getIcons().add(colorIcon);

        stage.initStyle(UNDECORATED);
        stage.setTitle("modz");
        stage.setScene(scene);
        stage.show();

        logger.info("started on Java [{}], JavaFX [{}]",
                System.getProperty("java.version"),
                System.getProperty("javafx.version"));
    }
}
