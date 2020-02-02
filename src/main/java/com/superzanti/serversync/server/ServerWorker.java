package com.superzanti.serversync.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;

import com.superzanti.serversync.util.FileHash;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.ServerTimeout;
import com.superzanti.serversync.util.SyncFile;
import com.superzanti.serversync.util.enums.EErrorType;
import com.superzanti.serversync.util.enums.EServerMessage;
import com.superzanti.serversync.util.errors.InvalidSyncFileException;
import com.superzanti.serversync.util.errors.MessageError;
import com.superzanti.serversync.util.errors.UnknownMessageError;

import runme.Main;

/**
 * This worker handles requests from the client continuously until told to exit
 * using SECURE_EXIT These workers are assigned per socket connection i.e. one
 * per client
 * 
 * @author superzanti
 */
public class ServerWorker implements Runnable {

	private Socket clientsocket;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;

	private EnumMap<EServerMessage, String> messages;

	private Date clientConnectionStarted;
	private DateFormat dateFormatter;
	private Timer timeout;

	protected ServerWorker(Socket socket, ServerSocket theServer, EnumMap<EServerMessage, String> comsMessages,
			Timer timeoutScheduler) {
		clientsocket = socket;
		messages = comsMessages;
		clientConnectionStarted = new Date();
		dateFormatter = DateFormat.getDateTimeInstance();
		timeout = timeoutScheduler;

		Logger.log("建立连接与:" + clientsocket + dateFormatter.format(clientConnectionStarted));
		Logger.log(ServerSetup.directories.toString());
	}

	@Override
	public void run() {
		try {
			ois = new ObjectInputStream(clientsocket.getInputStream());
			oos = new ObjectOutputStream(clientsocket.getOutputStream());
			oos.flush();
		} catch (IOException e) {
			Logger.log("创建客户端流失败");
			e.printStackTrace();
		}

		while (!clientsocket.isClosed()) {
			String message = null;
			try {
				ServerTimeout task = new ServerTimeout(this);
				timeout.schedule(task, 10000);
				message = (String) ois.readObject();
				Logger.log("从 " + clientsocket.getInetAddress()+" 收到信息");
				task.cancel();
				timeout.purge();
			} catch (SocketException e) {
				// Client timed out
				break;
			} catch (ClassNotFoundException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (message == null) {
				continue;
			}

			try {
				if (message.equals(Main.HANDSHAKE)) {
					Logger.log("发送连接信息");
					oos.writeObject(messages);
					oos.flush();
					continue;
				}

				if (!messages.containsValue(message)) {
					try {
						Logger.log("收到未知信息 IP: " + clientsocket.getInetAddress());
						oos.writeObject(new UnknownMessageError(message));
						oos.flush();
					} catch (IOException e) {
						Logger.log("对客户端写出错误失败： " + clientsocket);
						e.printStackTrace();
					}
					timeout = new Timer();
					timeout.schedule(new ServerTimeout(this), 5000);
					continue;
				}

				if (message.equals(messages.get(EServerMessage.INFO_LAST_UPDATE))) {
					Logger.log("发送最后更新时间戳");
					oos.writeObject(Main.CONFIG.LAST_UPDATE);
					oos.flush();
					continue;
				}

				if (message.equals(messages.get(EServerMessage.UPDATE_NEEDED))) {
					int checkLevel = ois.readInt();
					ArrayList<String> serverFileNames = new ArrayList<>(200);
					if (checkLevel == 3) {
						Logger.log("客户端请求完整文件列表");
						serverFileNames.addAll(SyncFile.listModNames(ServerSetup.allFiles));
					} else {
						Logger.log("客户端拒绝接收客户端模组，发送标准文件列表");
						serverFileNames.addAll(SyncFile.listModNames(ServerSetup.standardSyncableFiles));
					}
					Logger.log("发送可同步模组...");

					serverFileNames.removeAll(new ArrayList<>(Main.CONFIG.FILE_IGNORE_LIST));

					Logger.log("可同步模组列表: " + serverFileNames.toString());
					oos.writeObject(serverFileNames);
					oos.flush();
					continue;
				}

				if (message.equals(messages.get(EServerMessage.FILE_GET_LIST))) {
					Logger.log("发送服务器文件列表给:" + clientsocket);

					oos.writeObject(ServerSetup.standardSyncableFiles);
					oos.flush();
					continue;
				}

				if (message.equals(messages.get(EServerMessage.UPDATE_GET_SYNCABLE_DIRECTORIES))) {
					Logger.log("发送可同步目录: " + ServerSetup.directories);
					oos.writeObject(ServerSetup.directories);
					oos.flush();
					continue;
				}

				if (message.equals(messages.get(EServerMessage.FILE_COMPARE))) {
					Logger.log("比较客户端与服务端文件中 " + clientsocket);
					File theFile;
					try {
						theFile = (File) ois.readObject();
						String serverChecksum = FileHash.hashString(theFile);
						oos.writeObject(serverChecksum);
						oos.flush();
					} catch (ClassNotFoundException e) {
						Logger.log("读取客户端信息失败 " + clientsocket);
						e.printStackTrace();
						oos.writeObject(new MessageError("读取文件失败", EErrorType.STREAM_ACCESS));
						oos.flush();
					}
					continue;
				}

				if (message.equals(messages.get(EServerMessage.UPDATE_GET_CLIENT_ONLY_FILES))) {
					Logger.log("发送客户端模组列表");
					oos.writeObject(ServerSetup.clientOnlyFiles);
					oos.flush();
					continue;
				}

				// Main file update message
				if (message.equals(messages.get(EServerMessage.UPDATE))) {

					SyncFile file;
					try {
						// TODO update this to NIO
						file = (SyncFile) ois.readObject();
						File f = file.getFile();
						Logger.log("写出文件 " + f + " 到客户端 " + clientsocket + "...");
						byte[] buff = new byte[clientsocket.getSendBufferSize()];
						int bytesRead = 0;
						InputStream in = new FileInputStream(f);
						if ((bytesRead = in.read(buff)) == -1) {
							// End of file
							oos.writeBoolean(false);
						} else {
							oos.writeBoolean(true);
							oos.write(buff, 0, bytesRead);

							while ((bytesRead = in.read(buff)) > 0) {
								// oos.writeObject("BLOB");
								oos.write(buff, 0, bytesRead);
							}
						}
						in.close();
						oos.flush();
						// oos.writeObject("EOF");
						Logger.log("写出文件 "+f+" 完成: " + clientsocket);

					} catch (ClassNotFoundException e) {
						Logger.log("读取客户端信息失败 " + clientsocket);
						e.printStackTrace();
						oos.flush();
						oos.writeObject(new MessageError("读取文件路径失败", EErrorType.STREAM_ACCESS));
						oos.flush();
					}
					continue;
				}

				if (message.equals(messages.get(EServerMessage.FILE_GET_CONFIG))) {
					Logger.log("发送配置给客户端...");
					HashMap<String, List<String>> rules = new HashMap<>();
					rules.put("ignore", Main.CONFIG.FILE_IGNORE_LIST);
					rules.put("include", Main.CONFIG.CONFIG_INCLUDE_LIST);
					// TODO add security info in transfer
					oos.writeObject(rules);
					oos.flush();
					continue;
				}

				if (message.equals(messages.get(EServerMessage.INFO_GET_FILESIZE))) {
					Logger.log("写出文件大小到客户端 " + clientsocket + "...");

					SyncFile theFile;
					try {
						theFile = (SyncFile) ois.readObject();
						oos.writeLong(Files.size(theFile.getFileAsPath()));
						oos.flush();
					} catch (ClassNotFoundException e) {
						Logger.log("读取客户端信息失败 " + clientsocket);
						e.printStackTrace();
						oos.writeObject(new MessageError("读取文件目录失败", EErrorType.STREAM_ACCESS));
						oos.flush();
					}
					continue;
				}

				if (message.equals(messages.get(EServerMessage.FILE_EXISTS))) {
					try {
						int checkLevel = ois.readInt();
						SyncFile clientFile = (SyncFile) ois.readObject();
						boolean exists = false;

						if (checkLevel == 3) {
							for (SyncFile serverFile : ServerSetup.allFiles) {
								try {
									if (serverFile.equals(clientFile)) {
										exists = true;
									}
								} catch (InvalidSyncFileException e) {
									// TODO stub invalid file handling
									e.printStackTrace();
								}
							}
						} else {
							for (SyncFile serverFile : ServerSetup.standardSyncableFiles) {
								try {
									if (serverFile.equals(clientFile)) {
										exists = true;
									}
								} catch (InvalidSyncFileException e) {
									// TODO stub invalid file handling
									e.printStackTrace();
								}
							}
						}

						if (exists) {
							System.out.println(clientFile.getFileName() + " exists");
							oos.writeBoolean(true);
							oos.flush();
						} else {
							System.out.println(clientFile.getFileName() + " does not exist");
							oos.writeBoolean(false);
							oos.flush();
						}
					} catch (ClassNotFoundException e) {
						Logger.log("读取客户端信息失败 " + clientsocket);
						e.printStackTrace();
						oos.writeObject(new MessageError("读取文件目录失败", EErrorType.STREAM_ACCESS));
						oos.flush();
					}
					continue;
				}
			} catch (SocketException e) {
				Logger.log("客户端 " + clientsocket + " 断开连接：连接超时");
				break;
			} catch (IOException e) {
				Logger.log("写出流失败 ：" + clientsocket);
				e.printStackTrace();
				break;
			}

			if (message.equals(messages.get(EServerMessage.EXIT))) {
				break;
			}
		}

		Logger.log("连接正常关闭: " + clientsocket);
		teardown();
		return; // End thread, probably not needed here as it is the terminal point of the
				// thread anyway
	}

	private void teardown() {
		try {
			timeout = null;

			if (!clientsocket.isClosed()) {
				clientsocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void timeoutShutdown() {
		try {
			Logger.log("客户端连接超时: " + clientsocket);

			if (!clientsocket.isClosed()) {
				clientsocket.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
