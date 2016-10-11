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
package net.sourceforge.tessboxeditor.datamodel;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

/**
 * Box data model.
 */
public class TessBox {

    private final SimpleStringProperty chrs;
    private Rectangle2D rect;
    private final SimpleIntegerProperty x;
    private final SimpleIntegerProperty y;
    private final SimpleIntegerProperty width;
    private final SimpleIntegerProperty height;
    private short page;
    private boolean selected;

    public TessBox(String chrs, Rectangle2D rect, short page) {
        this.chrs = new SimpleStringProperty(chrs);
        this.page = page;
        this.rect = rect;
        this.x = new SimpleIntegerProperty((int) rect.getMinX());
        this.y = new SimpleIntegerProperty((int) rect.getMinY());
        this.width = new SimpleIntegerProperty((int) rect.getWidth());
        this.height = new SimpleIntegerProperty((int) rect.getHeight());
    }

    /**
     * Whether the box is selected.
     *
     * @return the selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Select a box.
     *
     * @param selected the selected to set
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Whether the box contains a coordinate.
     *
     * @param x
     * @param y
     * @return
     */
    boolean contains(int x, int y) {
        return this.rect.contains(x, y);
    }

    /**
     * Whether the box contains a point.
     *
     * @param p
     * @return
     */
    boolean contains(Point2D p) {
        return this.rect.contains(p);
    }

    /**
     * A box information.
     *
     * @return
     */
    @Override
    public String toString() {
        return String.format("%s %.0f %.0f %.0f %.0f %d", chrs, rect.getMinX(), rect.getMinY(), rect.getMaxX(), rect.getMaxY(), page);
    }

    /**
     * Gets box bounding rectangle.
     *
     * @return the rectangle
     */
    public Rectangle2D getRect() {
        return this.rect;
    }

    /**
     * Sets box bounding rectangle.
     *
     * @param rect the rectangle to set
     */
    public void setRect(Rectangle2D rect) {
        this.rect = rect;
        this.x.set((int) rect.getMinX());
        this.y.set((int) rect.getMinY());
        this.width.set((int) rect.getWidth());
        this.height.set((int) rect.getHeight());
    }

    /**
     * Gets box character value.
     *
     * @return the chrs
     */
    public String getCharacter() {
        return chrs.get();
    }

    /**
     * Sets box character value.
     *
     * @param value the chrs to set
     */
    public void setCharacter(String value) {
        this.chrs.set(value);
    }

    public SimpleStringProperty characterProperty() {
        return chrs;
    }

    public int getX() {
        return this.x.get();
    }

    public SimpleIntegerProperty xProperty() {
        return this.x;
    }

    public int getY() {
        return this.y.get();
    }

    public SimpleIntegerProperty yProperty() {
        return this.y;
    }

    public int getWidth() {
        return this.width.get();
    }

    public SimpleIntegerProperty widthProperty() {
        return this.width;
    }

    public int getHeight() {
        return this.height.get();
    }

    public SimpleIntegerProperty heightProperty() {
        return this.height;
    }

    /**
     * Gets the page the box is in.
     *
     * @return the page
     */
    public short getPage() {
        return page;
    }

    /**
     * Sets the page the box is in.
     *
     * @param page the page to set
     */
    public void setPage(short page) {
        this.page = page;
    }
}
