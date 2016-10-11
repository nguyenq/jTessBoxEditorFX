/**
 * Copyright @ 2013 Quan Nguyen
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
package net.sourceforge.tessboxeditor.utilities;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class ImageUtils {

    /**
     * Adds noise to an image. Adapted from an algorithm in
     * http://www.gutgames.com/post/Adding-Noise-to-an-Image-in-C.aspx
     *
     * @param originalImage
     * @param amount
     * @return
     */
    public static BufferedImage addNoise(BufferedImage originalImage, int amount) {
        BufferedImage targetImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Random randomizer = new Random();
        int n = amount * 2 + 1;

        for (int x = 0; x < targetImage.getWidth(); ++x) {
            for (int y = 0; y < targetImage.getHeight(); ++y) {
                int rgb = originalImage.getRGB(x, y);
                Color color = new Color(rgb);
                // add random integers ranging from -amount to amount
                int r = color.getRed() + randomizer.nextInt(n) - amount;
                int g = color.getGreen() + randomizer.nextInt(n) - amount;
                int b = color.getBlue() + randomizer.nextInt(n) - amount;
                r = r > 255 ? 255 : r;
                r = r < 0 ? 0 : r;
                g = g > 255 ? 255 : g;
                g = g < 0 ? 0 : g;
                b = b > 255 ? 255 : b;
                b = b < 0 ? 0 : b;

                color = new Color(r, g, b);
                targetImage.setRGB(x, y, color.getRGB());
            }
        }

        return targetImage;
    }

    /**
     * Gets a subimage for display in boxview.
     *
     * @param image
     * @param rect
     * @param margin
     * @return
     */
    public static Image getSubimage(Image image, Rectangle2D rect, int margin) {
        double iconPosX = rect.getMinX();
        double iconPosY = rect.getMinY();

        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();

        double iconHeight = rect.getHeight();
        double iconWidth = rect.getWidth();

        double height = iconHeight + margin * 2;
        double width = iconWidth + margin * 2;

        while (width + iconPosX > image.getWidth() + 1) {
            width -= 1;
        }

        while (height + iconPosY > image.getHeight() + 1) {
            height -= 1;
        }

        WritableImage subImage = new WritableImage(image.getPixelReader(), (int) Math.max(0, Math.min(imageWidth - 1, iconPosX - margin)),
                (int) Math.max(0, Math.min(imageHeight - 1, iconPosY - margin)), (int) width, (int) height);

        return subImage;
    }

    /**
     * https://gist.github.com/jewelsea/5415891
     *
     * @param input
     * @param scaleFactor
     * @return
     */
    public static Image resample(Image input, int scaleFactor) {
        final int W = (int) input.getWidth();
        final int H = (int) input.getHeight();
        final int S = scaleFactor;

        WritableImage output = new WritableImage(
                W * S,
                H * S
        );

        PixelReader reader = input.getPixelReader();
        PixelWriter writer = output.getPixelWriter();

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                final int argb = reader.getArgb(x, y);
                for (int dy = 0; dy < S; dy++) {
                    for (int dx = 0; dx < S; dx++) {
                        writer.setArgb(x * S + dx, y * S + dy, argb);
                    }
                }
            }
        }

        return output;
    }
}
