package com.db;

import com.google.gson.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.db.ToocanController.sr;
import static com.db.ToocanController.st;

public class BuildController {

    @FXML
    Button buttonClose;
    @FXML
    Label labelBuildStatus;
    @FXML
    TreeView<PartClass> treeViewParts, treeViewBuild;
    @FXML
    TableColumn<Summary, String> tableColumnName;
    @FXML
    TableColumn<Summary, Integer> tableColumnValue;
    @FXML
    TableView<Summary> tableViewSummary;
    @FXML
    ImageView imageTrash;

    private HashMap<String, TreeItem<PartClass>> partHash;
    private HashMap<String, TreeItem<PartClass>> buildHash;
    private HashMap<String, Summary> sumHash = new HashMap<>();
    static TreeItem<PartClass> menuItem;
    private ObservableList<Summary> totalData = FXCollections.observableArrayList();
    private TreeItem<PartClass> buildItemRoot;
    private Stage mainStage = Toocan.getPrimaryStage();
    private TreeItem<PartClass> partItemRoot;
    private int BuildCount = 0;

    public void initialize() {

        partHash = new HashMap<>();
        buildHash = new HashMap<>();
        PartClass partNodeRoot = new PartClass("Part");
        partItemRoot = new TreeItem<>(partNodeRoot);
        treeViewParts.setRoot(partItemRoot);
        treeViewParts.setShowRoot(false);

        /* Build TreeView setup */

        PartClass buildNodeRoot = new PartClass("Build");
        buildItemRoot = new TreeItem<>(buildNodeRoot);
        treeViewBuild.setRoot(buildItemRoot);
        treeViewBuild.setShowRoot(false);

        treeViewParts.setCellFactory(cellData -> {
            final Tooltip tooltip = new Tooltip();
            TreeCell<PartClass> cell = new TreeCell<>() {
                @Override
                protected void updateItem(PartClass p, boolean empty) {
                    super.updateItem(p, empty);
                    if (empty || p == null) {
                        setText(null);
                        setGraphic(null);
                        setTooltip(null);
                    } else {
                        setText(p.Description);
                        setStyle("-fx-text-fill: green");
                        tooltip.setText(p.Code);
                        setTooltip(tooltip);
                        setGraphic(getTreeItem().getGraphic());
                    }
                }
            };
            cell.setOnDragOver(event -> {
                event.acceptTransferModes(TransferMode.COPY);
                event.consume();
            });
            return cell;
        });

        treeViewBuild.setCellFactory(cellData -> {
            final Tooltip tooltip = new Tooltip();
            TreeCell<PartClass> cell = new TreeCell<>() {
                @Override
                protected void updateItem(PartClass p, boolean empty) {
                    super.updateItem(p, empty);
                    if (empty || p == null) {
                        setText(null);
                        setGraphic(null);
                        setTooltip(null);
                    }
                    else {
                        setStyle("-fx-text-fill: blue");
                        if (p.itemCount > 1) {
                            if (p.Label == null)
                                setText(Integer.toString(p.itemCount) + " x " + p.Description);
                            else
                                setText(Integer.toString(p.itemCount) + " x " + p.Description + " - " + p.Label);
                        }
                        else {
                            if (p.Label == null)
                                setText(p.Description);
                            else
                                setText(p.Description + " - " + p.Label);
                        }
                        tooltip.setText(Integer.toString(p.totalCount) + " x " + p.Code);
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
                PartClass source = partHash.get(dragNode).getValue().deepClone();

                PartClass target = cell.getTreeItem().getValue();

                int count = dropNode(source, target);
                if (count > 0) {
                    source.Tree = 'B';
                    source.Parent = target.Code;
                    source.ParentBuildId = target.BuildId;
                    source.BuildId = Integer.toString(BuildCount++);
                    source.itemCount = count;
                    source.totalCount = count * target.totalCount;
                    TreeItem<PartClass> buildItem;
                    if (event.getDragboard().hasImage()) {
                        Image itemImage = event.getDragboard().getImage();
                        buildItem = new TreeItem<>(source, new ImageView(itemImage));
                    } else {
                        buildItem = new TreeItem<>(source);
                    }
                    cell.getTreeItem().getChildren().add(buildItem);
                    String key = makeKey(source.Code, source.BuildId);
                    buildHash.put(key, buildItem);
                    cell.getTreeItem().setExpanded(true);

                    /* Update Summary Data */

                    if (source.Increment > 0) {
                        Summary sum = sumHash.get(source.SummaryName);
                        int index = totalData.indexOf(sum);
                        int total = sum.getValue();
                        sum.setValue(total + source.Increment * source.totalCount);
                        totalData.set(index, sum);
                    }
                }
                event.setDropCompleted(true);
                event.consume();
            });
            return cell;
        });

        /* load treeViewParts from Database */

        loadParts();

        /* Summary Data Initialization */

        PropertyValueFactory<Summary, String> nameProperty = new PropertyValueFactory<>("name");
        PropertyValueFactory<Summary, Integer> valueProperty = new PropertyValueFactory<>("value");

        tableColumnName.setCellValueFactory(nameProperty);
        tableColumnValue.setCellValueFactory(valueProperty);
        tableViewSummary.setItems(totalData);

        PartClass base = partHash.get("Base").getValue().deepClone();
        base.Tree = 'B';
        base.totalCount = 1;
        base.BuildId = Integer.toString(BuildCount++);
        String iconPath = "/img/Base.png";
        Image icon = new Image(getClass().getResourceAsStream(iconPath));
        TreeItem<PartClass> partItem = new  TreeItem<>(base, new ImageView(icon));
        buildItemRoot.getChildren().add(partItem);
        String baseKey = makeKey(base.Code, base.BuildId);
        buildHash.put(baseKey, partItem);

        ContextMenu treeContext = new ContextMenu();

        MenuItem listSlots = new MenuItem("List Slot Details");
        listSlots.setOnAction(e -> {
            menuItem = treeViewBuild.getSelectionModel().getSelectedItem();
            try {
                FXMLLoader fxmlFormLoader = new FXMLLoader(getClass().getResource("SlotDisplay.fxml"));
                Parent slotForm = fxmlFormLoader.load();
                Stage slotStage = new Stage();
                slotStage.setTitle("Slot List");
                slotStage.setScene(new Scene(slotForm, 700, 600));
                slotStage.show();
            } catch (IOException ex) {
                labelBuildStatus.setText(ex.getMessage());
            }
        });

        /* change Node quantity */

        MenuItem changeItem = new MenuItem("Change Quantity");
        changeItem.setOnAction(e -> {
            menuItem = treeViewBuild.getSelectionModel().getSelectedItem();
            PartClass child = menuItem.getValue();
            TextInputDialog labelDialog = new TextInputDialog();
            labelDialog.setHeaderText("Change Quantity from " + child.itemCount + " to :" );
            Optional<String> nodeQty = labelDialog.showAndWait();
            if (nodeQty.isPresent()) {
                int newCount = Integer.parseInt(nodeQty.get());
            if (newCount > 0) {
                child.totalCount = child.totalCount * newCount / child.itemCount;
                child.itemCount = newCount;
                updateChildCount(menuItem, newCount);
            }
            }
        });

        // Add node label

        MenuItem labelItem = new MenuItem("Add Label");
        labelItem.setOnAction(e -> {
            menuItem = treeViewBuild.getSelectionModel().getSelectedItem();
            PartClass child = menuItem.getValue();
            TextInputDialog labelDialog = new TextInputDialog();
            labelDialog.setHeaderText("Enter Label :");
            Optional<String> nodeLabel = labelDialog.showAndWait();
            child.Label = nodeLabel.orElse(null);
        });

        treeContext.getItems().add(listSlots);
        treeContext.getItems().add(changeItem);
        treeContext.getItems().add(labelItem);
        treeViewBuild.setContextMenu(treeContext);


        imageTrash.setOnDragOver(event -> {
            event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });

        imageTrash.setOnDragDropped(event -> {
            String key = event.getDragboard().getString();
            TreeItem<PartClass> nodeItem = buildHash.get(key);
            if (nodeItem.isLeaf()) {
                PartClass node = nodeItem.getValue();
                String parentKey = makeKey(node.Parent, node.ParentBuildId);
                TreeItem<PartClass> parentItem = buildHash.get(parentKey);
                PartClass parent = parentItem.getValue();
                parent.slotContents[node.ParentSlotIndex] -= node.itemCount;
                parentItem.getChildren().remove(nodeItem);
                buildHash.remove(key, nodeItem);

                /* Update Summary Data */

                if (node.Increment > 0) {
                    Summary sum = sumHash.get(node.SummaryName);
                    int index = totalData.indexOf(sum);
                    int total = sum.getValue();
                    sum.setValue(total - node.Increment * node.totalCount);
                    totalData.set(index, sum);
                }

            }
            else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                Image stopIcon = new Image(getClass().getResourceAsStream("/img/stop.png"));
                alert.setGraphic(new ImageView(stopIcon));
                alert.setContentText("Delete leaves first");
                alert.showAndWait();
            }
            event.setDropCompleted(true);
            event.consume();
        });
    }

    private void updateChildCount(TreeItem<PartClass> item, int i) {
        for (TreeItem<PartClass> ch : item.getChildren()) {
            ch.getValue().totalCount = ch.getValue().itemCount * i;
            if (!ch.getChildren().isEmpty())
                updateChildCount(ch, ch.getValue().itemCount * i);
        }
    }

    private void loadParts() {

        String SQL = "SELECT CODE, DESCRIPTION, CATEGORY FROM PART ORDER BY CATEGORY, DESCRIPTION";
        try {
            ResultSet rsPart = sr.executeQuery(SQL);
            while (rsPart.next()) {
                String partCode = rsPart.getString("CODE");
                String partCat = rsPart.getString("CATEGORY");
                PartClass part = new PartClass(partCode,
                        rsPart.getString("DESCRIPTION"), partCat);
                part.Tree = 'P';
                String iconPath = "/img/" + partCat + ".png";
                TreeItem<PartClass> partItem;
                InputStream iconStream = getClass().getResourceAsStream(iconPath);
                if (iconStream != null) {
                    Image icon = new Image(iconStream);
                    partItem = new TreeItem<>(part, new ImageView(icon));
                } else
                    partItem = new TreeItem<>(part);
                TreeItem<PartClass> parentItem = partHash.get(partCat);
                if (parentItem == null) {
                    parentItem = new TreeItem<>(new PartClass(partCat));
                    treeViewParts.getRoot().getChildren().add(parentItem);
                    partHash.put(partCat, parentItem);
                }
                parentItem.getChildren().add(partItem);
                partHash.put(partCode, partItem);

                /* Load Slot Information */

                SQL = "SELECT NAME, CONTENT, SELECTOR FROM SLOT INNER JOIN LINK ON SLOT.NAME = LINK.SLOT_NAME " +
                        "WHERE DIR = 'F' AND PART_CODE = '" + partCode + "'";
                ResultSet rsSlot = st.executeQuery(SQL);
                rsSlot.last();
                part.slotCount = rsSlot.getRow();
                if(part.slotCount > 0 ) {
                    part.slotName = new String[part.slotCount];
                    part.slotHash = new int[part.slotCount];
                    part.slotMax = new int[part.slotCount];
                    part.slotContents = new int[part.slotCount];
                    rsSlot.beforeFirst();
                    int i = 0;
                    while (rsSlot.next()) {
                        String name = rsSlot.getString("NAME");
                        part.slotName[i] = name;
                        part.slotHash[i] = name.hashCode();
                        switch(rsSlot.getString("SELECTOR")) {
                            case "M":
                                part.slotMax[i] = rsSlot.getInt("CONTENT");
                                break;
                            case "E":
                                part.slotMax[i] = -rsSlot.getInt("CONTENT");
                                break;
                            case "U":
                                part.slotMax[i] = 0;
                                break;
                        }
                        part.slotContents[i++] = 0;
                    }
                }
                rsSlot.close();

                SQL = "SELECT SLOT_NAME FROM LINK WHERE DIR = 'M' AND PART_CODE = '" + partCode + "'";
                rsSlot = st.executeQuery(SQL);
                rsSlot.last();
                part.tabCount = rsSlot.getRow();
                if (part.tabCount > 0) {
                    part.tabHash = new int[part.tabCount];
                    rsSlot.beforeFirst();
                    int i = 0;
                    while (rsSlot.next()) {
                        part.tabHash[i++] = rsSlot.getString("SLOT_NAME").hashCode();
                    }
                }
                rsSlot.close();
            }
            rsPart.close();

            /* Create Summary Tables */

            SQL = "SELECT NAME, INCREMENT, PART_CODE FROM SUMMARY";
            rsPart = sr.executeQuery(SQL);
            while (rsPart.next()) {
                TreeItem<PartClass> nodeItem = partHash.get(rsPart.getString("PART_CODE"));
                PartClass node = nodeItem.getValue();
                node.SummaryName = rsPart.getString("NAME");
                node.Increment = rsPart.getInt("INCREMENT");
            }
            rsPart.close();

            SQL = "SELECT DISTINCT NAME FROM SUMMARY";
            rsPart = sr.executeQuery(SQL);
            while (rsPart.next()) {
                String sumName = rsPart.getString("NAME");
                Summary sumData = new Summary(sumName, 0);
                totalData.add(sumData);
                sumHash.put(sumName, sumData);
            }

        } catch(SQLException ex) {
            labelBuildStatus.setText(ex.getMessage());
        }
    }
    public void partDragDetected(MouseEvent event) {
        TreeItem<PartClass> nodeItem = treeViewParts.getSelectionModel().getSelectedItem();
        PartClass node = nodeItem.getValue();
        if ( !node.Code.equals("folder")) {
            Dragboard db = treeViewParts.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.put(DataFormat.PLAIN_TEXT, node.Code);
            if (nodeItem.getGraphic() != null) {
                ImageView iv = (ImageView) nodeItem.getGraphic();
                content.put(DataFormat.IMAGE, iv.getImage());
            }
            db.setContent(content);
        }
        event.consume();
    }
    public void buildDragDetected(MouseEvent event) {
        TreeItem<PartClass> nodeItem = treeViewBuild.getSelectionModel().getSelectedItem();
        PartClass node = nodeItem.getValue();
        Dragboard db = treeViewBuild.startDragAndDrop(TransferMode.COPY);
        ClipboardContent content = new ClipboardContent();
        String key = makeKey(node.Code, node.BuildId);
        content.put(DataFormat.PLAIN_TEXT, key);
        db.setContent(content);
        event.consume();
    }

    private String makeKey(String a, String b) {
        return a + '~' + b;
    }


    private int dropNode (PartClass leaf, PartClass branch) {
        int parentSlot = 0;
        int qtyParent = 0, maxParent = 0, addQty;
        boolean match = false;
        for (int i = 0; i < leaf.tabCount; i++) {
            for (int j = 0; j < branch.slotCount; j++) {
                if (leaf.tabHash[i] == branch.slotHash[j]) {
                    match = true;
                    parentSlot = j;
                    qtyParent = branch.slotContents[parentSlot];
                    maxParent = branch.slotMax[parentSlot];
                    break;
                }
            }
        }
        if (match) {
            Optional<String> addCount;
            leaf.ParentSlotIndex = parentSlot;
            for (; ;) {
                TextInputDialog countDialog = new TextInputDialog();
                if (maxParent > qtyParent) {
                    countDialog.setHeaderText("Enter Quantity <= " + Integer.toString(maxParent - qtyParent) + " :");
                    addCount = countDialog.showAndWait();
                    if (addCount.isPresent()) {
                        addQty = Integer.parseInt(addCount.get());
                        if (addQty + qtyParent <= maxParent)
                            branch.slotContents[parentSlot] = addQty + qtyParent;
                        return addQty;
                    }

                } else if (maxParent < 0 && qtyParent != -maxParent) {
                    branch.slotContents[parentSlot] = -maxParent;
                    return -maxParent;
                } else if (maxParent == 0) {
                    countDialog.setHeaderText("Enter Quantity :");
                    addCount = countDialog.showAndWait();
                    if (addCount.isPresent()) {
                        addQty = Integer.parseInt(addCount.get());
                        branch.slotContents[parentSlot] = addQty + qtyParent;
                        return addQty;
                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("Part slots are full");
                    alert.showAndWait();
                    return 0;
                }
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            Image stopIcon = new Image(getClass().getResourceAsStream("/img/stop.png"));
            alert.setGraphic(new ImageView(stopIcon));
            alert.setContentText("Part is not valid here");
            alert.showAndWait();
            return 0;
        }
    }


    public void ButtonCloseOnAction (){
        Stage stage = (Stage) buttonClose.getScene().getWindow();
        stage.close();
    }


    public void ButtonExportOnAction() {
        HashMap<String, Integer> exportHash = new HashMap<>();
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("csv file (*.csv)", "*.csv"));
            fileChooser.setTitle("Save csv file");
            File exportFile = fileChooser.showSaveDialog(mainStage);
            if (exportFile.exists())
                exportFile.delete();
            if (exportFile.createNewFile()) {
                FileWriter fw = new FileWriter(exportFile);
                BufferedWriter bw = new BufferedWriter(fw);

                exportTree(buildItemRoot, exportHash);
                for(Map.Entry<String, Integer> entry : exportHash.entrySet() ) {
                    bw.write(entry.getKey() + "," + entry.getValue());
                    bw.newLine();
                }
                bw.close();
                fw.close();
            }

        } catch (IOException e ) {
            e.printStackTrace();

        }
    }
    private void exportTree(TreeItem<PartClass> item, HashMap<String, Integer> map) {

        if (item.getValue().itemCount > 0) {
            updateTotal(item.getValue(), map);
        }
        for (TreeItem<PartClass> cp : item.getChildren()) {
            if (cp.getChildren().isEmpty()) {
                updateTotal(cp.getValue(), map);
            } else
                exportTree(cp, map);
        }

    }

    private void updateTotal (PartClass part, HashMap<String, Integer> m) {
        if (m.containsKey(part.Code))
            m.put(part.Code, m.get(part.Code) + part.totalCount);
        else
            m.put(part.Code, part.totalCount);
    }

    public void openJsonOnAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("json file (*.json)", "*.json"));
        fileChooser.setTitle("Open json file");
        File jsonFile = fileChooser.showOpenDialog(mainStage);
        Gson gson = new Gson();
        try {

            FileReader fr = new FileReader(jsonFile);
            BufferedReader br = new BufferedReader(fr);

            readTree(br, gson);
            br.close();
            fr.close();

        }  catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveJsonOnAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("json file (*.json)", "*.json"));
        fileChooser.setTitle("Save json file");

        File jsonFile = fileChooser.showSaveDialog(mainStage);
        Gson gson = new Gson();
        try {
            if (jsonFile.exists())
                jsonFile.delete();
            if (jsonFile.createNewFile()) {

                FileWriter fw = new FileWriter(jsonFile);
                BufferedWriter bw = new BufferedWriter(fw);
                saveTree(buildItemRoot, bw, gson);

                bw.close();
                fw.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void saveTree(TreeItem<PartClass> item, BufferedWriter bw, Gson gs) {
        try {
            if (item.getValue().itemCount > 0) {
                String json = gs.toJson(item.getValue());
                bw.write(json);
                bw.newLine();
            }
            for (TreeItem<PartClass> cp : item.getChildren()) {
                if (cp.getChildren().isEmpty()) {
                    String json = gs.toJson(cp.getValue());
                    bw.write(json);
                    bw.newLine();
                } else
                    saveTree(cp, bw, gs);
            }
        } catch (IOException ex) {
            labelBuildStatus.setText(ex.getMessage());
        }
    }
    private void readTree(BufferedReader br, Gson gs) {

        String line;
        PartClass child;
        try {
            while ((line = br.readLine()) != null && line.length() != 0) {
                child = gs.fromJson(line, PartClass.class);
                String iconPath = "/img/" + child.Category + ".png";
                InputStream iconStream = getClass().getResourceAsStream(iconPath);
                TreeItem<PartClass> newItem;
                if (iconStream != null) {
                    Image icon = new Image(iconStream);
                    newItem = new TreeItem<>(child, new ImageView(icon));
                } else
                    newItem = new TreeItem<>(child);
                String key = makeKey(child.Code, child.BuildId);
                buildHash.put(key, newItem);
                key = makeKey(child.Parent, child.ParentBuildId);
                TreeItem<PartClass> parentItem = buildHash.get(key);
                parentItem.getChildren().add(newItem);
                ++BuildCount;

                /* Update Summary Totals */

                if (child.Increment > 0) {
                    Summary sum = sumHash.get(child.SummaryName);
                    int index = totalData.indexOf(sum);
                    int total = sum.getValue();
                    sum.setValue(total + child.Increment * child.totalCount);
                    totalData.set(index, sum);
                }
            }
        } catch (IOException ex) {
            labelBuildStatus.setText(ex.getMessage());
        }
    }
    public void buttonPartCollapseOnAction() {
        for (TreeItem<PartClass> node : partItemRoot.getChildren()) {
            node.setExpanded(false);
        }
    }
    public void buttonBuildCollapseOnAction() {
        for (TreeItem<PartClass> node : buildItemRoot.getChildren()) {
            node.setExpanded(false);
        }

    }
}
