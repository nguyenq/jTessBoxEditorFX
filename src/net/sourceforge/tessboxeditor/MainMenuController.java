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

import java.awt.Desktop;
import java.awt.HeadlessException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.beans.value.*;

import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.controlsfx.dialog.FontSelectorDialog;

import org.w3c.dom.*;
import org.w3c.dom.events.*;

public class MainMenuController implements Initializable {

    @FXML
    private MenuBar menuBar;
    @FXML
    private Menu menuEdit;
    @FXML
    private Menu menuRecentFiles;
    @FXML
    private MenuItem miOpen;
    @FXML
    private MenuItem miSave;
    @FXML
    private MenuItem miSaveAs;
    @FXML
    protected MenuRecentFilesController menuRecentFilesController;
    @FXML
    private MenuItem separatorRecentFiles;
    @FXML
    private MenuItem separatorExit;
    @FXML
    protected MenuToolsController menuToolsController;
    @FXML
    private MenuItem miExit;
    @FXML
    private MenuItem miMerge;
    @FXML
    private MenuItem miSplit;
    @FXML
    private MenuItem miInsert;
    @FXML
    private MenuItem miDelete;
    @FXML
    private Menu menuSettings;
    @FXML
    private MenuItem miFont;
    @FXML
    private MenuItem miHelp;
    @FXML
    private MenuItem miAbout;

    Stage helpDialog;
    private Font font;
    final Preferences prefs = MainController.prefs;
    private final static Logger logger = Logger.getLogger(MainMenuController.class.getName());

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        menuToolsController.setMenuBar(menuBar);

        String style = prefs.get("programfontStyle", "");
        font = Font.font(
                prefs.get("programfontName", Font.getDefault().getFamily()),
                style.contains("Bold") ? FontWeight.BOLD : FontWeight.NORMAL,
                style.contains("Italic") ? FontPosture.ITALIC : FontPosture.REGULAR,
                prefs.getDouble("programfontSize", Font.getDefault().getSize()));
    }
    
    Font getFont() {
        return font;
    }

    /**
     * Hides or shows menus and menu items based on current tab selection.
     *
     * @param tabIndex
     */
    void configureMenus(int tabIndex) {
        boolean boxEditorActive = tabIndex == 1;
        this.menuEdit.setVisible(boxEditorActive);
        this.menuSettings.setVisible(boxEditorActive || tabIndex == 0);
        this.miFont.setVisible(boxEditorActive || tabIndex == 0);
        this.miOpen.setVisible(boxEditorActive);
        this.miSave.setVisible(boxEditorActive);
        this.miSaveAs.setVisible(boxEditorActive);
        this.menuRecentFiles.setVisible(boxEditorActive);
        this.separatorRecentFiles.setVisible(boxEditorActive);
        this.separatorExit.setVisible(boxEditorActive);
    }

    @FXML
    private void handleAction(ActionEvent event) {
        if (event.getSource() == miOpen) {
            ((Button) menuBar.getScene().lookup("#btnOpen")).fire();
        } else if (event.getSource() == miSave) {
            ((Button) menuBar.getScene().lookup("#btnSave")).fire();
        } else if (event.getSource() == miSaveAs) {
            MainController.getInstance().getBoxEditorController().saveFileDlg();
        } else if (event.getSource() == miExit) {
            Stage stage = (Stage) menuBar.getScene().getWindow();
            stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
        } else if (event.getSource() == miMerge) {
            ((Button) menuBar.getScene().lookup("#btnMerge")).fire();
        } else if (event.getSource() == miSplit) {
            ((Button) menuBar.getScene().lookup("#btnSplit")).fire();
        } else if (event.getSource() == miInsert) {
            ((Button) menuBar.getScene().lookup("#btnInsert")).fire();
        } else if (event.getSource() == miDelete) {
            ((Button) menuBar.getScene().lookup("#btnDelete")).fire();
        } else if (event.getSource() == miFont) {
            FontSelectorDialog dialog = new FontSelectorDialog(font);
            Optional<Font> op = dialog.showAndWait();
            if (op.isPresent()) {
                font = op.get();
                MainController.getInstance().setFont(font);
            }
        } else if (event.getSource() == miHelp) {
            if (helpDialog == null) {
                Label urlLabel = new Label();
                urlLabel.setTranslateX(5);
                WebView webView = new WebView();

                URL url = getClass().getResource("/readme.html");
                WebEngine webEngine = webView.getEngine();
                webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
                    public void changed(ObservableValue<? extends State> observable, State oldState, State newState) {
                        if (newState == Worker.State.SUCCEEDED) {
                            EventListener listener = (Event ev) -> {
                                String domEventType = ev.getType();
                                if (domEventType.equals(EVENT_TYPE_CLICK)) {
                                    String href = ((Element) ev.getTarget()).getAttribute("href");
                                    try {
                                        linkActivated(new URL(href));
                                    } catch (Exception e) {

                                    }
                                    ev.preventDefault(); // prevent loading into webview; launch external browser only
                                } else if (domEventType.equals(EVENT_TYPE_MOUSEOVER)) {
                                    String href = ((Element) ev.getTarget()).getAttribute("href");
                                    urlLabel.setText(href);
                                } else if (domEventType.equals(EVENT_TYPE_MOUSEOUT)) {
                                    urlLabel.setText(null);
                                }
                            };

                            Document doc = webEngine.getDocument();
                            NodeList nodeList = doc.getElementsByTagName("a");
                            for (int i = 0; i < nodeList.getLength(); i++) {
                                Node el = nodeList.item(i);
                                ((EventTarget) el).addEventListener(EVENT_TYPE_CLICK, listener, false);
                                ((EventTarget) el).addEventListener(EVENT_TYPE_MOUSEOVER, listener, false);
                                ((EventTarget) el).addEventListener(EVENT_TYPE_MOUSEOUT, listener, false);
                            }
                        }
                    }
                });

                webEngine.load(url.toExternalForm());
                helpDialog = new Stage();
                helpDialog.initModality(Modality.WINDOW_MODAL);
                helpDialog.setTitle(JTessBoxEditor.APP_NAME + " Help");
                VBox vbox = new VBox();
                vbox.setSpacing(5);
                vbox.getChildren().addAll(webView, urlLabel);
                VBox.setVgrow(webView, Priority.ALWAYS);
                helpDialog.setScene(new Scene(vbox));
            }
            helpDialog.setIconified(false);
            helpDialog.show();
        } else if (event.getSource() == miAbout) {
            try {
                Properties config = new Properties();
                config.loadFromXML(getClass().getResourceAsStream("config.xml"));
                String version = config.getProperty("Version");
                LocalDate releaseDate = LocalDate.parse(config.getProperty("ReleaseDate"));
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
                String msg = JTessBoxEditor.APP_NAME + " " + version + " \u00a9 2016\n"
                        + "Tesseract Box Editor & Trainer\n"
                        + releaseDate.format(formatter)
                        + "\nhttp://vietocr.sourceforge.net";
                Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
                alert.setTitle("About " + JTessBoxEditor.APP_NAME);
                alert.setHeaderText(null);
                alert.showAndWait();
            } catch (IOException | HeadlessException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    /**
     * Follows the reference in an link. The given url is the requested
     * reference. By default this calls <a href="#setPage">setPage</a>, and if
     * an exception is thrown the original previous document is restored and a
     * beep sounded. If an attempt was made to follow a link, but it represented
     * a malformed url, this method will be called with a null argument.
     *
     * @param url the URL to follow
     */
    protected void linkActivated(URL url) {
        try {
            if (url.toString().startsWith("jar:")) {
//                html.setPage(url);
            } else {
                Desktop.getDesktop().browse(url.toURI());
            }
        } catch (IOException | URISyntaxException e) {
            System.err.println("Error message: " + e.getMessage());
        }
    }

    public static final String EVENT_TYPE_CLICK = "click";
    public static final String EVENT_TYPE_MOUSEOVER = "mouseover";
    public static final String EVENT_TYPE_MOUSEOUT = "mouseout";

    public void savePrefs() {
        menuRecentFilesController.savePrefs();

        if (font != null) {
            prefs.put("programfontName", font.getName());
            prefs.putDouble("programfontSize", font.getSize());
            prefs.put("programfontStyle", font.getStyle());
        }
    }
}
