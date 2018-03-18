package com.admxj.client;


import java.awt.Insets;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

public class SpeedSetFrame extends JDialog {
    private static final long serialVersionUID = -3248779355079724594L;
    ClientUI ui;
    JTextField text_ds;
    JTextField text_us;
    int downloadSpeed;
    int uploadSpeed;

    SpeedSetFrame(final ClientUI ui, JFrame parent) {
        super(parent, ModalityType.APPLICATION_MODAL);
        this.ui = ui;
        this.setTitle("设置带宽");
        JPanel panel = (JPanel)this.getContentPane();
        panel.setLayout(new MigLayout("alignx center,aligny center,insets 10 10 10 10"));
        panel.add(new JLabel("单位Mb ( 1Mb=128KB,10Mb=1280KB )"), "height ::,wrap");
        panel.add(new JLabel("请正确输入,该值会直接影响加速效果."), "height ::,wrap");
        JPanel p5 = new JPanel();
        panel.add(p5, "wrap");
        p5.setLayout(new MigLayout(""));
        p5.add(new JLabel("下载带宽:"));
        this.text_ds = new JTextField("");
        p5.add(this.text_ds, "width 50::");
        p5.add(new JLabel("Mb"));
        p5.add(new JLabel("  "));
        p5.add(new JLabel("上传带宽:"));
        this.text_us = new JTextField("");
        p5.add(this.text_us, "width 50::");
        p5.add(new JLabel("Mb"));
        JPanel p6 = new JPanel();
        panel.add(p6, "align center,wrap");
        p6.setLayout(new MigLayout("align center"));
        JButton button_ok = this.createButton("确定");
        p6.add(button_ok);
        button_ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String us = SpeedSetFrame.this.text_ds.getText().trim();
                String ds = SpeedSetFrame.this.text_us.getText().trim();

                try {
                    int d = (int)((double)(Float.parseFloat(us) * 1024.0F * 1024.0F / 8.0F) / 1.1D);
                    int u = (int)((double)(Float.parseFloat(ds) * 1024.0F * 1024.0F / 8.0F) / 1.1D);
                    ui.setSpeed(d, u);
                    SpeedSetFrame.this.setVisible(false);
                } catch (Exception var6) {
                    JOptionPane.showMessageDialog(ui.mainFrame, "输入错误!");
                }

            }
        });
        p6.add(new JLabel(" "));
        JButton button_cancel = this.createButton("取消");
        p6.add(button_cancel);
        button_cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SpeedSetFrame.this.setVisible(false);
            }
        });
        this.pack();
        this.setLocationRelativeTo(parent);
        this.setVisible(true);
    }

    JButton createButton(String name) {
        JButton button = new JButton(name);
        button.setMargin(new Insets(0, 5, 0, 5));
        button.setFocusPainted(false);
        return button;
    }
}
