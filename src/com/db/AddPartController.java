package com.db;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.db.ToocanController.sr;
import static com.db.PartsController.*;


public class AddPartController {

    @FXML
    Button ButtonAddPartDone, ButtonAddPart;
    @FXML
    Label LabelAddStatus;
    @FXML
    TextField TextPartCode, TextPartDescription, TextPartCat, TextTotalCount;
    @FXML
    ComboBox<String> comboTotal;

    public void initialize() {

        TextPartCat.setText(selectItem.getValue().Description);
        LabelAddStatus.setText("");
        comboTotal.setEditable(true);

        String SQL = "SELECT DISTINCT NAME FROM SUMMARY";
        String[] totals;
        try {
            ResultSet rs = sr.executeQuery(SQL);
            rs.last();
            totals = new String[rs.getRow()];
            rs.beforeFirst();

            int i = 0;
            while (rs.next()) {
                totals[i++] = rs.getString("NAME");
            }
            comboTotal.getItems().addAll(totals);
        }
        catch(SQLException ex){
            LabelAddStatus.setText(ex.getMessage());
        }

    }
    public void ButtonAddPartAction() {

        String totalText = comboTotal.getEditor().getText();
        String description = TextPartDescription.getText();
        String category = TextPartCat.getText();
        String code = TextPartCode.getText();

        if (code.isEmpty() || description.isEmpty() || category.isEmpty() ) {
            LabelAddStatus.setText("All Fields are NOT NULL");
            return;
        }

        String SQL = "INSERT INTO PART (CODE, DESCRIPTION, CATEGORY) VALUES ('" +
                    code + "', '" + description + "', '" + category + "')";

        try {
            sr.executeUpdate(SQL);
            TreeItem<NodeClass> parentItem = selectItem;
            if ( partHash.get(category) == null) {
                NodeClass parent = new NodeClass(category);
                parentItem = new TreeItem<>(parent);
                partHash.put(category, parentItem);
                partItemRoot.getChildren().add(parentItem);
            }
            NodeClass node = new NodeClass(code, TextPartDescription.getText(), category,"" );
            TreeItem<NodeClass> nodeItem;
            InputStream iconStream = getClass().getResourceAsStream("/img/" + category  + ".png");
            if (iconStream != null) {
                Image icon = new Image(iconStream);
                nodeItem = new TreeItem<>(node, new ImageView(icon));
            }
            else
                nodeItem = new TreeItem<>(node);
            parentItem.getChildren().add(nodeItem);
            partHash.put(code, nodeItem);

            LabelAddStatus.setText("Part Addition Success");

            if (! totalText.isEmpty() ) {
                SQL = "INSERT INTO SUMMARY (NAME, INCREMENT, PART_CODE) VALUES ('" + totalText +
                        "', " + TextTotalCount.getText() + ", '" + code + "')" +
                        " EXCEPT SELECT NAME, INCREMENT, PART_CODE FROM SUMMARY";
                sr.executeUpdate(SQL);
            }

        }
        catch(SQLException ex){
            LabelAddStatus.setText(ex.getMessage());
        }

    }
    public void ButtonAddPartDoneAction() {
        Stage stage = (Stage) ButtonAddPartDone.getScene().getWindow();
        stage.close();
    }

}
