/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.atlauncher.App;
import com.atlauncher.data.OfflineAccount;
import com.atlauncher.managers.AccountManager;

public final class AddOfflineAccountDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final JTextField usernameField;
    private boolean confirmed = false;

    private AddOfflineAccountDialog() {
        setTitle("Add Offline Account");
        setModalityType(ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!confirmed) {
                    System.exit(0);
                }
            }
        });

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        main.add(new JLabel("Enter your offline username (min 4 characters):"), BorderLayout.NORTH);

        usernameField = new JTextField(20);
        main.add(usernameField, BorderLayout.CENTER);

        JButton ok = new JButton("Add Account");
        ok.addActionListener(e -> submit());
        usernameField.addActionListener(e -> submit());

        JPanel south = new JPanel();
        south.add(ok);
        main.add(south, BorderLayout.SOUTH);

        add(main);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void submit() {
        String name = usernameField.getText().trim();
        if (name.length() < 4) {
            JOptionPane.showMessageDialog(this, "Username must be at least 4 characters.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        confirmed = true;
        dispose();
        OfflineAccount account = new OfflineAccount(name);
        App.settings.lastAccount = account.username;
        AccountManager.addAccount(account);
        AccountManager.saveAccounts();
    }

    public static void showDialog() {
        new AddOfflineAccountDialog();
    }
}
