package com.db;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class SlotClass {
    private final SimpleStringProperty slotName;
    private final SimpleIntegerProperty slotMax;
    private final SimpleStringProperty slotType;
    private final SimpleIntegerProperty slotContents;

    SlotClass(String name, int max, String type, int qty) {
        this.slotName = new SimpleStringProperty(name);
        this.slotMax = new SimpleIntegerProperty(max);
        this.slotType = new SimpleStringProperty(type);
        this.slotContents =  new SimpleIntegerProperty(qty);
    }

    public String getSlotName () {
        return slotName.get();
    }
    public int getSlotMax () {
        return slotMax.get();
    }
    public String getSlotType () {
        return slotType.get();
    }
    public int getSlotContents () {
        return slotContents.get();
    }
}
