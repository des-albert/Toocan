package com.db;

import java.io.*;

class NodeClass implements Serializable {

    char NodeType;

    /* Part Data */

    String Code;
    String Description;
    String Category;

    /* Slot Data */

    String Name;
    int MaxContent;
    String SlotType;

    /* Summary Data */

    String SummaryName;
    int Increment;

    NodeClass(String folder) {
        this.Description = folder;
        this.NodeType = 'F';
    }

    /* Part Constructor */

    NodeClass (String code, String desc, String cat, String name) {
        this.NodeType = 'P';
        this.Code = code;
        this.Description = desc;
        this.Category = cat;
        this.Name = name;
        this.Increment = 0;
    }

    /* Slot Constructor */

    NodeClass (String code, String name, int max, String type) {
        this.NodeType = 'S';
        this.Code = code;
        this.Name = name;
        this.MaxContent = max;
        this.SlotType = type;
    }
    NodeClass deepClone () {
        NodeClass copy = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(this);
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bis);
            copy = (NodeClass) in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return copy;
    }

}
