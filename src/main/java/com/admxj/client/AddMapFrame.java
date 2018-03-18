package com.admxj.client;


import java.awt.Insets;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

public class AddMapFrame extends JDialog {
    private static final long serialVersionUID = -3248779355079724594L;
    ClientUI ui;
    JTextField portTextField;
    JTextField text_port;
    JTextField nameTextField;
    int downloadSpeed;
    int uploadSpeed;
    MapRule maprule_origin;
    boolean edit = false;

    AddMapFrame(final ClientUI ui, JFrame parent, final MapRule maprule_origin, final boolean edit) {
        super(parent, ModalityType.APPLICATION_MODAL);
        this.ui = ui;
        this.edit = edit;
        this.maprule_origin = maprule_origin;
        this.setTitle("增加映射");
        if (edit) {
            this.setTitle("编辑映射");
        }

        JPanel panel = (JPanel)this.getContentPane();
        panel.setLayout(new MigLayout("alignx center,aligny center,insets 10 10 10 10"));
        String text = "<html><head></head><body>单位Mb ( 1Mb=128KB,10Mb=1280KB )<br>请正确输入,该值会直接影响加速效果.</span></br></body></html>";
        JPanel p3 = new JPanel();
        panel.add(p3, "wrap");
        p3.setBorder(BorderFactory.createEtchedBorder());
        p3.setLayout(new MigLayout("inset 5 5 5 5"));
        p3.add(new JLabel("名称:"));
        this.nameTextField = new JTextField();
        p3.add(this.nameTextField, "width :100: ,wrap");
        p3.add(new JLabel("加速端口:"));
        this.portTextField = new JTextField("");
        p3.add(this.portTextField, "width :50:,wrap");
        this.portTextField.setToolTipText("需要加速的端口号");
        p3.add(new JLabel("本地端口:\t"));
        this.text_port = new JTextField();
        p3.add(this.text_port, "width :50: ,wrap");
        JPanel p6 = new JPanel();
        panel.add(p6, "align center,wrap");
        p6.setLayout(new MigLayout("align center"));
        JButton button_ok = this.createButton("确定");
        p6.add(button_ok);
        button_ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    AddMapFrame.this.checkName(AddMapFrame.this.nameTextField.getText());
                    AddMapFrame.this.checkPort(AddMapFrame.this.text_port.getText());
                    AddMapFrame.this.checkPort(AddMapFrame.this.portTextField.getText());
                    String name = AddMapFrame.this.nameTextField.getText();
                    int listen_port = Integer.parseInt(AddMapFrame.this.text_port.getText());
                    int dst_port = Integer.parseInt(AddMapFrame.this.portTextField.getText());
                    MapRule mapRule_new = new MapRule();
                    mapRule_new.setName(name);
                    mapRule_new.listen_port = listen_port;
                    mapRule_new.setDst_port(dst_port);
                    if (!edit) {
                        ui.mapClient.portMapManager.addMapRule(mapRule_new);
                    } else {
                        ui.mapClient.portMapManager.updateMapRule(maprule_origin, mapRule_new);
                    }

                    ui.loadMapRule();
                    ui.select(mapRule_new.name);
                    AddMapFrame.this.setVisible(false);
                } catch (Exception var6) {
                    JOptionPane.showMessageDialog(ui.mainFrame, var6.getMessage(), "消息", 2);
                }

            }
        });
        p6.add(new JLabel(" "));
        JButton button_cancel = this.createButton("取消");
        p6.add(button_cancel);
        button_cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AddMapFrame.this.setVisible(false);
            }
        });
        if (edit) {
            this.nameTextField.setText(maprule_origin.name);
            this.text_port.setText(String.valueOf(maprule_origin.listen_port));
            this.portTextField.setText(String.valueOf(maprule_origin.dst_port));
        }

        this.pack();
        this.setLocationRelativeTo(parent);
        this.setVisible(true);
    }

    void checkName(String s) throws Exception {
        if (s.trim().equals("")) {
            throw new Exception("请输入名称");
        }
    }

    void checkDstAddress(String s) throws Exception {
        if (s.trim().equals("")) {
            throw new Exception("请输入目标地址");
        }
    }

    void checkPort(String s) throws Exception {
        boolean var2 = false;

        int port;
        try {
            port = Integer.parseInt(s);
        } catch (Exception var4) {
            throw new Exception("请输入正确端口号");
        }

        if (port < 1 | port > 65536) {
            throw new Exception("请输入正确端口号");
        }
    }

    JButton createButton(String name) {
        JButton button = new JButton(name);
        button.setMargin(new Insets(0, 5, 0, 5));
        button.setFocusPainted(false);
        return button;
    }
}
