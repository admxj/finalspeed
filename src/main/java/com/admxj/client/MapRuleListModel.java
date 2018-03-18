package com.admxj.client;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.table.AbstractTableModel;

public class MapRuleListModel extends AbstractTableModel {
    private static final long serialVersionUID = 2267856423317178816L;
    private List<MapRule> mapRuleList = new ArrayList();
    String[] titles = new String[]{""};
    Class<?>[] types = new Class[]{String.class, String.class, String.class, String.class, String.class, String.class};

    MapRuleListModel() {
    }

    public void setMapRuleList(List<MapRule> list) {
        this.mapRuleList.clear();
        if (list != null) {
            this.mapRuleList.addAll(list);
        }

        this.fireTableDataChanged();
    }

    public int getMapRuleIndex(String name) {
        int index = -1;
        int i = 0;

        for(Iterator var5 = this.mapRuleList.iterator(); var5.hasNext(); ++i) {
            MapRule r = (MapRule)var5.next();
            if (name.equals(r.getName())) {
                index = i;
                break;
            }
        }

        return index;
    }

    List<MapRule> getMapRuleList() {
        return this.mapRuleList;
    }

    public MapRule getMapRuleAt(int row) {
        return row > -1 & row < this.mapRuleList.size() ? (MapRule)this.mapRuleList.get(row) : null;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        MapRule node = (MapRule)this.mapRuleList.get(rowIndex);
        return node;
    }

    public void setValueAt(Object value, int row, int col) {
        this.fireTableCellUpdated(row, col);
    }

    public int getRowCount() {
        return this.mapRuleList.size();
    }

    public int getColumnCount() {
        return this.titles.length;
    }

    public String getColumnName(int c) {
        return this.titles[c];
    }

    public Class<?> getColumnClass(int c) {
        return this.types[c];
    }

    public boolean isCellEditable(int row, int col) {
        boolean b = false;
        if (col == 0) {
            b = true;
        }

        return false;
    }
}
