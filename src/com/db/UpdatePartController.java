package com.db;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;


import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.db.PartsController.*;
import static com.db.ToocanController.sr;
import static com.db.ToocanController.st;

public class UpdatePartController {

    @FXML
    Button ButtonUpdatePartClose, ButtonUpdatePart;
    @FXML
    TextField TextPartCode, TextPartDescription, TextPartCat, TextTotalCount;
    @FXML
    ComboBox<String> comboTotal;
    @FXML
    Label LabelUpdateStatus;
    private String code;
    private NodeClass node;

    public void initialize() {
        TextPartCode.setEditable(false);
        node = selectItem.getValue();
        code = node.Code;
        TextPartCode.setText(code);
        TextPartDescription.setText(node.Description);
        TextPartDescription.setEditable(true);
        TextPartCat.setText(node.Category);
        TextPartCat.setEditable(true);

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
            LabelUpdateStatus.setText(ex.getMessage());
        }
        SQL = "SELECT NAME, INCREMENT FROM SUMMARY WHERE PART_CODE = '" + code + "'";
        try {
            ResultSet rs = sr.executeQuery(SQL);
            if (rs.next()) {
                String totalName = rs.getString("NAME");
                comboTotal.getItems().addAll(totalName);
                comboTotal.getSelectionModel().select(totalName);
                comboTotal.setEditable(true);
                TextTotalCount.setText(Integer.toString(rs.getInt("INCREMENT")));

                TextTotalCount.setEditable(true);
            }
        } catch (SQLException ex) {
            LabelUpdateStatus.setText(ex.getMessage());
        }
    }

    public void ButtonUpdatePartCloseAction() {
        Stage stage = (Stage) ButtonUpdatePartClose.getScene().getWindow();
        stage.close();
    }

    private String makeKey(String a, String b) {
        return a + '~' + b;
    }

    public void ButtonUpdatePartAction() {

        String description = TextPartDescription.getText();
        String category = TextPartCat.getText();
        String SQL = "UPDATE PART SET DESCRIPTION = '" +  description + "', CATEGORY = '" +
                category + "' WHERE CODE = '" + code + "'";
        try {
            st.executeUpdate(SQL);
        }
        catch (SQLException ex) {
            LabelUpdateStatus.setText(ex.getMessage());
        }

        /* Update Part Tree */

        TreeItem<NodeClass> parentItem;

        if (! node.Category.equals(category)) {
            node.Category = category;
            if ( (parentItem = partHash.get(category)) == null) {
                NodeClass parent = new NodeClass(category);
                parentItem = new TreeItem<>(parent);
                partHash.put(category, parentItem);
                partItemRoot.getChildren().add(parentItem);
            }
            InputStream iconStream = getClass().getResourceAsStream("/img/" + category  + ".png");
            if (iconStream != null) {
                Image icon = new Image(iconStream);
                selectItem.setGraphic(new ImageView(icon));
            }
            selectItem.getParent().getChildren().remove(selectItem);
            parentItem.getChildren().add(selectItem);

        }
        selectItem.setValue(node);

        /* Update Slot Tree */

        if( ! node.Description.equals(description)) {
            node.Description = description;
            SQL = "SELECT SLOT_NAME FROM LINK WHERE DIR = 'M' AND PART_CODE = '" + code + "'";
            try {
                ResultSet rs = sr.executeQuery(SQL);
                while (rs.next()) {
                    String key = makeKey(code, rs.getString("SLOT_NAME"));
                    TreeItem<NodeClass> slotItem = slotHash.get(key);
                    NodeClass slotPart = slotItem.getValue();
                    slotPart.Description = description;
                }
            }
            catch (SQLException ex) {
                LabelUpdateStatus.setText(ex.getMessage());
            }

        }
        LabelUpdateStatus.setText("Part Update Success");

    }
}
