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

import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class JTessBoxEditor extends Application {

    public static final String APP_NAME = "jTessBoxEditorFX";

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Main.fxml"));
        Parent root = fxmlLoader.load();
        ((MainController) fxmlLoader.getController()).setStageState(stage);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("resources/editor.css").toExternalForm());
        stage.setTitle(APP_NAME);
        stage.setScene(scene);
        stage.show();
        
        Logger logger = Logger.getLogger("");
        logger.setUseParentHandlers(false);
        Handler fh = new FileHandler("program.log");
        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
