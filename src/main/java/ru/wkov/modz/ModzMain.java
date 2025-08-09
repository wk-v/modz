package ru.wkov.modz;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import ru.wkov.modz.layout.ModzRoot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static java.lang.Integer.max;
import static java.util.logging.Level.*;
import static java.util.stream.Collectors.toMap;
import static javafx.scene.input.KeyCombination.NO_MATCH;
import static javafx.stage.StageStyle.UNDECORATED;
import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.commons.lang3.math.NumberUtils.min;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzMain extends Application {

    static final Logger logger;

    static {
//        TimeZone.setDefault(TimeZone.getTimeZone(UTC));
        Locale.setDefault(Locale.ENGLISH);

        try (var stream = ModzMain.class.getResourceAsStream("/logger.cfg")) {
            LogManager.getLogManager().readConfiguration(stream);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        logger = Logger.getLogger("");
    }

    static String webApiKey;
    static Stage mainStage;
    static Color mainColor;
    static Color darkColor;
    static Image aboutLogo;
    static Image blackIcon;
    static Image colorIcon;
    static Path rootPath;
    static int tileSize;

    public static void main(String[] args) {
        try {
            var props = Map.<String, String>of();
            if (args.length > 0) {
                props = Arrays.stream(args)
                        .map(arg -> split(arg, '='))
                        .filter(arg -> arg.length == 2 && isNotBlank(arg[1]))
                        .collect(toMap(arg -> arg[0], arg -> arg[1]));
            }

            webApiKey = props.get("webApiKey");
            if (isBlank(webApiKey)) {
                logger.log(WARNING, "WebAPI key is not set. Mod details is not available.");
                webApiKey = null;
            }

            tileSize = Integer.parseInt(props.getOrDefault("tileSize", "200"));
            tileSize = min(max(tileSize, 100), 248);

            launch();
        } catch (Exception error) {
            logger.log(SEVERE, "Application crashed due to unexpected error:", error);
        }
    }

    public void start(Stage stage) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
            logger.log(SEVERE, "Operation failed due to unexpected error:", error));

        mainStage = stage;
        mainColor = Color.web("#146478");
        darkColor = Color.web("#0D404C");
        aboutLogo = new Image("about.png", true);
        blackIcon = new Image("black.png", false);
        colorIcon = new Image("color.png", false);
        rootPath = Path.of("modz");

        var scene = new Scene(new ModzRoot(), 1000, 800);
        scene.getStylesheets().add("style.css");

        var bounds = Screen.getPrimary().getVisualBounds();

        stage.setMaxHeight(bounds.getHeight());
        stage.setMaxWidth(bounds.getWidth());

        stage.setMinHeight(250);
        stage.setMinWidth(250);

        stage.setFullScreenExitKeyCombination(NO_MATCH);
        stage.getIcons().add(colorIcon);

        stage.initStyle(UNDECORATED);
        stage.setTitle("modz");
        stage.setScene(scene);
        stage.show();

        logger.log(INFO, """
                        Application started on Java [{0}], JavaFX [{1}]
                        
                          `7MMM.     ,MMF′ .g8""8q. `7MM""\"Yb.   MOD""\"MOD
                            MMMb    dPMM .dP′    `YM. MM    `Yb. M′   MOD
                            M YM   ,M MM dM′      `MM MM     `Mb ′   MOD
                            M  Mb  M′ MM MM        MM MM      MM    MOD
                            M  YM.P′  MM MM.      ,MP MM     ,MP   MOD   ,
                            M  `YM′   MM `Mb.    ,dP′ MM    ,dP′  MOD   ,M
                          .JML. `′  .JMML. `"bmmd"′ .JMMmmmdP′   MOD 2.0.1
                        """,
                new Object[]{System.getProperty("java.version"), System.getProperty("javafx.version")});
    }
}
