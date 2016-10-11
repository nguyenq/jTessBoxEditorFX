/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sourceforge.tessboxeditor;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

public class MenuRecentFilesController implements Initializable {

    @FXML
    protected Menu menuRecentFiles;

    protected ResourceBundle bundle;
    final Preferences prefs = MainController.prefs;

    private final java.util.List<String> mruList = new java.util.ArrayList<String>();
    private String strClearRecentFiles;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        bundle = ResourceBundle.getBundle("net.sourceforge.tessboxeditor.Gui"); // NOI18N
        populateMRUList();
    }

    /**
     * Populates MRU List when the program first starts.
     */
    void populateMRUList() {
        String[] fileNames = prefs.get("MruList", "").split(File.pathSeparator);

        for (String fileName : fileNames) {
            if (!fileName.equals("")) {
                mruList.add(fileName);
            }
        }
        updateMRUMenu();
    }

    /**
     * Update MRU List after open or save file.
     *
     * @param fileName
     */
    public void updateMRUList(String fileName) {
        if (mruList.contains(fileName)) {
            mruList.remove(fileName);
        }
        mruList.add(0, fileName);

        if (mruList.size() > 10) {
            mruList.remove(10);
        }

        updateMRUMenu();
    }

    /**
     * Update MRU Submenu.
     */
    private void updateMRUMenu() {
        this.menuRecentFiles.getItems().clear();

        if (mruList.isEmpty()) {
            this.menuRecentFiles.getItems().add(new MenuItem(bundle.getString("No_Recent_Files")));
        } else {
            EventHandler<ActionEvent> mruAction = (ActionEvent e) -> {
                MenuItem item = (MenuItem) e.getSource();
                String fileName = item.getText();

                if (fileName.equals(strClearRecentFiles)) {
                    mruList.clear();
                    menuRecentFiles.getItems().clear();
                    menuRecentFiles.getItems().add(new MenuItem(bundle.getString("No_Recent_Files")));
                } else {
                    MainController.getInstance().openFile(new File(fileName));
                }
            };

            for (String fileName : mruList) {
                MenuItem item = new MenuItem(fileName);
                this.menuRecentFiles.getItems().add(item);
                item.setOnAction(mruAction);
            }
            this.menuRecentFiles.getItems().add(new SeparatorMenuItem());
            strClearRecentFiles = bundle.getString("Clear_Recent_Files");
            MenuItem miClear = new MenuItem(strClearRecentFiles);
            miClear.setMnemonicParsing(true);
            this.menuRecentFiles.getItems().add(miClear);
            miClear.setOnAction(mruAction);
        }
    }

    public void savePrefs() {
        StringBuilder buf = new StringBuilder();
        for (String item : this.mruList) {
            buf.append(item).append(File.pathSeparatorChar);
        }
        prefs.put("MruList", buf.toString());
    }
}
