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

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

public class BoxEditorKeyEventController extends BoxEditorEditController {

    private static int movementMultiplier = 1;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        super.initialize(url, bundle);

        tabPane.sceneProperty().addListener((observableScene, oldScene, newScene) -> {
            if (oldScene == null && newScene != null) {
                // scene is set for the first time
                installEventHandler(newScene);
            }
        });
    }

    private void installEventHandler(final Scene scene) {
        scene.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            private void inc(Spinner s) {
                if (s == spinnerX || s == spinnerY) {
                    s.decrement(movementMultiplier);
                } else {
                    s.increment(movementMultiplier);
                }
            }

            private void dec(Spinner s) {
                if (s == spinnerX || s == spinnerY) {
                    s.increment(movementMultiplier);
                } else {
                    s.decrement(movementMultiplier);
                }
            }

            @Override
            public void handle(final KeyEvent keyEvent) {
                Node focusOwner = scene.getFocusOwner();

                if ((focusOwner instanceof Spinner) || (focusOwner instanceof TextField)) {
                    return;
                }

                if (labelCharacter.isFocused() || !tabBoxView.isSelected()) {
                    return;
                }

                movementMultiplier = keyEvent.isShiftDown() ? 10 : 1;

                String str = keyEvent.getText();
                if (str.length() == 0) {
                    return;
                }
                char c = Character.toLowerCase(str.charAt(0));

                switch (c) {
                    case 'w':
                        inc(spinnerY);
                        break;
                    case 's':
                        dec(spinnerY);
                        break;
                    case 'd':
                        dec(spinnerX);
                        break;
                    case 'a':
                        inc(spinnerX);
                        break;
                    case 'q':
                        dec(spinnerW);
                        break;
                    case 'e':
                        inc(spinnerW);
                        break;
                    case 'r':
                        dec(spinnerH);
                        break;
                    case 'f':
                        inc(spinnerH);
                        break;
                    case ',':
                        paginationBox.setCurrentPageIndex(paginationBox.getCurrentPageIndex() - 1);
                        break;
                    case '.':
                        paginationBox.setCurrentPageIndex(paginationBox.getCurrentPageIndex() + 1);
                        break;
                }
            }
        });

        scene.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(final KeyEvent keyEvent) {
                Node focusOwner = scene.getFocusOwner();

                if ((focusOwner instanceof Spinner) || (focusOwner instanceof TextField)) {
                    return;
                }

                if (labelCharacter.isFocused() || !tabBoxView.isSelected()) {
                    return;
                }

                String str = keyEvent.getText();

                if (str.toLowerCase().equals("x")) {
                    tfCharacter.requestFocus();
                }
            }
        });
    }
}
