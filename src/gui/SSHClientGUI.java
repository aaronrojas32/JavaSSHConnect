package gui;

import com.jcraft.jsch.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class SSHClientGUI extends JFrame {
    private JSch jsch;
    private Session session;
    private Channel channel;
    private BufferedReader reader;
    private BufferedWriter writer;

    private JTextField userField;
    private JTextField hostField;
    private JTextField portField;
    private JButton selectKeyButton;
    private JButton connectButton;

    private JTextArea outputTextArea;
    private JTextField commandTextField;
    private JButton sendCommandButton;

    private boolean keySelected = false;
    private String privateKeyPath;

    public SSHClientGUI() {
        setTitle("Java SSH Client");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Set a professional look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JPanel connectPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        connectPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        connectPanel.add(new JLabel("Username:"));
        userField = new JTextField();
        connectPanel.add(userField);
        connectPanel.add(new JLabel("Host:"));
        hostField = new JTextField();
        connectPanel.add(hostField);
        connectPanel.add(new JLabel("Port:"));
        portField = new JTextField();
        connectPanel.add(portField);
        selectKeyButton = new JButton("Select Private Key");
        selectKeyButton.addActionListener(e -> {
            privateKeyPath = selectPrivateKey();
            if (privateKeyPath != null) {
                keySelected = true;
                selectKeyButton.setEnabled(false);
            }
        });
        connectPanel.add(selectKeyButton);
        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> {
            String user = userField.getText();
            String host = hostField.getText();
            int port = Integer.parseInt(portField.getText());
            if (keySelected) {
                connectToSSH(user, host, port);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a private key.");
            }
        });
        connectPanel.add(connectButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(connectPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    private void connectToSSH(String user, String host, int port) {
        try {
            jsch = new JSch();

            jsch.addIdentity(privateKeyPath);

            session = jsch.getSession(user, host, port);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            channel = session.openChannel("shell");
            channel.connect();

            reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(channel.getOutputStream()));

            getContentPane().removeAll();

            JPanel mainPanel = new JPanel(new BorderLayout());

            outputTextArea = new JTextArea();
            outputTextArea.setEditable(false);
            outputTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(outputTextArea);

            JPanel commandPanel = new JPanel(new BorderLayout());
            commandPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            commandTextField = new JTextField();
            commandTextField.addActionListener(e -> sendCommand());
            commandPanel.add(commandTextField, BorderLayout.CENTER);
            sendCommandButton = new JButton("Send");
            sendCommandButton.addActionListener(e -> sendCommand());
            commandPanel.add(sendCommandButton, BorderLayout.EAST);

            mainPanel.add(scrollPane, BorderLayout.CENTER);
            mainPanel.add(commandPanel, BorderLayout.SOUTH);

            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(mainPanel, BorderLayout.CENTER);

            revalidate();
            repaint();

            new Thread(this::refreshOutput).start();

        } catch (JSchException | IOException e) {
            e.printStackTrace();
        }
    }

    private String selectPrivateKey() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            return selectedFile.getAbsolutePath();
        } else {
            return null;
        }
    }

    private void refreshOutput() {
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll("\\033\\[[0-9;]*[a-zA-Z]", ""); // Eliminate ANSI escape sequences
                line = line.replaceAll("\\033\\[K", ""); // Eliminate line erase sequences
                if (!line.trim().isEmpty()) { // Only append non-empty lines
                    outputTextArea.append(line + "\n");
                    outputTextArea.setCaretPosition(outputTextArea.getDocument().getLength());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendCommand() {
        String command = commandTextField.getText();
        if (!command.isEmpty()) {
            try {
                writer.write(command);
                writer.newLine();
                writer.flush();
                commandTextField.setText("");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SSHClientGUI::new);
    }
}
