/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 16/ott/2013
 * Copyright 2013 by Andrea Vacondio (andrea.vacondio@gmail.com).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pdfsam;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.pdfsam.ui.event.SetActiveModuleRequest.activeteModule;
import static org.sejda.eventstudio.StaticStudio.eventStudio;

import java.awt.SplashScreen;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.pdfsam.configuration.ApplicationContextHolder;
import org.pdfsam.configuration.StylesConfig;
import org.pdfsam.context.DefaultUserContext;
import org.pdfsam.context.UserContext;
import org.pdfsam.i18n.DefaultI18nContext;
import org.pdfsam.i18n.SetLocaleEvent;
import org.pdfsam.ui.MainPane;
import org.pdfsam.ui.commons.OpenUrlRequest;
import org.pdfsam.ui.commons.ShowStageRequest;
import org.pdfsam.ui.dialog.OverwriteConfirmationDialog;
import org.pdfsam.ui.io.SetLatestDirectoryEvent;
import org.pdfsam.ui.notification.NotificationsContainer;
import org.pdfsam.update.UpdateCheckRequest;
import org.sejda.core.Sejda;
import org.sejda.eventstudio.EventStudio;
import org.sejda.eventstudio.annotation.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * PDFsam application
 * 
 * @author Andrea Vacondio
 * 
 */
public class PdfsamApp extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(PdfsamApp.class);
    private static StopWatch STOPWATCH = new StopWatch();
    public static String SPLASH_STATION = "SplashStation";
    private Scene mainScene;

    @Override
    public void init() {
        STOPWATCH.start();
        UserContext userContext = new DefaultUserContext();
        System.setProperty(EventStudio.MAX_QUEUE_SIZE_PROP, Integer.toString(userContext.getNumberOfLogRows()));
        LOG.info("Starting PDFsam");
        System.setProperty(Sejda.UNETHICAL_READ_PROPERTY_NAME, "true");
        cleanUserContextIfNeeded(userContext);
        String localeString = userContext.getLocale();
        if (isNotBlank(localeString)) {
            eventStudio().broadcast(new SetLocaleEvent(localeString));
        }
        String defaultworkingPath = userContext.getDefaultWorkingPath();
        if (isNotBlank(defaultworkingPath)) {
            try {
                if (Files.isDirectory(Paths.get(defaultworkingPath))) {
                    eventStudio().broadcast(new SetLatestDirectoryEvent(new File(defaultworkingPath)));
                }
            } catch (InvalidPathException e) {
                LOG.warn("Unable to set initial directory, default path is invalid.", e);
            }
        }
    }

    private void cleanUserContextIfNeeded(UserContext userContext) {
        if (getParameters().getRaw().contains("-clean")) {
            userContext.clear();
            LOG.info("Cleared user preferences");
        }
    }

    @Override
    public void start(Stage primaryStage) {
        ApplicationContextHolder.getContext();
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionLogger());
        initScene();
        primaryStage.setScene(mainScene);
        primaryStage.getIcons().addAll(ApplicationContextHolder.getContext().getBeansOfType(Image.class).values());
        primaryStage.setTitle(ApplicationContextHolder.getContext().getBean(Pdfsam.class).name());
        initWindowsStatusController(primaryStage);
        initOverwriteDialogController(primaryStage);
        initActiveModule();
        primaryStage.show();

        requestCheckForUpdateIfNecessary();
        requestLatestNewsPanelDisplay();
        eventStudio().addAnnotatedListeners(this);
        closeSplash();
        STOPWATCH.stop();
        LOG.info(DefaultI18nContext.getInstance().i18n("Started in {0}",
                DurationFormatUtils.formatDurationWords(STOPWATCH.getTime(), true, true)));
    }

    private void closeSplash() {
        Optional.ofNullable(SplashScreen.getSplashScreen()).ifPresent(SplashScreen::close);
    }

    private void initScene() {
        MainPane mainPane = ApplicationContextHolder.getContext().getBean(MainPane.class);

        NotificationsContainer notifications = ApplicationContextHolder.getContext()
                .getBean(NotificationsContainer.class);
        StackPane main = new StackPane();
        StackPane.setAlignment(notifications, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(mainPane, Pos.TOP_LEFT);
        main.getChildren().addAll(mainPane, notifications);

        StylesConfig styles = ApplicationContextHolder.getContext().getBean(StylesConfig.class);

        mainScene = new Scene(main);
        mainScene.getStylesheets().addAll(styles.styles());
        mainScene.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN),
                () -> eventStudio().broadcast(new ShowStageRequest(), "LogStage"));
    }

    @Override
    public void stop() {
        LOG.info(DefaultI18nContext.getInstance().i18n("Closing PDFsam..."));
        ApplicationContextHolder.getContext().close();
    }

    private static void requestCheckForUpdateIfNecessary() {
        if (ApplicationContextHolder.getContext().getBean(UserContext.class).isCheckForUpdates()) {
            eventStudio().broadcast(new UpdateCheckRequest());
        }
    }

    private static void requestLatestNewsPanelDisplay() {
        eventStudio().broadcast(new ShowStageRequest(), "NewsStage");
    }

    @EventListener
    public void openUrl(OpenUrlRequest event) {
        HostServices services = getHostServices();
        if (services != null) {
            services.showDocument(event.getUrl());
        } else {
            LOG.warn("Unable to open '{}', please copy and paste the url to your browser.", event.getUrl());
        }
    }

    private void initOverwriteDialogController(Stage primaryStage) {
        OverwriteConfirmationDialog overwriteDialog = ApplicationContextHolder.getContext()
                .getBean(OverwriteConfirmationDialog.class);
        overwriteDialog.setOwner(primaryStage);
    }

    private void initWindowsStatusController(Stage primaryStage) {
        WindowStatusController stageStatusController = ApplicationContextHolder.getContext()
                .getBean(WindowStatusController.class);
        stageStatusController.setStage(primaryStage);
    }

    private void initActiveModule() {
        String startupModule = new DefaultUserContext().getStartupModule();
        if (isNotBlank(startupModule)) {
            LOG.trace("Activating startup module '{}'", startupModule);
            eventStudio().broadcast(activeteModule(startupModule));
        }
    }
}
