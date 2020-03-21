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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import net.sourceforge.tessboxeditor.utilities.Utils;

public class TrainerController implements Initializable {

    @FXML
    private TextField tfTessDir;
    @FXML
    private Button btnBrowseTess;
    @FXML
    private TextField tfDataDir;
    @FXML
    private Button btnBrowseData;
    @FXML
    protected TextField tfLang;
    @FXML
    private TextField tfBootstrapLang;
    @FXML
    private CheckBox chbRTL;
    @FXML
    private ComboBox<TrainingMode> cbOps;
    @FXML
    private Button btnTrain;
    @FXML
    protected Button btnValidate;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnSaveLog;
    @FXML
    private Button btnClearLog;
    @FXML
    protected TextArea taOutput;
    @FXML
    protected ProgressBar progressBar1;
    @FXML
    protected Label labelStatus;

    protected static final String DIALOG_TITLE = "Train Tesseract";
    protected String tessDirectory;
    protected String trainDataDirectory;
    private DirectoryChooser fcTrainingData;
    private FileChooser fcTessExecutables;
    final Preferences prefs = MainController.prefs;

    private TrainingWorker trainWorker;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        tessDirectory = prefs.get("tessDirectory", null);
        if (tessDirectory == null || !new File(tessDirectory).exists()) {
            tessDirectory = MainController.WINDOWS ? new File(System.getProperty("user.dir"), "tesseract-ocr").getPath() : "/usr/bin";        
        }
        tfTessDir.setText(tessDirectory);
        tfTessDir.setStyle("-fx-focus-color: transparent;");

        fcTessExecutables = new FileChooser();
        fcTessExecutables.setTitle("Set Location of Tesseract Executables");
        fcTessExecutables.setInitialDirectory(new File(tessDirectory));

        trainDataDirectory = prefs.get("trainDataDirectory", new File(System.getProperty("user.dir"), "samples/vie").getPath());
        if (!Files.exists(Paths.get(trainDataDirectory))) {
            trainDataDirectory = System.getProperty("user.home");
        }
        tfDataDir.setText(trainDataDirectory);
        tfDataDir.setStyle("-fx-focus-color: transparent;");

        fcTrainingData = new DirectoryChooser();
        fcTrainingData.setTitle("Set Location of Source Training Data");
        fcTrainingData.setInitialDirectory(new File(trainDataDirectory));

        tfLang.setText(prefs.get("trainnedLanguage", ""));
        tfBootstrapLang.setText(prefs.get("bootstrapLanguage", ""));
        cbOps.getItems().addAll(TrainingMode.values());
        cbOps.getSelectionModel().select(prefs.getInt("trainingMode", 0));
        chbRTL.setSelected(prefs.getBoolean("trainingRTL", false));
    }

    @FXML
    protected void handleAction(ActionEvent event) {
        if (event.getSource() == btnTrain) {
            train();
        } else if (event.getSource() == btnCancel) {
            if (trainWorker != null && !trainWorker.isDone()) {
                trainWorker.cancel(true);
                taOutput.appendText("** Cancel Training **");
            }
            this.btnCancel.setDisable(true);
        } else if (event.getSource() == btnValidate) {
            validate();
        } else if (event.getSource() == btnBrowseTess) {
            File file = fcTessExecutables.showOpenDialog(btnBrowseTess.getScene().getWindow());
            if (file != null) {
                tessDirectory = file.getParentFile().getPath();
                tfTessDir.setText(tessDirectory);
            }
        } else if (event.getSource() == btnBrowseData) {
            File dir = fcTrainingData.showDialog(btnBrowseData.getScene().getWindow());
            if (dir != null) {
                trainDataDirectory = dir.getPath();
                tfDataDir.setText(trainDataDirectory);
            }
        } else if (event.getSource() == btnSaveLog) {
            if (taOutput.getLength() == 0) {
                return;
            }

            try {
                File outFile = new File(trainDataDirectory, "training.log");
                try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {
                    out.write(taOutput.getText());
                }

                String msg = String.format("Log has been saved as \"%s\".", outFile.getPath());
                Alert alert = new Alert(Alert.AlertType.NONE, msg, ButtonType.OK);
                alert.setTitle(DIALOG_TITLE);
                alert.show();
            } catch (IOException e) {
                //ignore
            }
        } else if (event.getSource() == btnClearLog) {
            this.taOutput.clear();
        }
    }

    void train() {
        String msg = "";

        TrainingMode selectedMode = TrainingMode.getValueByDesc(this.cbOps.getSelectionModel().getSelectedItem().toString());
        if (this.tfTessDir.getLength() == 0 || this.tfDataDir.getLength() == 0) {
            msg = "Input is not complete.";
        } else if (this.tfLang.getText() == null || this.tfLang.getText().trim().length() == 0) {
            msg = "Language is required.";
        } else if (selectedMode == TrainingMode.HeaderText) {
            msg = "Please select a Training Mode.";
        }

        if (msg.length() > 0) {
            new Alert(Alert.AlertType.NONE, msg, ButtonType.OK).showAndWait();
            return;
        }

        // make sure all required data files exist before training
        if (selectedMode == TrainingMode.Train_with_Existing_Box || selectedMode == TrainingMode.Dictionary || selectedMode == TrainingMode.Train_from_Scratch) {
            final String lang = tfLang.getText();

            File font_propertiesFile = new File(trainDataDirectory, lang + ".font_properties");
            Utils.createFile(font_propertiesFile);
            File frequent_words_listFile = new File(trainDataDirectory, lang + ".frequent_words_list");
            Utils.createFile(frequent_words_listFile);
            File words_listFile = new File(trainDataDirectory, lang + ".words_list");
            Utils.createFile(words_listFile);

            boolean otherFilesExist = font_propertiesFile.exists() && frequent_words_listFile.exists() && words_listFile.exists();
            if (!otherFilesExist) {
                msg = String.format("The required file %1$s.font_properties, %1$s.frequent_words_list, or %1$s.words_list does not exist.", lang);
                new Alert(Alert.AlertType.NONE, msg, ButtonType.OK).showAndWait();
                return;
            }
        }

        String[] boxFiles = new File(trainDataDirectory).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".box");
            }
        });

        // warn about potential box overwrite
        if (selectedMode == TrainingMode.Make_Box_File || selectedMode == TrainingMode.Make_LSTM_Box_File || selectedMode == TrainingMode.Make_WordStr_Box_File || selectedMode == TrainingMode.Train_from_Scratch) {
            if (boxFiles.length > 0) {
                Alert alert = new Alert(AlertType.CONFIRMATION, "There are existing box files. Continuing may overwrite them.\nDo you want to proceed?");
                alert.setHeaderText(null);
                Optional<ButtonType> option = alert.showAndWait();
                if (option.isPresent() && option.get() == ButtonType.CANCEL) {
                    return;
                }
            }
        } else if (boxFiles.length == 0) {
            new Alert(AlertType.NONE, "There are no existing box files.", ButtonType.OK).showAndWait();
            return;
        }

        this.btnTrain.setDisable(true);
        this.btnCancel.setDisable(false);
        this.progressBar1.setVisible(true);
        labelStatus.getScene().setCursor(Cursor.WAIT);
        taOutput.setCursor(Cursor.WAIT);
        this.btnCancel.setDisable(false);
        trainWorker = new TrainingWorker();
        new Thread(trainWorker).start();
    }

    /**
     * A worker class for training process.
     */
    public class TrainingWorker extends Task<Void> {

        TessTrainer trainer;
        long startTime;

        public TrainingWorker() {
            trainer = new TessTrainer(tessDirectory, trainDataDirectory, tfLang.getText(), tfBootstrapLang.getText(), chbRTL.isSelected());
            progressBar1.progressProperty().unbind();
            progressBar1.progressProperty().bind(this.progressProperty());
            labelStatus.textProperty().unbind();
            labelStatus.textProperty().bind(this.messageProperty());
            trainer.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            taOutput.appendText(newValue + "\n");
//                        taOutput.positionCaret(taOutput.getLength());
                        }
                    });
                }
            });

            // listen for any failure during training
            this.exceptionProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    Exception ex = (Exception) newValue;
                    String msg = ex.getMessage();

                    if (msg != null) {
                        Alert alert = new Alert(Alert.AlertType.NONE, msg, ButtonType.OK);
                        alert.setTitle("Train Tesseract");
                        alert.show();
                    }
                }
            });
        }

        @Override
        protected Void call() throws Exception {
            startTime = System.currentTimeMillis();
            updateMessage("Training...");
            trainer.generate(TrainingMode.getValueByDesc(cbOps.getSelectionModel().getSelectedItem().toString()));
            return null;
        }

        @Override
        protected void succeeded() {
            super.succeeded();

            long millis = System.currentTimeMillis() - startTime;
            updateMessage("Completed. Elapsed time: " + getDisplayTime(millis));

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    progressBar1.progressProperty().unbind();
                    progressBar1.setProgress(1);
                    btnTrain.setDisable(false);
                    btnCancel.setDisable(true);
                    labelStatus.getScene().setCursor(Cursor.DEFAULT);
                    taOutput.setCursor(Cursor.DEFAULT);
                }
            });
        }

        @Override
        protected void cancelled() {
            super.cancelled();
            updateMessage("Training cancelled.");

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    progressBar1.progressProperty().unbind();
                    progressBar1.setProgress(0);
                    btnTrain.setDisable(false);
                    btnCancel.setDisable(true);
                    labelStatus.getScene().setCursor(Cursor.DEFAULT);
                    taOutput.setCursor(Cursor.DEFAULT);
                }
            });
        }

        @Override
        protected void failed() {
            super.failed();
            updateMessage("Failed!");

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    progressBar1.progressProperty().unbind();
                    progressBar1.setProgress(0);
                    btnTrain.setDisable(false);
                    btnCancel.setDisable(true);
                    labelStatus.getScene().setCursor(Cursor.DEFAULT);
                    taOutput.setCursor(Cursor.DEFAULT);
                }
            });
        }
    }

    public static String getDisplayTime(long millis) {
        String elapsedTime = String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % 60,
                TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        );
        return elapsedTime;
    }

    void validate() {
        // to be implemented in subclass
    }

    void setFont(Font font) {
        this.taOutput.setFont(font);
    }

    public void savePrefs() {
        if (tessDirectory != null) {
            prefs.put("tessDirectory", tessDirectory);
        }
        if (trainDataDirectory != null) {
            prefs.put("trainDataDirectory", trainDataDirectory);
        }
        prefs.put("trainnedLanguage", this.tfLang.getText());
        prefs.put("bootstrapLanguage", this.tfBootstrapLang.getText());
        prefs.putInt("trainingMode", this.cbOps.getSelectionModel().getSelectedIndex());
        prefs.putBoolean("trainingRTL", this.chbRTL.isSelected());
    }

}
