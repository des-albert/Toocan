package com.db;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;


public class ToocanController {

    @FXML
    Label labelBaseStatus;

    private static final String dbURL = "jdbc:derby:dbParts";
    private static final String username = "PARTS";
    private static final String password = "working";

    static Statement sr,st;

    public void initialize() {
        connection();
    }
    public void ButtonBaseQuitOnAction() {
        Platform.exit();
    }
    public void ButtonPartManagerOnAction() {
        try {
            FXMLLoader fxmlFormLoader = new FXMLLoader(getClass().getResource("Parts.fxml"));
            Parent partForm = fxmlFormLoader.load();
            Stage baseStage = new Stage();
            baseStage.setTitle("db Parts");
            baseStage.setScene(new Scene(partForm, 1200, 900));
            baseStage.show();
        } catch (IOException ex) {
            labelBaseStatus.setText(ex.getMessage());
        }
    }
    public void  ButtonBuildOnAction () {
        try {
            FXMLLoader fxmlFormLoader = new FXMLLoader(getClass().getResource("Builder.fxml"));
            Parent buildForm = fxmlFormLoader.load();
            Stage buildStage = new Stage();
            buildStage.setTitle("db Builder");
            buildStage.setScene(new Scene(buildForm, 1100,850));
            buildStage.show();

        } catch (IOException ex) {
            labelBaseStatus.setText(ex.getMessage());
        }
    }

    /* Open Derby Database */

    private void connection() {

        try {
            Connection dataConnection = DriverManager.getConnection(dbURL, username, password);
            sr = dataConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            st = dataConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            labelBaseStatus.setText("Database Open");
        } catch (Exception ex ){
            labelBaseStatus.setText(ex.getMessage());
        }
    }

}
