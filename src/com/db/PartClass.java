package com.db;

import java.io.*;

class PartClass implements Serializable {
    char Tree;
    String BuildId;
    String Parent;
    String ParentBuildId;
    String Label;
    String Code;
    String Description;
    String Category;
    int slotCount;
    String slotName[];
    int slotHash[];
    int slotMax[];
    int slotContents[];
    int ParentSlotIndex;

    int tabCount;
    int tabHash[];

    int itemCount;
    int totalCount;

    /* Summary Data */

    String SummaryName;
    int Increment;


    PartClass(String desc) {
        this.Tree = 'P';
        this.Description = desc;
        this.Code = "folder";
        this.itemCount = 0;
    }

    PartClass(String code, String desc, String cat) {
        this.Code = code;
        this.Description = desc;
        this.Category = cat;
        this.Increment = 0;
        this.itemCount = 0;
        this.Label = null;
    }
    public PartClass deepClone () {
        PartClass copy = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(this);
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bis);
            copy = (PartClass) in.readObject();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return copy;
    }
}
