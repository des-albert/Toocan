package com.db;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;

import static com.db.ToocanController.sr;
import static com.db.PartsController.*;

public class AddSlotController {

    @FXML
    Button ButtonAddSlot, ButtonAddSlotDone;
    @FXML
    TextField TextSlotName, TextSlotCount;
    @FXML
    RadioButton RadioButtonMax, RadioButtonExact, RadioButtonRequired, RadioButtonUnlimited;
    @FXML
    ToggleGroup CountGroup;
    @FXML
    Label LabelAddSlotStatus;

    int slotCount;

    public void initialize() {

    }
    public void ButtonAddSlotDoneOnAction() {
        Stage stage = (Stage) ButtonAddSlotDone.getScene().getWindow();
        stage.close();
    }
    public void ButtonAddSlotOnAction() {

        String countType = CountGroup.getSelectedToggle().getUserData().toString();
        String slotName = TextSlotName.getText();
        if (slotName.isEmpty() || countType.isEmpty() && ! RadioButtonUnlimited.isSelected()  || TextSlotName.getText().isEmpty()) {
            LabelAddSlotStatus.setText("Fields must be NOT NULL");
            return;
        }
        try {
        if (! RadioButtonUnlimited.isSelected())
            slotCount = Integer.parseInt(TextSlotCount.getText());
        else
            slotCount = 0;

            String SQL = "INSERT INTO SLOT (NAME, CONTENT, SELECTOR) VALUES  ('" +
                slotName + "', " + slotCount  + ", '" + countType + "')";
            sr.executeUpdate(SQL);
            String code = selectItem.getValue().Code;
            SQL = "INSERT INTO Link (PART_CODE, SLOT_NAME, DIR) VALUES ('" + code  +
                "', '" + slotName + "', 'F')";
            sr.executeUpdate(SQL);
            NodeClass node = new NodeClass(code, slotName, slotCount, countType);
            TreeItem<NodeClass> nodeItem = new TreeItem<>(node);
            slotItemRoot.getChildren().add(nodeItem);
            slotHash.put(slotName, nodeItem);

            node = new NodeClass("", slotName, slotCount, countType);
            nodeItem = new TreeItem<>(node);
            selectItem.getChildren().add(nodeItem);
            String key = code + "@" + slotName;
            partHash.put(key, nodeItem);

            LabelAddSlotStatus.setText("Slot Addition Success");

        }
        catch(SQLException ex) {
            LabelAddSlotStatus.setText(ex.getMessage());
        }

        catch (NumberFormatException ex){
            LabelAddSlotStatus.setText("Non Integer Count Input");
        }
    }
}