## jTessBoxEditorFX

A box editor and trainer for Tesseract OCR, providing editing of box data of both Tesseract 2.0x and 3.0x formats and full automation of Tesseract training. It can read images of common image formats, including multi-page TIFF.

Note: LSTM Training for Tesseract 4.0x is not supported.

jTessBoxEditorFX is jTessBoxEditor rewritten in JavaFX to address the current issue of rendering complex scripts existing in Java Swing. The program requires Java Runtime Environment 8u40 or later.

jTessBoxEditorFX is released and distributed under the [Apache License, v2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Features

- Capable of rendering complex scripts
- Tesseract Windows training executable 5.3.3 bundled

## System requirements

[Java 21](https://www.oracle.com/java/technologies/downloads/) and [JavaFX 21](https://gluonhq.com/products/javafx/).

## Command line

Windows:

set PATH_TO_FX="C:\Program Files\Java\javafx-sdk-21.0.1\lib"

java -Xms128m -Xmx1024m --module-path %PATH_TO_FX% --add-modules javafx.controls,javafx.fxml,javafx.web -jar jTessBoxEditorFX.jar

Linux/Mac:

export PATH_TO_FX=path/to/javafx-sdk-21.0.1/lib

java -Xms128m -Xmx1024m --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml,javafx.web -jar jTessBoxEditorFX.jar

