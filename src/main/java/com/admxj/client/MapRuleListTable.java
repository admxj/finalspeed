package com.admxj.client;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;

public class MapRuleListTable extends JTable {
    private static final long serialVersionUID = -547936371303904463L;
    MapRuleListModel model;
    MapRuleListTable table;
    ClientUI ui;

    MapRuleListTable(ClientUI ui, MapRuleListModel model) {
        this.model = model;
        this.ui = ui;
        this.table = this;
        this.setModel(model);
        this.setSelectionMode(0);
        this.setRowSorter((RowSorter)null);
        this.getColumnModel().getColumn(0).setMinWidth(30);
        MapRuleRender rr = new MapRuleRender();
        this.getColumnModel().getColumn(0).setCellRenderer(rr);
        this.setRowHeight(50);
        (new Thread() {
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(1000L);
                        MapRuleListTable.this.refresh();
                    } catch (InterruptedException var2) {
                        var2.printStackTrace();
                    }
                }
            }
        }).start();
        this.addMouseListener(new MouseListener() {
            public void mouseReleased(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
                if (e.getButton() == 3 && e.getClickCount() == 1) {
                    int index = MapRuleListTable.this.rowAtPoint(e.getPoint());
                    int modelIndex = MapRuleListTable.this.convertRowIndexToModel(index);
                    MapRuleListTable.this.getSelectionModel().setSelectionInterval(modelIndex, modelIndex);
                }

            }

            public void mouseExited(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == 1 && e.getClickCount() == 2) {
                    MapRuleListTable.this.editRule();
                }

            }
        });
    }

    void editRule() {
        int index = this.getSelectedRow();
        int modelIndex = this.convertRowIndexToModel(index);
        MapRule mapRule = this.getModel().getMapRuleAt(modelIndex);
        new AddMapFrame(this.ui, this.ui.mainFrame, mapRule, true);
    }

    void refresh() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                MapRuleListTable.this.updateUI();
            }
        });
    }

    public void setMapRuleList(List<MapRule> list) {
        this.model.setMapRuleList(list);
    }

    public MapRuleListModel getModel() {
        return this.model;
    }

    public void setModel(MapRuleListModel model) {
        super.setModel(model);
        this.model = model;
    }
}
