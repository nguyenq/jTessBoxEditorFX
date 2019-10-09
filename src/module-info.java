/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
module JTessBoxEditorFX {
    requires javafx.swt;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.media;
    requires javafx.swing;
    requires javafx.web;
    requires java.logging;
    requires java.prefs;

//    requires controlsfx;
//    requires src;
    
    exports net.sourceforge.tessboxeditor to javafx.graphics;
    opens net.sourceforge.tessboxeditor to javafx.fxml;
}
