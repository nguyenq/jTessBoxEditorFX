/**
 * Copyright @ 2016 Quan Nguyen
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.sourceforge.tessboxeditor;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class MainController implements Initializable {

    @FXML
    private MenuBar menuBar;
    @FXML
    private TabPane tabPane;
    @FXML
    private ImageGeneratorController tabGeneratorController;
    @FXML
    private TrainerController tabTrainerController;
    @FXML
    private BoxEditorController tabBoxEditorController;
    @FXML
    private MainMenuController menuBarController;

    private static MainController instance;

    public static final String TO_BE_IMPLEMENTED = "To be implemented in subclass";
    public static final boolean MAC_OS_X = System.getProperty("os.name").startsWith("Mac");
    public static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
    public static final boolean LINUX = System.getProperty("os.name").toLowerCase().contains("nux");

    protected ResourceBundle bundle;
    static final Preferences prefs = Preferences.userRoot().node("/net/sourceforge/tessboxeditorfx");
    protected File baseDir;

    private final static Logger logger = Logger.getLogger(MainController.class.getName());

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        instance = this;
        //this.baseDir = Utils.getBaseDir(this);
        bundle = ResourceBundle.getBundle("net.sourceforge.tessboxeditor.Gui"); // NOI18N

        tabPane.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Tab> ov, Tab oldTab, Tab newTab) -> {
            int selectedIndex = tabPane.getSelectionModel().getSelectedIndex();
            menuBarController.configureMenus(selectedIndex);
        });

        menuBarController.configureMenus(0);
        tabGeneratorController.setMenuBar(menuBar);

//        // DnD support
//        new DropTarget(this.jSplitPaneEditor, new FileDropTargetListener(Gui.this, this.jSplitPaneEditor));
//        setSize(
//                snap(prefs.getInt("frameWidth", 500), 300, screen.width),
//                snap(prefs.getInt("frameHeight", 360), 150, screen.height));
//        setLocation(
//                snap(
//                        prefs.getInt("frameX", (screen.width - getWidth()) / 2),
//                        screen.x, screen.x + screen.width - getWidth()),
//                snap(
//                        prefs.getInt("frameY", screen.y + (screen.height - getHeight()) / 3),
//                        screen.y, screen.y + screen.height - getHeight()));
//
//        KeyEventDispatcher dispatcher = new KeyEventDispatcher() {
//
//            @Override
//            public boolean dispatchKeyEvent(KeyEvent e) {
//                if (e.getID() == KeyEvent.KEY_PRESSED) {
//                    if (e.getKeyCode() == KeyEvent.VK_F3) {
//                        jButtonFind.doClick();
//                    }
//                }
//                return false;
//            }
//        };
//        DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);
    }

    /**
     * Gets MainController instance (for child controllers).
     *
     * @return
     */
    public static MainController getInstance() {
        return instance;
    }

    BoxEditorController getBoxEditorController() {
        return tabBoxEditorController;
    }

    public void openFile(File selectedFile) {
        this.tabBoxEditorController.openFile(selectedFile);
        this.menuBarController.menuRecentFilesController.updateMRUList(selectedFile.getPath());
    }

    public void saveFile(File selectedFile) {
//        this.tabBoxEditorController.saveBoxFile(selectedFile);
    }

    public void setFont(Font font) {
        this.tabBoxEditorController.setFont(font);
        this.tabTrainerController.setFont(font);
    }

    boolean quit() {
        if (!MainController.getInstance().getBoxEditorController().promptToSave()) {
            return false;
        }

        this.tabBoxEditorController.savePrefs();
        this.tabGeneratorController.savePrefs();
        this.tabTrainerController.savePrefs();
        this.menuBarController.savePrefs();

        Stage stage = (Stage) menuBar.getScene().getWindow();
        prefs.putBoolean("windowState", stage.isMaximized());

        if (!stage.isMaximized()) {
            prefs.putDouble("frameHeight", stage.getHeight());
            prefs.putDouble("frameWidth", stage.getWidth());
            prefs.putDouble("frameX", stage.getX());
            prefs.putDouble("frameY", stage.getY());
        }

        return true;
    }

    void setStageState(Stage stage) {
        stage.setOnShowing(we -> {
            boolean maximized = prefs.getBoolean("windowState", false);
            stage.setMaximized(maximized);
            if (!maximized) {
                stage.setX(prefs.getDouble("frameX", 0));
                stage.setY(prefs.getDouble("frameY", 0));
                stage.setWidth(prefs.getDouble("frameWidth", 800));
                stage.setHeight(prefs.getDouble("frameHeight", 600));
            }

            this.tabBoxEditorController.setMenuBar(menuBar);
            setFont(this.menuBarController.getFont());
        });

        stage.setOnCloseRequest(we -> {
            if (!quit()) {
                we.consume();
            } else {
                Platform.exit();
                System.exit(0);
            }
        });
    }

    private int snap(final int ideal, final int min, final int max) {
        final int TOLERANCE = 0;
        return ideal < min + TOLERANCE ? min : (ideal > max - TOLERANCE ? max : ideal);
    }

}
