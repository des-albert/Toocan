package com.db;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import static com.db.ToocanController.sr;

public class PartsController {
    static TreeItem<NodeClass> selectItem;
    static HashMap<String, TreeItem<NodeClass>> partHash;
    static HashMap<String, TreeItem<NodeClass>> slotHash;
    static TreeItem<NodeClass> partItemRoot, slotItemRoot;
    @FXML
    public Label labelPartStatus;
    @FXML
    ImageView imageViewTrash;
    @FXML
    Button buttonQuitParts;
    @FXML
    TreeView<NodeClass> treeViewSlots, treeViewParts;
    private TreeItem<NodeClass> parentItem;
    private Stage partStage, slotStage;
    private ContextMenu folderContext = new ContextMenu();
    private ContextMenu partContext = new ContextMenu();
    private Dragboard db;
    private ClipboardContent content;
    private ResultSet rs;

    @FXML
    private void partDragDetected(MouseEvent event) {
        NodeClass node = treeViewParts.getSelectionModel().getSelectedItem().getValue();
        db = treeViewParts.startDragAndDrop(TransferMode.COPY);
        content = new ClipboardContent();
        if (node.NodeType == 'P')
            content.put(DataFormat.PLAIN_TEXT, node.Code);
        else
            content.put(DataFormat.PLAIN_TEXT, makeKey(node.Code,node.Name));
        content.put(DataFormat.HTML, "Part");
        db.setContent(content);
        event.consume();
    }

    @FXML
    private void slotDragDetected(MouseEvent event) {
        NodeClass node = treeViewSlots.getSelectionModel().getSelectedItem().getValue();
        db = treeViewSlots.startDragAndDrop(TransferMode.COPY);
        content = new ClipboardContent();
        if (node.NodeType == 'P')
            content.put(DataFormat.PLAIN_TEXT, makeKey(node.Code,node.Name));   // Part Drag n Drop
        else
            content.put(DataFormat.PLAIN_TEXT, node.Name); //Slot Drag n Drop
        content.put(DataFormat.HTML, "Slot");
        db.setContent(content);
        event.consume();
    }

    private void deleteLeaf(HashMap<String, TreeItem<NodeClass>> hash, String code, String name) {
        String key = makeKey(code, name);
        TreeItem<NodeClass> item = hash.get(key);
        item.getParent().getChildren().remove(item);
        hash.remove(key, item);
    }

    private void deleteBranch(HashMap<String, TreeItem<NodeClass>> hash, String key) {
        TreeItem<NodeClass> item = hash.get(key);
        item.getParent().getChildren().remove(item);
        hash.remove(key, item);
    }

    public void initialize() {

        partHash = new HashMap<>();
        slotHash = new HashMap<>();

        /* Parts TreeView setup */

        NodeClass partNodeRoot = new NodeClass("Part");
        partItemRoot = new TreeItem<>(partNodeRoot);
        treeViewParts.setRoot(partItemRoot);
        treeViewParts.setShowRoot(false);

        /* Slots TreeView setup */

        NodeClass slotNodeRoot = new NodeClass("Slot");
        slotItemRoot = new TreeItem<>(slotNodeRoot);
        treeViewSlots.setRoot(slotItemRoot);
        treeViewSlots.setShowRoot(false);

        /* Parts TreeView cellFactory */

        treeViewParts.setCellFactory(cellData -> {
            final Tooltip tooltip = new Tooltip();
            TreeCell<NodeClass> cell = new TreeCell<>() {

                @Override
                protected void updateItem(NodeClass p, boolean empty) {
                    super.updateItem(p, empty);
                    if (empty || p == null) {
                        setText(null);
                        setGraphic(null);
                        setTooltip(null);
                    } else {
                        switch (p.NodeType) {
                            case 'F':
                                setText(p.Description);
                                setStyle("-fx-text-fill: red");
                                tooltip.setText("Category");
                                setContextMenu(folderContext);
                                break;
                            case 'P':
                                if (p.Increment != 0)
                                    tooltip.setText(p.Code + " -> " + p.SummaryName + " - " + p.Increment);
                                else
                                    tooltip.setText(p.Code);
                                setText(p.Description);
                                setStyle("-fx-text-fill: green");
                                setContextMenu(partContext);
                                break;
                            case 'S':
                                setText(p.Name);
                                setStyle("-fx-text-fill: blue");
                                tooltip.setText(p.SlotType + " - " + p.MaxContent);
                                break;
                        }
                        setTooltip(tooltip);
                        setGraphic(getTreeItem().getGraphic());
                    }
                }
            };
            cell.setOnDragOver(event -> {
                event.acceptTransferModes(TransferMode.COPY);
                event.consume();
            });

            cell.setOnDragDropped(event -> {
                String dragNode = event.getDragboard().getString();
                parentItem = cell.getTreeItem();
                TreeItem<NodeClass> nodeItem = slotHash.get(dragNode);
                NodeClass node = nodeItem.getValue().deepClone();
                nodeItem = new TreeItem<>(node);
                String SQL = "INSERT INTO LINK (SLOT_NAME, PART_CODE, DIR) VALUES ('" +
                        node.Name + "', '" + parentItem.getValue().Code + "', 'F')";
                try {
                    sr.executeUpdate(SQL);
                } catch (SQLException ex) {
                    labelPartStatus.setText(ex.getMessage());
                }
                parentItem.getChildren().add(nodeItem);
                String key = makeKey(parentItem.getValue().Code,node.Name);
                partHash.put(key, nodeItem);
                labelPartStatus.setText("Slot Added to Part");

                event.setDropCompleted(true);
                event.consume();
            });
            return cell;
        });

        treeViewSlots.setCellFactory(cellData -> {
            final Tooltip tooltip = new Tooltip();
            TreeCell<NodeClass> cell = new TreeCell<>() {

                @Override
                protected void updateItem(NodeClass p, boolean empty) {
                    super.updateItem(p, empty);
                    if (empty || p == null) {
                        setText(null);
                        setGraphic(null);
                        setTooltip(null);
                    } else {
                        switch (p.NodeType) {
                            case 'P':
                                if (p.Increment != 0)
                                    tooltip.setText(p.Code + " -> " + p.SummaryName + " - " + p.Increment);
                                else
                                    tooltip.setText(p.Code);
                                setText(p.Description);
                                setStyle("-fx-text-fill: green");
                                break;
                            case 'S':
                                setText(p.Name);
                                setStyle("-fx-text-fill: blue");
                                tooltip.setText(p.SlotType + " - " + p.MaxContent);
                                break;
                        }
                        setTooltip(tooltip);
                        setGraphic(getTreeItem().getGraphic());
                    }
                }
            };
            cell.setOnDragOver(event -> {
                event.acceptTransferModes(TransferMode.COPY);
                event.consume();
            });

            cell.setOnDragDropped(event -> {
                String dragNode = event.getDragboard().getString();
                parentItem = cell.getTreeItem();
                if(parentItem.getValue().NodeType == 'S') {
                    TreeItem<NodeClass> nodeItem = partHash.get(dragNode);
                    NodeClass node = nodeItem.getValue().deepClone();
                    nodeItem = new TreeItem<>(node);
                    String SQL = "INSERT INTO LINK (SLOT_NAME, PART_CODE, DIR) VALUES ('" +
                            parentItem.getValue().Name + "', '" + node.Code + "', 'M')";
                    try {
                        sr.executeUpdate(SQL);
                    } catch (SQLException ex) {
                        labelPartStatus.setText(ex.getMessage());
                    }
                    parentItem.getChildren().add(nodeItem);
                    String key = makeKey(node.Code, parentItem.getValue().Name);
                    slotHash.put(key, nodeItem);
                    labelPartStatus.setText("Part Added to Slot");

                }
                event.setDropCompleted(true);
                event.consume();

            });
            return cell;
        });

        showParts();
        showSlots();

        /* Drag to Trash */

        imageViewTrash.setOnDragOver(event -> {
            event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });

        imageViewTrash.setOnDragDropped(event -> {
            TreeItem<NodeClass> nodeItem;
            NodeClass node;
            String dragNode = event.getDragboard().getString();
            String source = event.getDragboard().getHtml();
            try {
                switch (source) {
                    case "Slot":
                        nodeItem = slotHash.get(dragNode);
                        node = nodeItem.getValue();
                        switch (node.NodeType) {
                            case 'P':                      // Remove Part from selected Slot
                                deleteLeaf(slotHash, node.Code, node.Name);
                                String SQL = "DELETE FROM LINK WHERE PART_CODE = '" + node.Code + "' AND DIR = 'M'";
                                sr.executeUpdate(SQL);
                                labelPartStatus.setText("Part Removed from Slot ");
                                break;

                            case 'S':                      // Delete Slot and links
                                SQL = "SELECT PART_CODE FROM LINK WHERE SLOT_NAME = '" + node.Name + "' AND DIR = 'M'";
                                rs = sr.executeQuery(SQL);
                                while (rs.next()) {
                                    String code = rs.getString("PART_CODE");
                                    deleteLeaf(slotHash, code, node.Name);
                                }
                                deleteBranch(slotHash, node.Name);

                                SQL = "SELECT PART_CODE FROM LINK WHERE SLOT_NAME = '" + node.Name + "' AND DIR = 'F'";
                                rs = sr.executeQuery(SQL);
                                while (rs.next()) {
                                    String code = rs.getString("PART_CODE");
                                    deleteLeaf(partHash, code, node.Name);
                                }

                                SQL = "DELETE FROM LINK WHERE SLOT_NAME = '" + node.Name + "'";
                                sr.executeUpdate(SQL);
                                SQL = "DELETE FROM SLOT WHERE NAME = '" + node.Name + "'";
                                sr.executeUpdate(SQL);
                                labelPartStatus.setText("Slot Deleted ");
                                break;
                        }
                        break;

                    case "Part":
                        nodeItem = partHash.get(dragNode);
                        node = nodeItem.getValue();
                        switch (node.NodeType) {
                            case 'P':                       //  Delete Part and Slots
                                String SQL = "SELECT SLOT_NAME FROM LINK WHERE PART_CODE = '" + node.Code + "'";
                                rs = sr.executeQuery(SQL);
                                while (rs.next()) {
                                    String name = rs.getString("SLOT_NAME");
                                    deleteLeaf(slotHash, node.Code, name);
                                }
                                deleteBranch(partHash, node.Code);
                                SQL = "DELETE FROM LINK WHERE PART_CODE = '" + node.Code + "'";
                                sr.executeUpdate(SQL);
                                labelPartStatus.setText("Part Deleted");
                                break;
                            case 'S':                       //  Remove Slot from Part
                                deleteLeaf(partHash, node.Code, node.Name);
                                SQL = "DELETE FROM LINK WHERE SLOT_NAME = '" + node.Name + "' AND DIR ='F'";
                                sr.executeUpdate(SQL);
                                labelPartStatus.setText("Slot removed from Part");
                                break;
                        }
                }
            } catch (SQLException ex) {
                labelPartStatus.setText(ex.getMessage());
            }
            event.setDropCompleted(true);
            event.consume();
        });

        /* Add Parts to Folder  - AddPartController */

        MenuItem addPartMenu = new MenuItem("New Part");
        addPartMenu.setOnAction(e -> {
            selectItem = treeViewParts.getSelectionModel().getSelectedItem();
            try {
                FXMLLoader fxmlFormLoader = new FXMLLoader(getClass().getResource("AddPart.fxml"));
                Parent partForm = fxmlFormLoader.load();
                partStage = new Stage();
                partStage.setTitle("Add Part");
                partStage.setScene(new Scene(partForm));
                partStage.show();
            } catch (IOException ex) {
                labelPartStatus.setText(ex.getMessage());
            }
        });
        folderContext.getItems().add(addPartMenu);

        MenuItem updatePartMenu = new MenuItem("Update Part");
        updatePartMenu.setOnAction(e -> {
            selectItem = treeViewParts.getSelectionModel().getSelectedItem();
            try {
                FXMLLoader fxmlFormLoader = new FXMLLoader(getClass().getResource("UpdatePart.fxml"));
                Parent partForm = fxmlFormLoader.load();
                partStage = new Stage();
                partStage.setTitle("Update Part");
                partStage.setScene(new Scene(partForm));
                partStage.show();
            } catch (IOException ex) {
                labelPartStatus.setText(ex.getMessage());
            }

        });
        partContext.getItems().add(updatePartMenu);


        /* New Slot to a Part */

        MenuItem addSlotMenu = new MenuItem("Add New Slot to Part");
        addSlotMenu.setOnAction(e -> {
            selectItem = treeViewParts.getSelectionModel().getSelectedItem();
            try {
                FXMLLoader fxmlFormLoader = new FXMLLoader(getClass().getResource("AddSlot.fxml"));
                Parent slotForm = fxmlFormLoader.load();
                slotStage = new Stage();
                slotStage.setTitle("Add Slot");
                slotStage.setScene(new Scene(slotForm));
                slotStage.show();
            } catch (IOException ex) {
                labelPartStatus.setText(ex.getMessage());
            }
        });
        partContext.getItems().add(addSlotMenu);
    }

    private void showParts() {

        /* create root of Part Tree */

        try {
            String SQL = "SELECT CODE, DESCRIPTION, CATEGORY FROM PART ORDER BY CATEGORY, DESCRIPTION";
            rs = sr.executeQuery(SQL);
            while (rs.next()) {
                String category = rs.getString("CATEGORY");
                String code = rs.getString("CODE");
                if ((parentItem = partHash.get(category)) == null) {
                    NodeClass parent = new NodeClass(category);
                    parentItem = new TreeItem<>(parent);
                    partHash.put(category, parentItem);
                    partItemRoot.getChildren().add(parentItem);
                }
                NodeClass node = new NodeClass(code, rs.getString("DESCRIPTION"),
                        rs.getString("CATEGORY"), "");
                String iconPath = "/img/" + category + ".png";
                TreeItem<NodeClass> nodeItem;
                InputStream iconStream = getClass().getResourceAsStream(iconPath);
                if (iconStream != null) {
                    Image icon = new Image(getClass().getResourceAsStream(iconPath));
                    nodeItem = new TreeItem<>(node, new ImageView(icon));
                }
                else
                    nodeItem = new TreeItem<>(node);
                partHash.put(code, nodeItem);
                parentItem.getChildren().add(nodeItem);
            }
            rs.close();
            SQL = "SELECT NAME, INCREMENT, PART_CODE FROM SUMMARY";
            rs = sr.executeQuery(SQL);
            while (rs.next()) {
                TreeItem<NodeClass> nodeItem = partHash.get(rs.getString("PART_CODE"));
                NodeClass node = nodeItem.getValue();
                node.SummaryName = rs.getString("NAME");
                node.Increment = rs.getInt("INCREMENT");
            }
            rs.close();
        } catch (SQLException ex) {
            labelPartStatus.setText(ex.getMessage());
        }
    }

    private void showSlots() {
        String SQL = "SELECT NAME, CONTENT, SELECTOR, PART_CODE, SLOT_NAME FROM SLOT INNER JOIN LINK" +
                " ON NAME = SLOT_NAME WHERE DIR = 'F' ORDER BY NAME";
        try {
            rs = sr.executeQuery(SQL);
            while (rs.next()) {
                String code = rs.getString("PART_CODE");
                String name = rs.getString("NAME");
                if ((parentItem = partHash.get(code)) != null) {
                    NodeClass node = new NodeClass(code, rs.getString("NAME"), rs.getInt("CONTENT"),
                            rs.getString("SELECTOR"));
                    TreeItem<NodeClass> nodeItem = new TreeItem<>(node);
                    String key = makeKey(code, name);
                    partHash.put(key, nodeItem);
                    parentItem.getChildren().add(nodeItem);
                }
            }
            rs.close();
            SQL = "SELECT NAME, CONTENT, SELECTOR FROM Slot ORDER BY NAME";
            rs = sr.executeQuery(SQL);
            while (rs.next()) {
                String name = rs.getString("NAME");
                NodeClass node = new NodeClass("", rs.getString("NAME"), rs.getInt("CONTENT"),
                        rs.getString("SELECTOR"));
                TreeItem<NodeClass> nodeItem = new TreeItem<>(node);
                slotHash.put(name, nodeItem);
                slotItemRoot.getChildren().add(nodeItem);
            }
            rs.close();

            /* Add Parts to Slots */

            SQL = "SELECT CODE, DESCRIPTION, CATEGORY, SLOT_NAME FROM PART INNER JOIN LINK ON CODE = PART_CODE " +
                    "WHERE DIR = 'M' ORDER BY DESCRIPTION";
            rs = sr.executeQuery(SQL);
            while (rs.next()) {
                String name = rs.getString("SLOT_NAME");
                String code = rs.getString("CODE");
                if ((parentItem = slotHash.get(name)) != null) {
                    NodeClass node = new NodeClass(code, rs.getString("DESCRIPTION"),
                            rs.getString("CATEGORY"), rs.getString("SLOT_NAME"));
                    TreeItem<NodeClass> nodeItem = new TreeItem<>(node);
                    String key = makeKey(code, name);
                    slotHash.put(key, nodeItem);
                    parentItem.getChildren().add(nodeItem);
                }
            }
        } catch (SQLException ex) {
            labelPartStatus.setText(ex.getMessage());
        }
    }

    private String makeKey(String a, String b) {
        return a + '~' + b;
    }

    public void ButtonQuitPartsOnAction() {
        Stage stage = (Stage) buttonQuitParts.getScene().getWindow();
        stage.close();
    }
    public void ButtonPartCollapseOnAction() {
        for (TreeItem<NodeClass> node : partItemRoot.getChildren()) {
            node.setExpanded(false);
        }
    }
    public void ButtonSlotCollapseOnAction() {
        for (TreeItem<NodeClass> node : slotItemRoot.getChildren()) {
            node.setExpanded(false);
        }

    }
}
