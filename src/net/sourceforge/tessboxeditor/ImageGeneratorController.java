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
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.StringConverter;

import org.controlsfx.dialog.FontSelectorDialog;
import net.sourceforge.tessboxeditor.utilities.Utils;

public class ImageGeneratorController implements Initializable {

    @FXML
    private CheckBox chbText2Image;
    @FXML
    private Button btnInput;
    @FXML
    private Button btnBrowseOutput;
    @FXML
    private Button btnFont;
    @FXML
    private Button btnGenerate;
    @FXML
    private Button btnClear;
    @FXML
    private Button btnBrowseFontFolder;
    @FXML
    private HBox hbFontAttrib;
    @FXML
    private HBox hbFontFolder;
    @FXML
    private TextField tfOutputDir;
    @FXML
    private TextField tfPrefix;
    @FXML
    private TextField tfFileName;
    @FXML
    private TextField tfFontFolder;
    @FXML
    private Spinner<Integer> spnNoise;
    @FXML
    private Spinner<Double> spnTracking;
    @FXML
    private Spinner<Integer> spnExposure;
    @FXML
    private Spinner<Integer> spnLeading;
    @FXML
    private Spinner<Integer> spnH;
    @FXML
    private Spinner<Integer> spnW;
    @FXML
    private CheckBox chbAntiAliasing;
    @FXML
    private TextArea taInput;
    @FXML
    private TextFlow textFlow;
    @FXML
    private TabPane tabPane;
    MenuBar menuBar;

    private final int margin = 100;
    private File inputTextFile;
    private String outputDirectory;
    private DirectoryChooser dcOutputDir;
    private String fontFolder;
    private DirectoryChooser dcFontFolder;
    private Font fontGen;
    final Preferences prefs = MainController.prefs;
    ObservableList<ExtensionFilter> fileFilters;
    int filterIndex;
    private FileChooser fcInputText;
    boolean textChanged;
    int pageNum;
    int startIndex;
    List<String> allText = new ArrayList<String>();
    List<List<String>> textPages = new ArrayList<List<String>>();
    BreakIterator breakIterator = BreakIterator.getCharacterInstance();

    private final static Logger logger = Logger.getLogger(ImageGeneratorController.class.getName());

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        String style = prefs.get("trainfontStyle", "");
        // Set fontGen
        fontGen = Font.font(
                prefs.get("trainfontName", this.taInput.getFont().getFamily()),
                style.contains("Bold") ? FontWeight.BOLD : FontWeight.NORMAL,
                style.contains("Italic") ? FontPosture.ITALIC : FontPosture.REGULAR,
                prefs.getDouble("trainfontSize", 12));
        this.taInput.setFont(Utils.deriveFont(fontGen, fontGen.getSize() * 3));
//        this.taInput.setStyle(String.format("-fx-font-size: %dpx;", (int) fontGen.getSize() * 3));
//        this.taInput.setStyle("-fx-line-spacing: 1em;");  // no effect!
        this.tfPrefix.setText(prefs.get("trainLanguage", "eng"));
        this.btnFont.setText(fontDesc(fontGen));
        this.tfFileName.setText(createFileName(fontGen) + ".exp0.tif");

        hbFontAttrib.managedProperty().bind(hbFontAttrib.visibleProperty());
        hbFontFolder.managedProperty().bind(hbFontFolder.visibleProperty());

        chbText2Image.setSelected(prefs.getBoolean("Text2Image", false));
        handleAction(new ActionEvent(chbText2Image, null));

        textFlow.setPadding(new Insets(margin));
        textFlow.setLineSpacing((int) this.spnLeading.getValue());

        outputDirectory = prefs.get("outputDirectory", new File(System.getProperty("user.dir"), "samples/vie").getPath());
        if (!Files.exists(Paths.get(outputDirectory))) {
            outputDirectory = System.getProperty("user.home");
        }
        tfOutputDir.setText(outputDirectory);
        tfOutputDir.setStyle("-fx-focus-color: transparent;");

        dcOutputDir = new DirectoryChooser();
        dcOutputDir.setTitle("Set Ouput Directory");
        dcOutputDir.setInitialDirectory(new File(outputDirectory));

        fcInputText = new FileChooser();
        fcInputText.setTitle("Open Input File");
        ExtensionFilter textFilter = new ExtensionFilter("Text Files", "*.txt");
        fcInputText.getExtensionFilters().add(textFilter);
        fcInputText.setInitialDirectory(new File(outputDirectory));

        fontFolder = prefs.get("fontFolder", getFontFolder());
        tfFontFolder.setText(fontFolder);
        tfFontFolder.setStyle("-fx-focus-color: transparent;");

        dcFontFolder = new DirectoryChooser();
        dcFontFolder.setTitle("Set Font Folder");
        dcFontFolder.setInitialDirectory(new File(fontFolder));

        this.spnExposure.valueProperty().addListener(new ChangeListener<Integer>() {
            @Override
            public void changed(ObservableValue observable, Integer oldValue, Integer newValue) {
                String imageFilename = tfFileName.getText().trim();
                tfFileName.setText(imageFilename.replaceFirst("exp.*?\\.tif$", "exp" + newValue + ".tif"));
            }
        });

        this.spnTracking.valueProperty().addListener(new ChangeListener<Double>() {
            @Override
            public void changed(ObservableValue observable, Double oldValue, Double newValue) {
                textChanged = true;
                Region region = (Region) taInput.lookup(".content");
//                region.setStyle("-fx-background-color: yellow");
                region.setStyle("-fx-letter-spacing: " + newValue); // no effect; not supported yet in JDK8u91
            }
        });

        this.spnLeading.valueProperty().addListener(new ChangeListener<Integer>() {
            @Override
            public void changed(ObservableValue observable, Integer oldValue, Integer newValue) {
                Text text = (Text) taInput.lookup(".text");
                text.setLineSpacing(newValue);
                textFlow.setLineSpacing(newValue);
            }
        });

        taInput.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                if (db.hasFiles()) {
                    File file = db.getFiles().get(0);
                    boolean isAccepted = file.getName().toLowerCase().endsWith(".txt");
                    if (isAccepted) {
                        event.acceptTransferModes(TransferMode.COPY);
                    } else {
                        event.consume();
                    }
                } else {
                    event.consume();
                }
            }
        });

        taInput.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                    success = true;
                    openTextFile(db.getFiles().get(0));
                }

                event.setDropCompleted(success);
                event.consume();
            }
        });

        tabPane.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Tab> ov, Tab oldTab, Tab newTab) -> {
            // if text change, relayout
            if (textChanged) {
                textChanged = false;
                layoutbox();
            }
        });

        taInput.lengthProperty().addListener((Observable ov) -> {
            textChanged = true;
//            layoutbox();
        });

        spnW.valueProperty().addListener((Observable ov) -> {
            layoutbox();
        });

        spnW.focusedProperty().addListener((s, ov, nv) -> {
            if (nv) {
                return;
            }
            commitEditorText(spnW);
        });

        spnH.focusedProperty().addListener((s, ov, nv) -> {
            if (nv) {
                return;
            }
            commitEditorText(spnH);
        });
    }

    void setMenuBar(MenuBar menuBar) {
        this.menuBar = menuBar;
    }

    /**
     * Gets default system font folder.
     *
     * @return
     */
    String getFontFolder() {
        String folder;
        if (MainController.WINDOWS) {
            folder = "C:\\Windows\\Fonts";
        } else if (MainController.MAC_OS_X) {
            folder = "/Library/Fonts/";
        } else {
            folder = "/usr/share/fonts"; // assume Linux
        }
        return folder;
    }

    /**
     * Code from Spinner implementation.
     */
    private <T> void commitEditorText(Spinner<T> spinner) {
        if (!spinner.isEditable()) {
            return;
        }
        String text = spinner.getEditor().getText();
        SpinnerValueFactory<T> valueFactory = spinner.getValueFactory();
        if (valueFactory != null) {
            StringConverter<T> converter = valueFactory.getConverter();
            if (converter != null) {
                T value = converter.fromString(text);
                valueFactory.setValue(value);
            }
        }
    }

    @FXML
    private void handleAction(ActionEvent event) {
        if (event.getSource() == chbText2Image) {
            boolean selected = this.chbText2Image.isSelected();
            this.hbFontAttrib.setVisible(!selected);
            this.hbFontAttrib.setPrefWidth(selected ? 0 : Control.USE_COMPUTED_SIZE);
            this.hbFontFolder.setVisible(selected);
            this.hbFontFolder.setPrefWidth(!selected ? 0 : Control.USE_COMPUTED_SIZE);
            this.taInput.setDisable(selected);
        } else if (event.getSource() == btnInput) {
            inputTextFile = fcInputText.showOpenDialog(null);
            if (inputTextFile != null) {
                fcInputText.setInitialDirectory(inputTextFile.getParentFile());
                openTextFile(inputTextFile);
            }
        } else if (event.getSource() == btnBrowseOutput) {
            File dir = dcOutputDir.showDialog(btnBrowseOutput.getScene().getWindow());
            if (dir != null) {
                outputDirectory = dir.getPath();
                tfOutputDir.setText(outputDirectory);
                dcOutputDir.setInitialDirectory(dir);
                fcInputText.setInitialDirectory(dir);
            }
        } else if (event.getSource() == btnBrowseFontFolder) {
            File dir = dcFontFolder.showDialog(btnBrowseFontFolder.getScene().getWindow());
            if (dir != null) {
                fontFolder = dir.getPath();
                tfFontFolder.setText(fontFolder);
            }
        } else if (event.getSource() == btnFont) {
            FontSelectorDialog dialog = new FontSelectorDialog(fontGen);
            Optional<Font> op = dialog.showAndWait();
            if (op.isPresent()) {
                fontGen = op.get();
                this.taInput.setFont(Utils.deriveFont(fontGen, fontGen.getSize() * 3));
//                this.taInput.setStyle(String.format("-fx-font-size: %dpx;", (int) fontGen.getSize() * 3));
                layoutbox();
                this.btnFont.setText(fontDesc(fontGen));
                String curFontName = this.tfFileName.getText();
                String ext = curFontName.substring(curFontName.lastIndexOf(".exp"));
                String newFontName = createFileName(fontGen) + ext;
                this.tfFileName.setText(newFontName);
            }
        } else if (event.getSource() == btnGenerate) {
            if (this.taInput.getText().trim().length() == 0) {
                Alert alert = new Alert(Alert.AlertType.NONE, "Please load training text.", ButtonType.OK);
                alert.setTitle(JTessBoxEditor.APP_NAME);
                alert.show();
                return;
            } else if (inputTextFile == null && chbText2Image.isSelected()) {
                Alert alert = new Alert(Alert.AlertType.NONE, "Please select an input file.", ButtonType.OK);
                alert.setTitle(JTessBoxEditor.APP_NAME);
                alert.show();
                return;
            }

            btnGenerate.setDisable(true);
            taInput.getScene().setCursor(Cursor.WAIT);

            generateTiffBox();
        } else if (event.getSource() == btnClear) {
            this.taInput.clear();
            this.textFlow.getChildren().clear();
        }
    }

    /**
     * Lays out boxes in TextFlow control. Each Text control corresponds to a
     * grapheme that can be composed of one or multiple Unicode codepoints.
     */
    void layoutbox() {
        Font textFont = Utils.deriveFont(fontGen, fontGen.getSize() * 4);
        textFlow.setPrefWidth((int) this.spnW.getValue());
        textFlow.getChildren().clear();
        allText.clear();
        
        List<Text> texts = new ArrayList<Text>();
        List<Rectangle> boxes = new ArrayList<Rectangle>();

        String inputText = taInput.getText();
        breakIterator.setText(inputText);
        int start = breakIterator.first();

        for (int end = breakIterator.next(); end != BreakIterator.DONE; start = end, end = breakIterator.next()) {
            String ch = inputText.substring(start, end);
            allText.add(ch);
            Text text = new Text(ch);
            text.setFont(textFont);
            texts.add(text);
            
            Rectangle box = new Rectangle(0, 0, Color.TRANSPARENT);
            boxes.add(box);

            if (ch.length() == 0 || Character.isWhitespace(ch.charAt(0))) {
                // skip if spaces
                continue;
            }

            box.setStroke(Color.RED);
            box.setManaged(false);

            text.boundsInParentProperty().addListener(
                    (ObservableValue<? extends Bounds> obs, Bounds oldValue, Bounds newValue) -> {
                        Bounds b = newValue;
                        box.setX(b.getMinX());
                        box.setY(b.getMinY());
                        box.setWidth(b.getWidth());
                        box.setHeight(b.getHeight());
                    });
        }

        textFlow.getChildren().addAll(texts);
        textFlow.getChildren().addAll(boxes);
    }

    /**
     * Opens input text file.
     *
     * @param selectedFile
     */
    void openTextFile(final File selectedFile) {
        if (!selectedFile.getName().toLowerCase().endsWith(".txt")) {
            return; // not text file
        }
        try {
            String content = new String(Files.readAllBytes(selectedFile.toPath()), StandardCharsets.UTF_8);
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1); // remove BOM
            }
            this.taInput.setText(content);
            Text text = (Text) taInput.lookup(".text");
            text.setLineSpacing((int) this.spnLeading.getValue());

            if (this.tabPane.getSelectionModel().isSelected(1)) {
                layoutbox();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    void generateTiffBox() {
        try {
            String prefix = this.tfPrefix.getText();
            if (prefix.trim().length() > 0) {
                prefix += ".";
            }

            long lastModified = 0;
            File fontpropFile = new File(outputDirectory, prefix + "font_properties");
            if (fontpropFile.exists()) {
                lastModified = fontpropFile.lastModified();
            }

            if (chbText2Image.isSelected()) {
                // execute Text2Image
                String tessDirectory = ((TextField) menuBar.getScene().lookup("#tfTessDir")).getText();
                TessTrainer trainer = new TessTrainer(tessDirectory, outputDirectory, tfPrefix.getText(), null, false);
                String outputbase = tfFileName.getText();
                if (outputbase.endsWith(".tif")) {
                    outputbase = outputbase.substring(0, outputbase.lastIndexOf(".tif"));
                }
                outputbase = outputDirectory + "/" + prefix + outputbase;
                trainer.text2image(inputTextFile.getPath(), outputbase, fontGen, tfFontFolder.getText(), (int) this.spnExposure.getValue(), this.spnTracking.getValue().floatValue(), this.spnLeading.getValue(), (int) this.spnW.getValue(), (int) this.spnH.getValue());
                Utils.removeEmptyBoxes(new File(outputbase + ".box"));
            } else {
                // make box
                if (tabPane.getSelectionModel().getSelectedIndex() == 0) {
                    tabPane.getSelectionModel().select(1);

                    // wait for box layout complete first
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            generateTiffBox();
                        }
                    });
                    return;
                }

                pageNum = 1;
                startIndex = 0;
                textPages.clear();
                breakPages((int) this.spnH.getValue());

                TiffBoxGeneratorFX generator = new TiffBoxGeneratorFX(textPages, fontGen, (int) this.spnW.getValue(), (int) this.spnH.getValue());
                generator.setOutputFolder(new File(outputDirectory));
                generator.setFileName(prefix + this.tfFileName.getText());
                generator.setTracking(this.spnTracking.getValue().floatValue());
                generator.setLeading(this.spnLeading.getValue());
                generator.setMargin(margin);
                generator.setNoiseAmount((int) this.spnNoise.getValue());
                generator.setAntiAliasing(this.chbAntiAliasing.isSelected());
                generator.create();
            }

            // update font_properties file
            Utils.updateFontProperties(new File(outputDirectory), prefix + this.tfFileName.getText(), fontGen);
            String msg = String.format("TIFF/Box files have been generated and saved in %s folder.", outputDirectory);

            if (fontpropFile.exists() && lastModified != fontpropFile.lastModified()) {
                msg = msg.concat("\nBe sure to check the entries in font_properties file for accuracy.");
            }
            Alert alert = new Alert(Alert.AlertType.NONE, msg, ButtonType.OK);
            alert.setTitle(JTessBoxEditor.APP_NAME);
            // workaround text truncate in Linux
            alert.getDialogPane().getChildren().stream().filter(node -> node instanceof Label).forEach(node -> ((Label) node).setMinHeight(Region.USE_PREF_SIZE));
            alert.showAndWait();
        } catch (OutOfMemoryError oome) {
            String msg = "The input text was probably too large. Please reduce it to a more manageable amount.";
            Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            alert.setTitle("Out-Of-Memory Error");
            alert.setHeaderText(null);
            alert.showAndWait();
        } catch (Exception e) {
            //e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage() != null ? e.getMessage() : e.toString(), ButtonType.OK);
            alert.setTitle(JTessBoxEditor.APP_NAME);
            alert.setHeaderText(null);
            alert.showAndWait();
        } finally {
            btnGenerate.setDisable(false);
            taInput.getScene().setCursor(Cursor.DEFAULT);
        }
    }

    void breakPages(int height) {
        List<Rectangle> boxes = this.textFlow.getChildren().stream()
                .filter(b -> b instanceof Rectangle)
                .map(b -> (Rectangle) b)
                .collect(Collectors.toList());
        
        int printableHeight = pageNum == 1 ? height - margin * 2 : height - margin * 2 * pageNum;
        Optional<Rectangle> op = boxes.stream().filter(b -> (b.getY() + b.getHeight()) > printableHeight).findFirst();
        if (op.isPresent()) {
            int endIndex = boxes.indexOf(op.get());
            textPages.add(allText.subList(startIndex, endIndex));
            pageNum++;
            startIndex = endIndex;
            breakPages((int) this.spnH.getValue() * pageNum);
        } else {
            textPages.add(allText.subList(startIndex, allText.size()));
        }
    }

    public void savePrefs() {
        if (outputDirectory != null) {
            prefs.put("outputDirectory", outputDirectory);
        }

        if (fontFolder != null) {
            prefs.put("fontFolder", fontFolder);
        }

        prefs.putBoolean("Text2Image", chbText2Image.isSelected());
        prefs.put("trainLanguage", tfPrefix.getText());
        prefs.put("trainfontName", fontGen.getFamily());
        prefs.putDouble("trainfontSize", fontGen.getSize());
        prefs.put("trainfontStyle", fontGen.getStyle());
    }

    /**
     * Gets font description.
     *
     * @param font selected font
     * @return font description
     */
    String fontDesc(Font font) {
        return font.getName() + " " + (int) font.getSize() + "pt";
    }

    /**
     * Creates file name.
     *
     * @param font
     * @return file name
     */
    String createFileName(Font font) {
        return font.getFamily().replace(" ", "").toLowerCase() + (font.getStyle().contains("Bold") ? "b" : "") + (font.getStyle().contains("Italic") ? "i" : "");
    }
}
