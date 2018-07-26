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

import java.awt.Graphics2D;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import net.sourceforge.tessboxeditor.datamodel.TessBox;
import net.sourceforge.tessboxeditor.datamodel.TessBoxCollection;
import net.sourceforge.tessboxeditor.utilities.ImageUtils;
import net.sourceforge.tess4j.util.ImageIOHelper;
import static net.sourceforge.tessboxeditor.utilities.Utils.deriveFont;
import net.sourceforge.vietocr.util.Utils;

public class TiffBoxGeneratorFX {

    static final String EOL = System.getProperty("line.separator");
    private final List<List<String>> textPages;
    private final List<BufferedImage> imagePages = new ArrayList<>();
    private final List<TessBoxCollection> boxPages = new ArrayList<>();
    private final Font font;
    private int width, height;
    private int noiseAmount;
    private int margin = 100;
    private String fileName = "fontname.exp0";
    private File outputFolder;
    private final int COLOR_WHITE = java.awt.Color.WHITE.getRGB();
    private final int COLOR_BLACK = java.awt.Color.BLACK.getRGB();
    private float tracking = TextAttribute.TRACKING_LOOSE; // 0.04
    private int leading = 12;
    private boolean isAntiAliased;
    private final File baseDir = Utils.getBaseDir(TiffBoxGeneratorFX.this);
    private final TextFlow textFlow;

    private final static Logger logger = Logger.getLogger(TiffBoxGeneratorFX.class.getName());

    public TiffBoxGeneratorFX(List<List<String>> textPages, Font font, int width, int height) {
        this.textPages = textPages;
        this.font = deriveFont(font, font.getSize() * 4); // adjustment
        this.width = width;
        this.height = height;
        textFlow = new TextFlow();
        textFlow.setPrefWidth(width);
        textFlow.setPadding(new Insets(margin));
        textFlow.setStyle("-fx-letter-spacing: " + tracking); // no effect; not supported yet in JDK8u101
        textFlow.setLineSpacing(leading + 4); // adjustment
    }

    public void create() throws IOException {
        this.layoutPages();
        this.saveMultipageTiff();
        this.saveBoxFile();
    }

    String createFileName(Font font) {
        return font.getFamily().replace(" ", "").toLowerCase() + (font.getStyle().contains("Bold") ? "b" : "") + (font.getStyle().contains("Italic") ? "i" : "");
    }

    /**
     * Formats box content.
     *
     * @return
     */
    private String formatOutputString() {
        StringBuilder sb = new StringBuilder();
//        String combiningSymbols = readCombiningSymbols();
        for (short pageIndex = 0; pageIndex < imagePages.size(); pageIndex++) {
            TessBoxCollection boxCol = boxPages.get(pageIndex);
//            boxCol.setCombiningSymbols(combiningSymbols);
//            boxCol.combineBoxes();

            for (TessBox box : boxCol.toList()) {
                Rectangle2D rect = box.getRect();
                sb.append(String.format("%s %.0f %.0f %.0f %.0f %d", box.getCharacter(), rect.getMinX(), height - rect.getMinY() - rect.getHeight(), rect.getMinX() + rect.getWidth(), height - rect.getMinY(), pageIndex)).append(EOL);
            }
        }
//        if (isTess2_0Format) {
//            return sb.toString().replace(" 0" + EOL, EOL); // strip the ending zeroes
//        }
        return sb.toString();
    }

    /**
     * Gets bounding box of a Text node.
     *
     * @param text
     * @return bounding box
     */
    Bounds getBoundingBox(Text text) {
        Bounds tb = text.getBoundsInParent();
        Rectangle stencil = new Rectangle(tb.getMinX(), tb.getMinY(), tb.getWidth(), tb.getHeight());
        Shape intersection = Shape.intersect(text, stencil);
        Bounds ib = intersection.getBoundsInParent();
        return ib;
    }

    /**
     * Tightens bounding box in four directions b/c Java cannot produce bounding
     * boxes as tight as Tesseract can. Exam only the first pixel on each side.
     *
     * @param rect
     * @param bi
     */
    private Bounds tightenBoundingBox(Bounds rectShape, BufferedImage bi) {
        java.awt.Rectangle rect = new java.awt.Rectangle((int) rectShape.getMinX(), (int) rectShape.getMinY(), (int) Math.ceil(rectShape.getWidth()), (int) Math.ceil(rectShape.getHeight()));

        // left
        int endX = rect.x + 2;
        outerLeft:
        for (int x = rect.x; x < endX; x++) {
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                int color = bi.getRGB(x, y);
                if (color == COLOR_BLACK) {
                    break outerLeft;
                }
            }
            rect.x++;
//            rect.width--;
        }

        // right
        endX = rect.x + rect.width - 4;
        outerRight:
        for (int x = rect.x + rect.width; x > endX; x--) {
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                int color = bi.getRGB(x, y);
                if (color == COLOR_BLACK) {
                    break outerRight;
                }
            }
            rect.width = x - rect.x + 1;
        }

        //TODO: Need to account for Java's incorrect over-tightening the top of the bounding box
        // Need to move the top up by 1px and increase the height by 1px
        // top
        int endY = rect.y + 3;
        int startY = rect.y - 1;
        outerTop:
        for (int y = startY; y < endY; y++) {
            for (int x = rect.x; x < rect.x + rect.width; x++) {
                int color = bi.getRGB(x, y);
                if (color == COLOR_BLACK) {
                    if (y == startY) {
                        rect.y--;
//                        rect.height++;
                        continue outerTop;
                    } else {
                        break outerTop;
                    }
                }
            }
            if (y != startY) {
                rect.y++;
            }
        }

        // bottom
        endY = rect.y + rect.height - 4;
        outerBottom:
        for (int y = rect.y + rect.height - 1; y > endY; y--) {
            for (int x = rect.x; x < rect.x + rect.width; x++) {
                int color = bi.getRGB(x, y);
                if (color == COLOR_BLACK) {
                    break outerBottom;
                }
            }
            rect.height--;
        }

        return new BoundingBox(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * Creates box file.
     */
    private void saveBoxFile() {
        try {
            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputFolder, fileName + ".box")), StandardCharsets.UTF_8))) {
                out.write(formatOutputString());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Add Text nodes to TextFlow, which is one per page.
     */
    private void layoutPages() {
        boxPages.clear();
        imagePages.clear();

        Scene scene = new Scene(textFlow, width, height);

        for (List<String> textPage : textPages) {
            List<Text> texts = new ArrayList<Text>();
            textFlow.getChildren().clear();

            for (String ch : textPage) {
                // each ch can have multiple Unicode codepoints
                Text text = new Text(ch);
                text.setFont(font);
                texts.add(text);
            }

            textFlow.getChildren().addAll(texts);

//            StackPane pane = new StackPane();
//            pane.setAlignment(Pos.TOP_LEFT);
//            pane.getChildren().addAll(textFlow);
            // No need to show
//        Stage stage = new Stage();
//        stage.setTitle("TIFF/Boxes");
//        stage.setScene(scene);
//        stage.show();
            drawPage();
        }
    }

    /**
     * Takes snapshot of each text flow and store as <code>BufferedImage</code>.
     */
    private void drawPage() {
        BufferedImage bi;// = new BufferedImage(width, height, isAntiAliased ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_BYTE_BINARY);
        final SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(Color.WHITE);

        WritableImage snapshot = textFlow.snapshot(snapshotParameters, null);
        bi = SwingFXUtils.fromFXImage(snapshot, null);
        width = bi.getWidth();
        height = bi.getHeight();
        bi = redraw(bi);
        imagePages.add(bi);

        TessBoxCollection boxCol = new TessBoxCollection(); // for each page
        boxPages.add(boxCol);
        short pageNum = 0;
        List<Node> nodes = textFlow.getChildren();
        for (int i = 0; i < nodes.size(); i++) {
            Text text = (Text) nodes.get(i);
            String ch = text.getText();
            if (ch.length() == 0 || Character.isWhitespace(ch.charAt(0))) {
                // skip spaces
                continue;
            }

            // get bounding box for each character
            Bounds bounds = getBoundingBox(text);
//            System.out.println(bounds);
            if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
                // skip bad boxes
                continue;
            }

//            bounds = tightenBoundingBox(bounds, bi);
            boxCol.add(new TessBox(ch, new Rectangle2D(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight()), pageNum));
        }
    }

    /**
     * Reduces bit depth of 32bpp snapshot from to 8bpp or 1bpp depending on
     * anti-aliased mode selection.
     *
     * @param bi
     * @return
     */
    BufferedImage redraw(BufferedImage bi) {
        BufferedImage newImage = new BufferedImage(bi.getWidth(), bi.getHeight(), isAntiAliased ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2d = newImage.createGraphics();
        g2d.drawImage(bi, 0, 0, null);
        g2d.dispose();
        return newImage;
    }

    /**
     * Creates a multi-page TIFF image.
     */
    private void saveMultipageTiff() throws IOException {
        try {
            File tiffFile = new File(outputFolder, fileName + ".tif");
            tiffFile.delete();
            BufferedImage[] images = imagePages.toArray(new BufferedImage[imagePages.size()]);
            if (noiseAmount != 0) {
                for (int i = 0; i < images.length; i++) {
                    images[i] = ImageUtils.addNoise(images[i], noiseAmount);
                }
            }
            ImageIOHelper.mergeTiff(images, tiffFile, isAntiAliased ? "LZW" : "CCITT T.6");  // CCITT T.6 for bitonal; LZW for others);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Sets output filename.
     *
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        if (fileName != null && fileName.length() > 0) {
            int index = fileName.lastIndexOf(".");
            this.fileName = index > -1 ? fileName.substring(0, index) : fileName;
        }
    }

    /**
     * Sets letter tracking (letter spacing).
     *
     * @param tracking the tracking to set
     */
    public void setTracking(float tracking) {
        this.tracking = tracking;
        textFlow.setStyle("-fx-letter-spacing: " + tracking); // no effect; not supported yet in JDK8u91
    }

    /**
     * Sets leading (line spacing).
     *
     * @param leading the leading to set
     */
    public void setLeading(int leading) {
        this.leading = leading;
        textFlow.setLineSpacing(leading + 4); // adjustment
    }

    /**
     * Sets output folder.
     *
     * @param outputFolder the outputFolder to set
     */
    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    /**
     * Enables text anti-aliasing.
     *
     * @param enabled on or off
     */
    public void setAntiAliasing(boolean enabled) {
        this.isAntiAliased = enabled;
    }

    /**
     * Sets amount of noise to be injected to the generated image.
     *
     * @param noiseAmount the noiseAmount to set
     */
    public void setNoiseAmount(int noiseAmount) {
        this.noiseAmount = noiseAmount;
    }

    /**
     * Sets margin of text within image.
     *
     * @param margin the margin to set
     */
    public void setMargin(int margin) {
        this.margin = margin;
        textFlow.setPadding(new Insets(margin));
    }
}
