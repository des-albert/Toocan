package com.db;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;


import static com.db.BuildController.menuItem;

public class SlotController {

    @FXML
    Button buttonSlotClose;
    @FXML
    TableView<SlotClass> slotTableView;
    @FXML
    TableColumn<SlotClass, String> columnSlotName, columnSlotType;
    @FXML
    TableColumn<SlotClass, Integer> columnSlotMaxCount,  columnSlotContents;

    private ObservableList<SlotClass> data = FXCollections.observableArrayList();

    public void  initialize() {

        columnSlotName.setCellValueFactory(new PropertyValueFactory<>("slotName"));
        columnSlotMaxCount.setCellValueFactory(new PropertyValueFactory<>("slotMax"));
        columnSlotType.setCellValueFactory(new PropertyValueFactory<>("slotType"));
        columnSlotContents.setCellValueFactory(new PropertyValueFactory<>("slotContents"));

        columnSlotMaxCount.setStyle("-fx-alignment: CENTER-RIGHT;");
        columnSlotType.setStyle("-fx-alignment: CENTER;");
        columnSlotContents.setStyle("-fx-alignment: CENTER-RIGHT;");

        PartClass slotNode = menuItem.getValue();
        int  num_slots = slotNode.slotCount;
        if (num_slots > 0) {
            slotTableView.setItems(data);
            for (int i = 0; i < num_slots; i++) {
                int max = slotNode.slotMax[i];
                String slotType = "M";
                if (max < 0) {
                    max = -max;
                    slotType = "E";
                }
                SlotClass row = new SlotClass(slotNode.slotName[i], max, slotType, slotNode.slotContents[i]);
                data.add(row);
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Part has no slots");
            alert.showAndWait();
            Stage stage = (Stage) buttonSlotClose.getScene().getWindow();
            stage.close();
        }

    }
    public void ButtonSlotCloseOnAction() {
        Stage stage = (Stage) buttonSlotClose.getScene().getWindow();
        stage.close();
    }


}
