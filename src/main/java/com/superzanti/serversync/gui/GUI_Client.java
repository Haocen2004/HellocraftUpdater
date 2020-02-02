package com.superzanti.serversync.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Locale;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.TitledBorder;

import com.superzanti.serversync.RefStrings;
import com.superzanti.serversync.SyncConfig;
import com.superzanti.serversync.client.ClientWorker;
import com.superzanti.serversync.util.Log;
import com.superzanti.serversync.util.Logger;

import runme.Main;

public class GUI_Client extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6888496807573569823L;

	private JPanel root = new JPanel();

	private JPanel P_serverDetails;
	private JLabel ipLabel = new JLabel("IP ��ַ");
	private JTextField TF_ipAddress = new JTextField();
	private JLabel portLabel = new JLabel("�˿�");
	private JTextField TF_port = new JTextField();
	private JButton B_sync = new JButton("ͬ��");

	private JPanel P_information = new JPanel();
	private TitledBorder TA_border_title;
	private JTextArea TA_info = new JTextArea();

	private JProgressBar PB_fileProgress = new JProgressBar();

	public GUI_Client() {
		super();
		Logger.getLog().addObserver((o, arg) -> {
			if (o instanceof Log) {
				updateText(((Log) o).userFacingLog.toString());
			}
		});
		
		Dimension sDetailsElements = new Dimension(120, 20);
		SoftBevelBorder TF_border = new SoftBevelBorder(BevelBorder.LOWERED, new Color(64, 64, 64),
				new Color(192, 192, 192), new Color(64, 64, 64), new Color(0, 0, 0));

		setTitle(Main.APPLICATION_TITLE);
		getContentPane().setBackground(new Color(0, 0, 0));
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(600, 300));
		setIconImage(new ImageIcon(ClassLoader.getSystemResource("ServersyncLogo.png")).getImage());

		P_serverDetails = new JPanel();
		P_serverDetails.setBackground(Color.DARK_GRAY);
		P_serverDetails.setPreferredSize(new Dimension(143, 300));

		B_sync.setPreferredSize(sDetailsElements);
		B_sync.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		TF_ipAddress.setCaretColor(Color.WHITE);
		TF_ipAddress.setHorizontalAlignment(SwingConstants.CENTER);
		TF_ipAddress.setForeground(new Color(50, 205, 50));
		TF_ipAddress.setBorder(TF_border);
		TF_ipAddress.setBackground(new Color(30, 30, 30));
		TF_ipAddress.setPreferredSize(sDetailsElements);
		TF_port.setCaretColor(Color.WHITE);
		TF_port.setHorizontalAlignment(SwingConstants.CENTER);
		TF_port.setForeground(new Color(50, 205, 50));
		TF_port.setBackground(new Color(30, 30, 30));
		TF_port.setBorder(TF_border);
		TF_port.setPreferredSize(sDetailsElements);

		TF_ipAddress.setOpaque(true);
		TF_port.setOpaque(true);

		ipLabel.setForeground(Color.WHITE);

		ipLabel.setLabelFor(TF_ipAddress);
		portLabel.setForeground(Color.WHITE);
		portLabel.setLabelFor(TF_port);

		P_serverDetails.add(ipLabel);
		P_serverDetails.add(TF_ipAddress);
		P_serverDetails.add(portLabel);
		P_serverDetails.add(TF_port);
		P_serverDetails.add(B_sync);
		P_information.setBorder(new EmptyBorder(0, 0, 0, 0));

		P_information.setLayout(new BorderLayout(0, 0));
		P_information.add(PB_fileProgress, BorderLayout.NORTH);
		PB_fileProgress.setForeground(Color.GREEN);
		PB_fileProgress.setStringPainted(true);
		PB_fileProgress.setVisible(false);
		PB_fileProgress.setBorder(BorderFactory.createEmptyBorder());
		TA_info.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		TA_info.setBackground(Color.GRAY);

		JScrollPane sp_console = new JScrollPane(TA_info);
		sp_console.setBorder(new EmptyBorder(0, 0, 0, 0));
		sp_console.setViewportBorder(new EmptyBorder(0, 0, 0, 0));
		P_information.add(sp_console);
		sp_console.setPreferredSize(new Dimension(450, 300));

		TA_info.setLineWrap(true);
		TA_info.setWrapStyleWord(true);
		TA_info.setEditable(false);
		TA_border_title = new TitledBorder(UIManager.getBorder("TitledBorder.border"), "ͬ����Ϣ",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(255, 255, 255));
		CompoundBorder TA_border = new CompoundBorder(TA_border_title, new EmptyBorder(5, 10, 5, 10));
		TA_info.setBorder(TA_border);
		root.setLayout(new BorderLayout(0, 0));

		root.add(P_serverDetails, BorderLayout.WEST);
		root.add(P_information);

		add(root);

		// Listeners
		B_sync.addActionListener(e -> publishSyncPressed());

		TF_ipAddress.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(java.awt.event.KeyEvent e) { return; }

			@Override
			public void keyReleased(java.awt.event.KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB) {
					TF_ipAddress.transferFocus();
				}
			}

			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {
				return;
			}
		});

		TF_port.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(java.awt.event.KeyEvent e) {
				return;
			}

			@Override
			public void keyReleased(java.awt.event.KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					requestFocus();
					publishSyncPressed();
				}
			}

			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {
				return;
			}
		});
		
		B_sync.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				int port = getPort();
				String ip = getIPAddress();
				boolean error = false;
				
				if (ip.equals("") || port == 90000) {
					updateText("�����ļ�δ�ҵ���������ϸ��Ϣ��");
					
					if (ip.equals("")) {
						ip = JOptionPane.showInputDialog("������IP��ַ");
						setIPAddress(ip);
					}
					
					if (port == 90000) {
						String serverPort = JOptionPane.showInputDialog("�������˿�(������)");
						port = Integer.parseInt(serverPort);
						
						if(setPort(port)) {
							error = true;
						}
					}
					
					SyncConfig.pullServerConfig = true;
				}
				
				if (!error) {
					Main.CONFIG.SERVER_IP = ip;
					Main.CONFIG.SERVER_PORT = port;
					updateText("��ʼͬ������...");
					new Thread(new ClientWorker()).start();
				}
			}
		});
	}

	public void build(Locale loc) {
		this.internationalize();
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);

	}

	private void internationalize() {
		setTitle(Main.strings.getString("title") + " - " + RefStrings.VERSION);
		ipLabel.setText(Main.strings.getString("server_address"));
		portLabel.setText(Main.strings.getString("server_port"));
		B_sync.setText(Main.strings.getString("go_button"));
		B_sync.setToolTipText(Main.strings.getString("button_tooltip"));
		TA_border_title.setTitle(Main.strings.getString("console_title"));
		TA_info.repaint();
	}

	public void updateText(String text) {
		SwingUtilities.invokeLater(() -> TA_info.setText(text));
	}

	public void updateProgress(int progress) {
		SwingUtilities.invokeLater(() -> B_sync.setText(progress + "%"));
	}

	public void updateFileProgress(String message, int progress) {
		SwingUtilities.invokeLater(() -> {
			if (!PB_fileProgress.isVisible() && progress < 100) {
				PB_fileProgress.setVisible(true);
			}

			PB_fileProgress.setString("< " + progress + "% > " + message);
			PB_fileProgress.setValue(progress);

			if (message == null) {
				PB_fileProgress.setVisible(false);
				PB_fileProgress.setString(null);
			}
		});
	}

	public void enableSyncButton() {
		SwingUtilities.invokeLater(() -> {
			B_sync.setEnabled(true);
			B_sync.setText("��ʼͬ��");
		});
	}
	
	public void disableSyncButton() {
		SwingUtilities.invokeLater(() -> B_sync.setEnabled(false));
	}

	public void toggleSyncButton() {
		SwingUtilities.invokeLater(() -> {
			if (B_sync.isEnabled()) {
				B_sync.setEnabled(false);
			} else {
				B_sync.setEnabled(true);
				B_sync.setText("��ʼͬ��");
			}
		});
	}

	public interface SyncPressedListener {
		void onSyncPressed();
	}

	private SyncPressedListener listener;

	public void setSyncPressedListener(SyncPressedListener listener) {
		this.listener = listener;
	}

	private boolean publishSyncPressed() {
		if (this.listener != null) {
			this.listener.onSyncPressed();
			return true;
		}

		return false;
	}

	public String getIPAddress() {
		return this.TF_ipAddress.getText();
	}

	public void setIPAddress(String ip) {
		SwingUtilities.invokeLater(() -> TF_ipAddress.setText(ip));
	}

	public int getPort() {
		int port = 0;
		try {
			port = Integer.parseInt(TF_port.getText());
			if (!(port <= 49151 && port > 0)) {
				updateText("�˿ڳ�����Χ����ѡ��Χ�� 1 - 49151��");
			}
		} catch (NumberFormatException e) {
			updateText("����˿ڣ�");
			port = 90000;
		}

		return port;
	}

	public boolean setPort(int port) {
		if (!(port <= 49151 && port > 0)) {
			updateText("�˿ڳ�����Χ����ѡ��Χ�� 1 - 49151��");
			return false;
		}

		this.TF_port.setText(String.valueOf(port));
		return true;
	}
}
