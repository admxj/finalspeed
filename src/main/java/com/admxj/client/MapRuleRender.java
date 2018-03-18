package com.admxj.client;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTable.DropLocation;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;
import sun.swing.DefaultLookup;

public class MapRuleRender extends JLabel implements TableCellRenderer {
    private static final long serialVersionUID = -3260748459008436510L;
    JPanel pleft;
    JPanel pright;
    JPanel p1;
    JLabel label_wan_address;
    JLabel label2;
    MapRule rule;

    public MapRuleRender() {
        this.setOpaque(true);
        this.setLayout(new MigLayout("insets 8 10 0 0"));
        this.label_wan_address = new JLabel();
        this.add(this.label_wan_address, "width :500:,wrap");
        this.label_wan_address.setBackground(new Color(0.0F, 0.0F, 0.0F, 0.0F));
        this.label_wan_address.setOpaque(true);
        this.label2 = new JLabel();
        this.add(this.label2, "width :500:,wrap");
        this.label2.setBackground(new Color(0.0F, 0.0F, 0.0F, 0.0F));
        this.label2.setOpaque(true);
    }

    void update(MapRule rule, JTable table, int row) {
        this.rule = rule;
        int rowHeight = 50;
        int h = table.getRowHeight(row);
        if (h != rowHeight) {
            table.setRowHeight(row, rowHeight);
        }

        String name = rule.getName();
        if (name == null) {
            name = "无";
        } else if (name.trim().equals("")) {
            name = "无";
        }

        this.label_wan_address.setText("名称: " + rule.name + "  加速端口: " + rule.dst_port);
        this.label2.setText("本地端口: " + rule.getListen_port());
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Color fg = null;
        Color bg = null;
        DropLocation dropLocation = table.getDropLocation();
        if (dropLocation != null && !dropLocation.isInsertRow() && !dropLocation.isInsertColumn() && dropLocation.getRow() == row && dropLocation.getColumn() == column) {
            fg = DefaultLookup.getColor(this, this.ui, "Table.dropCellForeground");
            bg = DefaultLookup.getColor(this, this.ui, "Table.dropCellBackground");
            isSelected = true;
        }

        if (isSelected) {
            this.setBackground(DefaultLookup.getColor(this, this.ui, "Table.dropCellBackground"));
        } else {
            this.setBackground(DefaultLookup.getColor(this, this.ui, "Table.alternateRowColor"));
        }

        MapRule rule = (MapRule)value;
        this.update(rule, table, row);
        return this;
    }
}
