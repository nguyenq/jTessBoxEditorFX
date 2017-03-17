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

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import net.sourceforge.tess4j.util.ImageIOHelper;
import net.sourceforge.tessboxeditor.control.ImageCanvas;
import net.sourceforge.tessboxeditor.datamodel.TessBox;
import net.sourceforge.tessboxeditor.datamodel.TessBoxCollection;
import net.sourceforge.tessboxeditor.utilities.*;
import net.sourceforge.vietocr.util.Utils;
import net.sourceforge.vietpad.utilities.TextUtilities;

public class BoxEditorController implements Initializable {

    @FXML
    private SplitPane spBoxImage;
    @FXML
    private Button btnOpen;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnReload;
    @FXML
    private Region rgn2;
    @FXML
    private Button btnConvert;
    @FXML
    private Button btnFind;
    @FXML
    private TextField tfFind;
    @FXML
    protected Label labelCharacter;
    @FXML
    protected TextField tfCharacter;
    @FXML
    private TextField tfChar;
    @FXML
    private TextField tfCodepointValue;
    @FXML
    private StackPane stackPaneBoxView;
    @FXML
    protected Spinner<Integer> spinnerH;
    @FXML
    protected Spinner<Integer> spinnerW;
    @FXML
    protected Spinner<Integer> spinnerX;
    @FXML
    protected Spinner<Integer> spinnerY;
    @FXML
    private Spinner<Integer> spnMargins;
    @FXML
    private Spinner<Integer> spnScales;
    @FXML
    protected Pagination paginationBox;
    @FXML
    private Pagination paginationPage;
    @FXML
    private Region rgn3;
    @FXML
    private TextArea taBoxData;
    @FXML
    protected TabPane tabPane;
    @FXML
    protected Tab tabBoxView;
    @FXML
    protected ImageCanvas imageCanvas;
    @FXML
    private ScrollPane scrollPaneImage;
    @FXML
    private ImageView charImageView;
    @FXML
    private Rectangle charRectangle;
    @FXML
    private Label labelPageNbr;
    @FXML
    protected TableView<TessBox> tableView;
    @FXML
    private TableColumn<TessBox, String> tcChar;
    @FXML
    private TableColumn<TessBox, Integer> tcX;
    @FXML
    private TableColumn<TessBox, Integer> tcY;
    @FXML
    private TableColumn<TessBox, Integer> tcWidth;
    @FXML
    private TableColumn<TessBox, Integer> tcHeight;
    @FXML
    private TableColumn<TessBox, Integer> tcNum;

    private static final String IMAGE_PATTERN = "([^\\s]+(\\.(?i)(png|tif|tiff))$)";
    protected ResourceBundle bundle;
    final Preferences prefs = MainController.prefs;

    private File boxFile;
    private String currentDirectory, outputDirectory;
    protected List<TessBoxCollection> boxPages;
    protected TessBoxCollection boxes; // boxes of current page
    private short imageIndex;
    private int filterIndex;
    protected List<BufferedImage> imageList;
    private boolean isTess2_0Format;
    private BooleanProperty boxChangedProp;
    protected boolean tableSelectAction;
    static final String EOL = System.getProperty("line.separator");
    final String[] headers = {"Char", "X", "Y", "Width", "Height"};

    ObservableList<ExtensionFilter> fileFilters; //extensionFilters
    FileChooser fc;

    protected static int iconMargin = 3;
    protected static int scaleFactor = 4;

    Image image;

    private final StringProperty fontFamily = new SimpleStringProperty(Font.getDefault().getFamily());
    private final IntegerProperty fontSize = new SimpleIntegerProperty((int) Font.getDefault().getSize());
    private final StringProperty style = new SimpleStringProperty();

    private final static Logger logger = Logger.getLogger(BoxEditorController.class.getName());

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        style.bind(Bindings.createStringBinding(() -> String.format(
                "-fx-font-family: \"%s\"; -fx-font-size: %d;",
                fontFamily.get(), fontSize.get()
        ), fontFamily, fontSize
        ));

        currentDirectory = prefs.get("currentDirectory", System.getProperty("user.home"));
        if (!new File(currentDirectory).exists()) {
            currentDirectory = System.getProperty("user.home");
        }
        outputDirectory = currentDirectory;
        boxPages = new ArrayList<TessBoxCollection>();
        filterIndex = prefs.getInt("filterIndex", 0);

        if (MainController.LINUX) {
            stackPaneBoxView.setStyle("-fx-background-color: LightGray;");
        }

        boxChangedProp = new SimpleBooleanProperty();
        btnSave.disableProperty().bind(boxChangedProp.not());

        bundle = ResourceBundle.getBundle("net.sourceforge.tessboxeditor.Gui"); // NOI18N
        fc = new FileChooser();
        fc.setTitle("Open Image File");
        ExtensionFilter allImageFilter = new ExtensionFilter(bundle.getString("All_Image_Files"), "*.bmp", "*.jpg", "*.jpeg", "*.png", "*.tif", "*.tiff");
        ExtensionFilter pngFilter = new ExtensionFilter("PNG", "*.png");
        ExtensionFilter tiffFilter = new ExtensionFilter("TIFF", "*.tif", "*.tiff");

        fileFilters = fc.getExtensionFilters();
        fileFilters.addAll(allImageFilter, pngFilter, tiffFilter);
        if (filterIndex < fileFilters.size()) {
            fc.setSelectedExtensionFilter(fileFilters.get(filterIndex));
        }

        HBox.setHgrow(rgn2, Priority.ALWAYS);
        HBox.setHgrow(rgn3, Priority.ALWAYS);

        tfCharacter.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !this.btnConvert.isFocused()) {
                if (boxes != null && boxes.getSelectedBoxes().size() == 1) {
                    String str = tfCharacter.getText();
                    boxes.getSelectedBoxes().get(0).setCharacter(str);
                    tfChar.setText(str);
                    tfCodepointValue.setText(Utils.toHex(str));
                }
            }
        });

        tableView.setRowFactory(tv -> {
            TableRow<TessBox> row = new TableRow<>();
            row.styleProperty().bind(style);
            return row;
        });

        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends TessBox> obs, TessBox oldSelection, TessBox newSelection) -> {
            if (newSelection != null) {
                int selectedIndex = tableView.getSelectionModel().getSelectedIndex();
                if (selectedIndex != -1) {
                    if (!imageCanvas.isBoxClickAction()) { // not from image block click
                        boxes.deselectAll();
                    }
                    ObservableList<TessBox> boxesOfCurPage = boxes.toList(); // boxes of current page
                    for (int index : tableView.getSelectionModel().getSelectedIndices()) {
                        TessBox box = boxesOfCurPage.get(index);
                        // select box
                        box.setSelected(true);
                        scrollRectToVisible(scrollPaneImage, box.getRect());
                    }
                    imageCanvas.paint();

                    if (tableView.getSelectionModel().getSelectedIndices().size() == 1) {
                        enableReadout(true);
                        // update Character field
                        String str = newSelection.getCharacter();
                        tfCharacter.setText(str);
                        tfChar.setText(str);
                        tfCodepointValue.setText(Utils.toHex(str));
                        // mark this as table action event to prevent cyclic firing of events by spinners or box pagination
                        tableSelectAction = true;
                        paginationBox.setDisable(false);
                        paginationBox.setCurrentPageIndex(selectedIndex);
                        // update subimage
                        TessBox curBox = boxesOfCurPage.get(selectedIndex);
                        Rectangle2D rect = curBox.getRect();
                        updateSubimage(rect);

                        // update spinners
                        spinnerX.getValueFactory().setValue((int) rect.getMinX());
                        spinnerY.getValueFactory().setValue((int) rect.getMinY());
                        spinnerW.getValueFactory().setValue((int) rect.getWidth());
                        spinnerH.getValueFactory().setValue((int) rect.getHeight());
                        tableSelectAction = false;
                    } else {
                        enableReadout(false);
                        resetReadout();
                    }
                } else {
                    boxes.deselectAll();
                    imageCanvas.paint();
                    enableReadout(false);
                    tableSelectAction = true;
                    resetReadout();
                    tableSelectAction = false;
                    paginationBox.setDisable(true);
                }
            } else {
                int lastSelectedIndex = tableView.getSelectionModel().getSelectedIndex();
                if (lastSelectedIndex != -1) {
                    TessBox box = boxes.toList().get(lastSelectedIndex);
                    // deselect box
                    box.setSelected(false);
                    imageCanvas.paint();
                }

                tfChar.setText(null);
                tfCharacter.setText(null);
                tfCodepointValue.setText(null);
                paginationBox.setDisable(true);
                enableReadout(false);
                tableSelectAction = true;
                resetReadout();
                tableSelectAction = false;
            }
        });

        tcChar.setCellValueFactory(new PropertyValueFactory<TessBox, String>("character"));
        tcChar.setCellFactory(TextFieldTableCell.forTableColumn());
        tcChar.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent<TessBox, String>>() {
            @Override
            public void handle(TableColumn.CellEditEvent<TessBox, String> e) {
                String str = e.getNewValue();
                ((TessBox) e.getTableView().getItems().get(e.getTablePosition().getRow())).setCharacter(str);
                tfCharacter.setText(str);
                tfChar.setText(str);
                tfCodepointValue.setText(Utils.toHex(str));
            }
        });
        tcX.setCellValueFactory(new PropertyValueFactory<TessBox, Integer>("x"));
        tcY.setCellValueFactory(new PropertyValueFactory<TessBox, Integer>("y"));
        tcWidth.setCellValueFactory(new PropertyValueFactory<TessBox, Integer>("width"));
        tcHeight.setCellValueFactory(new PropertyValueFactory<TessBox, Integer>("height"));

        tcNum.setCellValueFactory(column -> new ReadOnlyObjectWrapper<Integer>(tableView.getItems().indexOf(column.getValue()) + 1));
        // alternatively
//        tcNum.setCellFactory(new Callback<TableColumn, TableCell>() {
//            @Override
//            public TableCell call(TableColumn p) {
//                return new TableCell() {
//                    @Override
//                    public void updateItem(Object item, boolean empty) {
//                        super.updateItem(item, empty);
//                        setGraphic(null);
//                        setText(empty ? null : String.valueOf(getIndex() + 1));
//                    }
//                };
//            }
//        });

        spBoxImage.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                if (db.hasFiles()) {
                    File file = db.getFiles().get(0);
                    boolean isAccepted = file.getName().matches(IMAGE_PATTERN);
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

        spBoxImage.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                    success = true;
                    MainController.getInstance().openFile(db.getFiles().get(0));
                }

                event.setDropCompleted(success);
                event.consume();
            }
        });

        this.spinnerX.valueProperty().addListener((obs, oldValue, newValue) -> {
            valuesChanged("X", newValue);
        });

        this.spinnerY.valueProperty().addListener((obs, oldValue, newValue) -> {
            valuesChanged("Y", newValue);
        });

        this.spinnerW.valueProperty().addListener((obs, oldValue, newValue) -> {
            valuesChanged("W", newValue);
        });

        this.spinnerH.valueProperty().addListener((obs, oldValue, newValue) -> {
            valuesChanged("H", newValue);
        });

        this.spnMargins.valueProperty().addListener((obs, oldValue, newValue) -> {
            iconMargin = (int) newValue;
            int index = tableView.getSelectionModel().getSelectedIndex();
            tableView.getSelectionModel().clearSelection();
            tableView.getSelectionModel().select(index);
            charImageView.requestFocus();
        });

        this.spnScales.valueProperty().addListener((obs, oldValue, newValue) -> {
            scaleFactor = (int) newValue;
            int index = tableView.getSelectionModel().getSelectedIndex();
            tableView.getSelectionModel().clearSelection();
            tableView.getSelectionModel().select(index);
            charImageView.requestFocus();
        });

        paginationBox.setStyle("-fx-page-information-visible: false;");
        paginationBox.currentPageIndexProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (tableSelectAction) {
                return;
            }
            if (boxes != null) {
                tableView.getSelectionModel().clearAndSelect(newValue.intValue());
            }
        });

        paginationPage.setStyle("-fx-page-information-alignment: left;");
        paginationPage.currentPageIndexProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (imageList != null) {
                imageIndex = newValue.shortValue();
                loadImage();
                loadTable();
            }
        });
    }

    void setMenuBar(MenuBar menuBar) {
        Menu fileMenu = menuBar.getMenus().get(0);
        FilteredList<MenuItem> menuItems = fileMenu.getItems().filtered(item -> item.getId().equals("miSave"));
        menuItems.get(0).disableProperty().bind(this.btnSave.disabledProperty());
    }

    @FXML
    protected void handleAction(ActionEvent event) {
        if (event.getSource() == btnOpen) {
            fc.setInitialDirectory(new File(currentDirectory));
            File file = fc.showOpenDialog(btnOpen.getScene().getWindow());
            if (file != null) {
                currentDirectory = file.getParent();
                filterIndex = fileFilters.indexOf(fc.getSelectedExtensionFilter());
                MainController.getInstance().openFile(file);
            }
        } else if (event.getSource() == btnSave) {
            saveAction();
        } else if (event.getSource() == btnReload) {
            if (!promptToDiscardChanges()) {
                return;
            }

            if (boxFile != null) {
                btnReload.setDisable(true);
                btnReload.getScene().setCursor(Cursor.WAIT);

                Task<Void> loadWorker = new Task<Void>() {

                    @Override
                    protected Void call() throws Exception {
                        loadBoxes(boxFile);
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                btnReload.setDisable(false);
                                btnReload.getScene().setCursor(Cursor.DEFAULT);
                            }
                        });
                    }
                };

                new Thread(loadWorker).start();
            }
        } else if (event.getSource() == btnConvert) {
            String curChar = this.tfCharacter.getText();
            if (curChar.trim().length() == 0) {
                return;
            }
            // Convert NCR or escape sequence to Unicode.
            this.tfCharacter.setText(TextUtilities.convertNCR(this.tfCharacter.getText()));
            // Commit the change, if no conversion.
            if (curChar.equals(this.tfCharacter.getText())) {
                tfCharacter.commitValue();
                handleAction(new ActionEvent(tfCharacter, null));
            }
        } else if (event.getSource() == tfCharacter) {
            if (boxes.getSelectedBoxes().size() == 1) {
                String str = tfCharacter.getText();
                boxes.getSelectedBoxes().get(0).setCharacter(str);
                tfChar.setText(str);
                tfCodepointValue.setText(Utils.toHex(str));
                boxChangedProp.set(true);
            }
        } else if (event.getSource() == btnFind || event.getSource() == tfFind) {
            if (imageList == null) {
                return;
            }
            int pageHeight = imageList.get(imageIndex).getHeight();
            String[] items = this.tfFind.getText().split("\\s+");
            try {
                TessBox findBox;

                if (items.length == 1) {
                    String chrs = items[0];
                    if (chrs.length() == 0) {
                        throw new Exception("Empty search values.");
                    }
                    // Convert NCR or escape sequence to Unicode.
                    chrs = TextUtilities.convertNCR(chrs);

                    findBox = new TessBox(chrs, Rectangle2D.EMPTY, imageIndex);
                    findBox = boxes.selectByChars(findBox);
                } else {
                    int x = Integer.parseInt(items[0]);
                    int y = Integer.parseInt(items[1]);
                    int w = Integer.parseInt(items[2]) - x;
                    int h = Integer.parseInt(items[3]) - y;
                    y = pageHeight - y - h; // flip the y-coordinate
                    findBox = new TessBox("", new Rectangle2D(x, y, w, h), imageIndex);
                    findBox = boxes.select(findBox);
                }

                if (findBox != null) {
                    int index = boxes.toList().indexOf(findBox);
                    this.tableView.getSelectionModel().clearAndSelect(index);
                    this.tableView.scrollTo(index > 10 ? index - 4 : index);
                } else {
                    this.tableView.getSelectionModel().clearSelection();
                    String msg = String.format("No box with the specified %s was found.", items.length == 1 ? "character(s)" : "coordinates");
                    new Alert(Alert.AlertType.NONE, msg, ButtonType.OK).showAndWait();
                }
            } catch (Exception e) {
                new Alert(Alert.AlertType.NONE, "Please enter box character(s) or coordinates (x1 y1 x2 y2).", ButtonType.OK).showAndWait();
            }
        }
    }

    private void valuesChanged(String changedValue, int value) {
        if (tableSelectAction || boxes == null) {
            return;
        }
        TessBox selectedBox = null;
        if (boxes.getSelectedBoxes().size() == 1) {
            selectedBox = boxes.getSelectedBoxes().get(0);
        }
        if (selectedBox != null) {
            int x = selectedBox.getX();
            int y = selectedBox.getY();
            int w = selectedBox.getWidth();
            int h = selectedBox.getHeight();

            if (changedValue.equals("X")) {
                x = value;
            } else if (changedValue.equals("Y")) {
                y = value;
            } else if (changedValue.equals("W")) {
                w = value;
            } else if (changedValue.equals("H")) {
                h = value;
            }

            Rectangle2D newRect = new Rectangle2D(x, y, w, h);
            if (!selectedBox.getRect().equals(newRect)) {
                selectedBox.setRect(newRect);
                boxChangedProp.set(true);
                imageCanvas.paint();
            }

            // update subimage
            updateSubimage(newRect);
        }
    }

    /**
     * Draws bounding box for individual box view.
     *
     * @param newRect
     */
    void updateSubimage(Rectangle2D newRect) {
        Image subImage = ImageUtils.getSubimage(image, newRect, iconMargin);
        Image rescaledImage = ImageUtils.resample(subImage, scaleFactor);
        charImageView.setImage(rescaledImage);
        charImageView.setFitWidth(rescaledImage.getWidth());
        charImageView.setFitHeight(rescaledImage.getHeight());
        charRectangle.setX(iconMargin * scaleFactor);
        charRectangle.setY(iconMargin * scaleFactor);
        charRectangle.setWidth(rescaledImage.getWidth() - iconMargin * scaleFactor * 2);
        charRectangle.setHeight(rescaledImage.getHeight() - iconMargin * scaleFactor * 2);
    }

    /**
     * Open image and box file.
     *
     * @param selectedFile
     */
    public void openFile(final File selectedFile) {
        if (!selectedFile.exists()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, bundle.getString("File_not_exist"));
            alert.show();
            return;
        }
        if (!promptToSave()) {
            return;
        }

        Task loadWorker = new Task<Void>() {

            @Override
            public Void call() throws Exception {
                readImageFile(selectedFile);
                int lastDot = selectedFile.getName().lastIndexOf(".");
                boxFile = new File(selectedFile.getParentFile(), selectedFile.getName().substring(0, lastDot) + ".box");
                loadBoxes(boxFile);
                return null;
            }
        };

        new Thread(loadWorker).start();
    }

    void readImageFile(File selectedFile) {
        try {
            imageList = ImageIOHelper.getImageList(selectedFile);
            if (imageList == null) {
                new Alert(Alert.AlertType.ERROR, bundle.getString("Cannotloadimage")).show();
                return;
            }
            imageIndex = 0;

            Platform.runLater(() -> {
                paginationPage.setPageCount(imageList.size());
                paginationPage.setCurrentPageIndex(0);
                loadImage();
                this.scrollPaneImage.setVvalue(0); // scroll to top
                this.scrollPaneImage.setHvalue(0); // scroll to left
                ((Stage) tableView.getScene().getWindow()).setTitle(JTessBoxEditor.APP_NAME + " - " + selectedFile.getName());
            });
        } catch (OutOfMemoryError oome) {
            new Alert(Alert.AlertType.ERROR, "Out-Of-Memory Exception").show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            if (e.getMessage() != null) {
                new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
            }
        }
    }

    void loadBoxes(File boxFile) {
        if (boxFile.exists()) {
            try {
                boxPages.clear();

                // load into textarea first
                String content = readBoxFile(boxFile);
                boxPages = parseBoxString(content, imageList);

                Platform.runLater(() -> {
                    this.taBoxData.setText(content);
                    loadTable();
                });
                boxChangedProp.set(false);
            } catch (OutOfMemoryError oome) {
                logger.log(Level.SEVERE, oome.getMessage(), oome);
                new Alert(Alert.AlertType.NONE, oome.getMessage(), ButtonType.OK).showAndWait();
            } catch (IOException | NumberFormatException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                if (e.getMessage() != null) {
                    new Alert(Alert.AlertType.NONE, e.getMessage(), ButtonType.OK).showAndWait();
                }
            }
        } else {
            // clear table and box display
            tableView.setItems(null);
            taBoxData.setText(null);
            imageCanvas.setBoxes(null);
            imageCanvas.setTable(null);
            imageCanvas.paint();
        }
    }

    String readBoxFile(File boxFile) throws IOException {
        return new String(Files.readAllBytes(Paths.get(boxFile.getPath())), StandardCharsets.UTF_8);
    }

    List<TessBoxCollection> parseBoxString(String boxStr, List<BufferedImage> imageList) throws IOException {
        List<TessBoxCollection> allBoxPages = new ArrayList<TessBoxCollection>();

        String[] boxdata = boxStr.split("\\n");
        if (boxdata.length > 0) {
            // if only 5 fields, it's Tess 2.0x format
            isTess2_0Format = boxdata[0].split("\\s+").length == 5;
        }

        int startBoxIndex = 0;

        for (int curPage = 0; curPage < imageList.size(); curPage++) {
            TessBoxCollection boxCol = new TessBoxCollection();
            // Note that the coordinate system used in the box file has (0,0) at the bottom-left.
            // On computer graphics device, (0,0) is defined as top-left.
            int pageHeight = imageList.get(curPage).getHeight();
            for (int i = startBoxIndex; i < boxdata.length; i++) {
                String[] items = boxdata[i].split("\\s+");

                // skip invalid data
                if (items.length < 5 || items.length > 6) {
                    continue;
                }

                String chrs = items[0];
                int x = Integer.parseInt(items[1]);
                int y = Integer.parseInt(items[2]);
                int w = Integer.parseInt(items[3]) - x;
                int h = Integer.parseInt(items[4]) - y;
                y = pageHeight - y - h; // flip the y-coordinate

                short page;
                if (items.length == 6) {
                    page = Short.parseShort(items[5]); // Tess 3.0x format
                } else {
                    page = 0; // Tess 2.0x format
                }
                if (page > curPage) {
                    startBoxIndex = i; // mark begin of next page
                    break;
                }
                boxCol.add(new TessBox(chrs, new Rectangle2D(x, y, w, h), page));
            }
            allBoxPages.add(boxCol); // add the last page
        }

        return allBoxPages;
    }

    /**
     * Displays a dialog to discard changes.
     *
     * @return false if user canceled or discard, true else
     */
    protected boolean promptToDiscardChanges() {
        if (!boxChangedProp.get()) {
            return false;
        }

        Alert alert = new Alert(AlertType.CONFIRMATION, JTessBoxEditor.APP_NAME, ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        alert.setTitle(JTessBoxEditor.APP_NAME);
        alert.setHeaderText(null);
        alert.setContentText(bundle.getString("Do_you_want_to_discard_the_changes_to_") + boxFile.getName() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.YES) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Displays a dialog to save changes.
     *
     * @return false if user canceled, true else
     */
    protected boolean promptToSave() {
        if (!boxChangedProp.get()) {
            return true;
        }

        Alert alert = new Alert(AlertType.CONFIRMATION, JTessBoxEditor.APP_NAME, ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        alert.setTitle(JTessBoxEditor.APP_NAME);
        alert.setHeaderText(null);
        alert.setContentText(bundle.getString("Do_you_want_to_save_the_changes_to_")
                + (boxFile == null ? bundle.getString("Untitled") : boxFile.getName()) + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.YES) {
            return saveAction();
        } else if (result.get() == ButtonType.NO) {
            return true;
        } else {
            return false;
        }
    }

    boolean saveAction() {
        if (boxFile == null || !boxFile.exists()) {
            return saveFileDlg();
        } else {
            return saveBoxFile();
        }
    }

    boolean saveFileDlg() {
        FileChooser fc = new FileChooser();
        fc.setTitle(bundle.getString("Save_As"));
        fc.setInitialDirectory(new File(outputDirectory));
        ExtensionFilter boxFilter = new ExtensionFilter("Box Files", "*.box");
        ExtensionFilter allFilter = new ExtensionFilter("All Files", "*.*");
        fc.getExtensionFilters().addAll(boxFilter, allFilter);

        if (boxFile != null) {
            fc.setInitialDirectory(boxFile.getParentFile());
            fc.setInitialFileName(boxFile.getName());
        }

        File f = fc.showSaveDialog(btnSave.getScene().getWindow());
        if (f != null) {
            outputDirectory = f.getParent();
            if (fc.getSelectedExtensionFilter() == boxFilter) {
                if (!f.getName().endsWith(".box")) {
                    f = new File(f.getPath() + ".box");
                }
                if (boxFile != null && boxFile.getPath().equals(f.getPath())) {
                    Alert alert = new Alert(AlertType.CONFIRMATION);
                    alert.setTitle(bundle.getString("Confirm_Save_As"));
                    alert.setContentText(boxFile.getName() + bundle.getString("file_already_exist"));

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.get() != ButtonType.OK) {
                        return false;
                    }
                } else {
                    boxFile = f;
                }
            } else {
                boxFile = f;
            }
            return saveBoxFile();
        } else {
            return false;
        }
    }

    boolean saveBoxFile() {
        try {
            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(boxFile), StandardCharsets.UTF_8))) {
                out.write(formatOutputString(imageList, boxPages));
            }
            boxChangedProp.set(false);
        } catch (OutOfMemoryError oome) {
            logger.log(Level.SEVERE, oome.getMessage(), oome);
            new Alert(Alert.AlertType.NONE, oome.getMessage(), ButtonType.OK).showAndWait();
        } catch (FileNotFoundException fnfe) {
            logger.log(Level.SEVERE, fnfe.getMessage(), fnfe);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } finally {

        }

        return true;
    }

    String formatOutputString(List<BufferedImage> imageList, List<TessBoxCollection> boxPages) {
        StringBuilder sb = new StringBuilder();
        for (short pageIndex = 0; pageIndex < imageList.size(); pageIndex++) {
            int pageHeight = ((BufferedImage) imageList.get(pageIndex)).getHeight(); // each page (in an image) can have different height
            for (TessBox box : boxPages.get(pageIndex).toList()) {
                Rectangle2D rect = box.getRect();
                sb.append(String.format("%s %.0f %.0f %.0f %.0f %d", box.getCharacter(), rect.getMinX(), pageHeight - rect.getMinY() - rect.getHeight(), rect.getMinX() + rect.getWidth(), pageHeight - rect.getMinY(), pageIndex)).append(EOL);
            }
        }
        if (isTess2_0Format) {
            return sb.toString().replace(" 0" + EOL, EOL); // strip the ending zeroes
        }
        return sb.toString();
    }

    void loadImage() {
        image = SwingFXUtils.toFXImage(imageList.get(imageIndex), null);
        imageCanvas.setImage(image);
        tableSelectAction = true;
        resetReadout();
        tableSelectAction = false;
        imageCanvas.paint();
    }

    void loadTable() {
        if (!this.boxPages.isEmpty()) {
            boxes = this.boxPages.get(imageIndex);
            boxes.deselectAll();
            tableSelectAction = true;
            paginationBox.setPageCount(boxes.toList().size());
            paginationBox.setDisable(true);
            tableSelectAction = false;
            tableView.setItems(boxes.toList());
            tableView.getSelectionModel().clearSelection();
            boxes.toList().addListener(new ListChangeListener<TessBox>() {
                @Override
                public void onChanged(ListChangeListener.Change change) {
                    boxChangedProp.set(true);
                }
            });
            imageCanvas.setBoxes(boxes);
            imageCanvas.setTable(tableView);
            imageCanvas.paint();
        }
    }

    void resetReadout() {
        tfCharacter.setText(null);
        tfChar.setText(null);
        tfCodepointValue.setText(null);
        spinnerH.getValueFactory().setValue(0);
        spinnerW.getValueFactory().setValue(0);
        spinnerX.getValueFactory().setValue(0);
        spinnerY.getValueFactory().setValue(0);
        charImageView.setImage(null);
    }

    void enableReadout(boolean enabled) {
        tfCharacter.setDisable(!enabled);
        spinnerX.setDisable(!enabled);
        spinnerY.setDisable(!enabled);
        spinnerH.setDisable(!enabled);
        spinnerW.setDisable(!enabled);
    }

    void setFont(Font font) {
        // set font for TableColumn, TextField controls, etc.
        this.taBoxData.setFont(font);
        Font font15 = net.sourceforge.tessboxeditor.utilities.Utils.deriveFont(font, Font.getDefault().getSize());
        this.tfCharacter.setFont(font15);
        this.tfFind.setFont(font15);
        this.tfChar.setFont(font15);

        fontFamily.set(font.getFamily());
        fontSize.set((int) font.getSize());

//        Font tableFont = tableView.getFont().deriveFont(font.getSize2D());
//        tableView.setFont(tableFont);
//        FontMetrics metrics = tableView.getFontMetrics(tableFont);
//        tableView.setRowHeight(metrics.getHeight()); // set row height to match font
        //rowHeader.setFont(tableFont);
//        ((MyTableCellEditor)jTable.getDefaultEditor(String.class)).setFont(font);
        this.imageCanvas.setFont(font);
    }

    /**
     * Scrolls pane to rectangle.
     *
     * @param pane
     * @param rect
     */
    private static void scrollRectToVisible(ScrollPane pane, Rectangle2D rect) {
//        // if already visible (inside viewport), do not scroll
//        Bounds viewport = pane.getViewportBounds();

//        if (!viewport.contains(rect.getMinX(), rect.getMinY(), rect.getWidth(), rect.getHeight())) {
//            double width = pane.getContent().getBoundsInLocal().getWidth();
//            double height = pane.getContent().getBoundsInLocal().getHeight();
//
//            pane.setHvalue(rect.getMinX() / width);
//            pane.setVvalue(rect.getMinY() / height);
//        }
//        double contentHeight = pane.getContent().getBoundsInLocal().getHeight();
//        double nodeMinY = rect.getMinY();
//        double nodeMaxY = rect.getMaxY();
//        double viewportMinY = (contentHeight - viewport.getHeight()) * pane.getVvalue();
//        double viewportMaxY = viewportMinY + viewport.getHeight();
//        if (nodeMinY < viewportMinY) {
//            pane.setVvalue(nodeMinY / (contentHeight - viewport.getHeight()));
//        } else if (nodeMaxY > viewportMaxY) {
//            pane.setVvalue((nodeMaxY - viewport.getHeight()) / (contentHeight - viewport.getHeight()));
//        }
//
//        double contentWidth = pane.getContent().getBoundsInLocal().getWidth();
//        double nodeMinX = rect.getMinX();
//        double nodeMaxX = rect.getMaxX();
//        double viewportMinX = (contentWidth - viewport.getWidth()) * pane.getVvalue();
//        double viewportMaxX = viewportMinX + viewport.getWidth();
//        if (nodeMinX < viewportMinX) {
//            pane.setHvalue(nodeMinX / (contentWidth - viewport.getWidth()));
//        } else if (nodeMaxX > viewportMaxX) {
//            pane.setHvalue((nodeMaxX - viewport.getWidth()) / (contentWidth - viewport.getWidth()));
//        }
        double hmin = pane.getHmin();
        double hmax = pane.getHmax();
        double hvalue = pane.getHvalue();
        double contentWidth = pane.getContent().getLayoutBounds().getWidth();
        double viewportWidth = pane.getViewportBounds().getWidth();

        double hoffset = Math.max(0, contentWidth - viewportWidth) * (hvalue - hmin) / (hmax - hmin);

        double vmin = pane.getVmin();
        double vmax = pane.getVmax();
        double vvalue = pane.getVvalue();
        double contentHeight = pane.getContent().getLayoutBounds().getHeight();
        double viewportHeight = pane.getViewportBounds().getHeight();

        double voffset = Math.max(0, contentHeight - viewportHeight) * (vvalue - vmin) / (vmax - vmin);

        Rectangle2D viewBounds = new Rectangle2D(hoffset, voffset, viewportWidth, viewportHeight);

        // is current box inside viewport?
        if (!viewBounds.contains(rect)) {
            pane.setHvalue(rect.getMinX() / contentWidth);
            pane.setVvalue(rect.getMinY() / contentHeight);
        }
    }

    public void savePrefs() {
        if (currentDirectory != null) {
            prefs.put("currentDirectory", currentDirectory);
        }

        prefs.putInt("filterIndex", filterIndex);
    }

}
