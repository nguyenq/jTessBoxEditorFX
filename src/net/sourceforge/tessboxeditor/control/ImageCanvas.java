/**
 * Copyright @ 2016 Quan Nguyen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sourceforge.tessboxeditor.control;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import net.sourceforge.tessboxeditor.datamodel.*;
import net.sourceforge.vietocr.util.Utils;

public class ImageCanvas extends Canvas {

    private TessBoxCollection boxes;
    private TableView tableView;
    private boolean boxClickAction;
    private Image image;
    private Font font = Font.font(24);
    Tooltip tooltip;
    TessBox prevBox;
    boolean installed;

    /**
     * Creates a new instance of ImageCanvas
     */
    public ImageCanvas() {
        tooltip = new Tooltip();

        this.setOnMousePressed((MouseEvent me) -> {
            if (boxes == null || tableView == null) {
                return;
            }

            TessBox box = boxes.hitObject(new Point2D(me.getX(), me.getY()));
            if (box == null) {
                if (!me.isControlDown()) {
                    boxes.deselectAll();
                    //repaint();
                    tableView.getSelectionModel().clearSelection();
                }
            } else {
                if (!me.isControlDown()) {
                    boxes.deselectAll();
                    tableView.getSelectionModel().clearSelection();
                }
                box.setSelected(!box.isSelected()); // toggle selection
                //paint();
                // select corresponding table rows
                boxClickAction = true;
                java.util.List<TessBox> boxesOfCurPage = boxes.toList(); // boxes of current page
                
                if (!box.isSelected()) {
                    int index = boxesOfCurPage.indexOf(box);
                    tableView.getSelectionModel().clearSelection(index);
                }
                for (TessBox selectedBox : boxes.getSelectedBoxes()) {
                    int index = boxesOfCurPage.indexOf(selectedBox);
                    tableView.getSelectionModel().select(index);
                    tableView.scrollTo(index > 10 ? index - 4 : index); // fix issue with selected row pegged at the top
                }
                boxClickAction = false;
            }

            paint();
        });

        this.setOnMouseMoved((MouseEvent me) -> {
            if (this.boxes != null) {
                TessBox curBox = this.boxes.hitObject(me.getX(), me.getY());
                
                if (curBox != null) {
                    if (prevBox != curBox) {
                        prevBox = curBox;
                        String curChrs = curBox.getCharacter();
                        Tooltip.install(this, tooltip);
                        installed = true;
                        tooltip.hide();
                        tooltip.setText(curChrs + " : " + Utils.toHex(curChrs));
                        tooltip.setFont(font);
                        tooltip.show(this, me.getScreenX(), me.getScreenY() + 25);
                    } else {
                        me.consume();
                    }
                } else {
                    if (installed) {
                        installed = false;
                        prevBox = null;
                        tooltip.hide();
                        Tooltip.uninstall(this, tooltip); 
                    }
                }
            }
        });
        
        this.setOnMouseExited((MouseEvent me) -> {
            if (this.boxes != null) {
                tooltip.hide();
            }
        });
    }

    public void paint() {
        final GraphicsContext gc = getGraphicsContext2D();

        if (image == null) {
            return;
        }

        gc.clearRect(0, 0, getWidth(), getHeight());
        gc.drawImage(image, 0, 0);
        
        if (boxes == null) {
            return;
        }

        gc.setStroke(Color.BLUE);
        boolean resetColor = false;

        for (TessBox box : boxes.toList()) {
            if (box.isSelected()) {
                gc.setLineWidth(2);
                gc.setStroke(Color.RED);
                resetColor = true;
            }
            Rectangle2D rect = box.getRect();
            gc.strokeRect(rect.getMinX(), rect.getMinY(), rect.getWidth(), rect.getHeight());
            if (resetColor) {
                gc.setLineWidth(1);
                gc.setStroke(Color.BLUE);
                resetColor = false;
            }
        }
    }

    public void setImage(Image image) {
        this.image = image;
        this.setWidth(image.getWidth());
        this.setHeight(image.getHeight());
    }

    public void setBoxes(TessBoxCollection boxes) {
        this.boxes = boxes;
        //paint();
    }
    
    public void setFont(Font font) {
        this.font = net.sourceforge.tessboxeditor.utilities.Utils.deriveFont(font, "", 24);
    }

    /**
     * @param table the table to set
     */
    public void setTable(TableView table) {
        this.tableView = table;
    }

    /**
     * @return the boxClickAction
     */
    public boolean isBoxClickAction() {
        return boxClickAction;
    }
}
