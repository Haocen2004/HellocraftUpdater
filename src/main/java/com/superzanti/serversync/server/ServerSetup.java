package com.superzanti.serversync.server;

import com.superzanti.serversync.filemanager.FileManager;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.SyncFile;
import com.superzanti.serversync.util.enums.EFileMatchingMode;
import com.superzanti.serversync.util.enums.EServerMessage;
import org.apache.commons.codec.digest.DigestUtils;
import runme.Main;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.Timer;

/**
 * Sets up various server data to be passed to the specific client socket being
 * communicated with
 *
 * @author Rheimus
 */
public class ServerSetup implements Runnable {
    private static ServerSocket server;

    // This is what's in our folders
    public static ArrayList<SyncFile> allFiles = new ArrayList<>(200);
    public static ArrayList<SyncFile> standardSyncableFiles = new ArrayList<>(200);
    public static ArrayList<SyncFile> standardFiles = new ArrayList<>(75);
    public static ArrayList<SyncFile> configFiles = new ArrayList<>(200);
    public static ArrayList<SyncFile> clientOnlyFiles = new ArrayList<>(20);
    public static ArrayList<String> directories = new ArrayList<>(20);

    private Timer timeoutScheduler = new Timer();

    public static EnumMap<EServerMessage, String> generateServerMessages() {
        EnumMap<EServerMessage, String> SERVER_MESSAGES = new EnumMap<>(EServerMessage.class);

        for (EServerMessage msg : EServerMessage.values()) {
            double rng = Math.random() * 1000d;
            String hashKey = DigestUtils.sha1Hex(msg.toString() + rng);

            SERVER_MESSAGES.put(msg, hashKey);
        }

        return SERVER_MESSAGES;
    }

    public ServerSetup() {
        DateFormat dateFormatter = DateFormat.getDateInstance();
        FileManager fileManager = new FileManager();

        boolean configsInDirectoryList = false;

        /* SYNC DIRECTORIES */
        for (String dir : Main.CONFIG.DIRECTORY_INCLUDE_LIST) {
            // Specific config handling later
            if (dir.equals("config") || dir.equals("clientmods")) {
                if (dir.equals("config")) {
                    configsInDirectoryList = true;
                    directories.add(dir);
                }
                continue;
            }
            directories.add(dir);
        }

        if (Main.CONFIG.PUSH_CLIENT_MODS) {
            Logger.log("�����������˷��Ϳͻ���ģ��!");
            Logger.log("���ͻ�����Ȼ���Ծܾ�����!");
            // Create clientmods directory if it does not exist
            Path clientOnlyMods = Paths.get("clientmods/");
            if (!Files.exists(clientOnlyMods)) {
                try {
                    Files.createDirectories(clientOnlyMods);
                    Logger.log("clientmods Ŀ¼�����ڣ�������");
                } catch (IOException e) {
                    Logger.error("�����ͻ���ģ��Ŀ¼ʧ��");
                }
            }

            clientOnlyFiles = fileManager.getClientOnlyFiles();
            Logger.log(String.format("�� clientmods Ŀ¼�ҵ�   %d ���ļ�", clientOnlyFiles.size()));
        }

        // Main directory scan for mods
        Logger.log("��ʼɨ��ͬ���ļ�: " + dateFormatter.format(new Date()));
        Logger.debug(String.format("�����ļ�: %s", String.join(", ", Main.CONFIG.FILE_IGNORE_LIST)));
        standardFiles = fileManager.getModFiles(directories, EFileMatchingMode.INGORE);
        Logger.log(String.format("�ҵ� %d�� ���û������ģʽƥ����ļ�", standardFiles.size()));

        /* CONFIGS */
        // If the include list is empty then we have no configs to add
        // If the user has added the config directory to the directory list then they are switching to blacklist mode
        // configs in this mode will be treated as standard files
        // TODO clean up this cruft, just let the user switch their config matching list from white to blacklist in the SS config
        if (!Main.CONFIG.CONFIG_INCLUDE_LIST.isEmpty() && !configsInDirectoryList) {
            configFiles = fileManager.getConfigurationFiles(Main.CONFIG.CONFIG_INCLUDE_LIST, EFileMatchingMode.INCLUDE);
        }

        ServerSetup.allFiles.addAll(ServerSetup.clientOnlyFiles);
        ServerSetup.allFiles.addAll(ServerSetup.standardFiles);
        ServerSetup.allFiles.addAll(ServerSetup.configFiles);

        ServerSetup.standardSyncableFiles.addAll(ServerSetup.standardFiles);
        ServerSetup.standardSyncableFiles.addAll(ServerSetup.configFiles);
    }

    @Override
    public void run() {
        Logger.debug("�����µĿͻ������Ӽ�����.");
        try {
            server = new ServerSocket(Main.CONFIG.SERVER_PORT);
        } catch (BindException e) {
            Logger.error("�����Ѱ󶨵��˿�: " + Main.CONFIG.SERVER_PORT);
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // keep listening indefinitely until program terminates
        Logger.log("���ڿ��Խ��տͻ�������...");

        while (true) {
            try {
                // Sanity check, server should never be null here
                if (server == null) {
                    break;
                }
                Socket socket = server.accept();
                ServerWorker sc = new ServerWorker(socket, server, generateServerMessages(), timeoutScheduler);
                Thread clientThread = new Thread(sc, "Server client Handler");
                clientThread.setName("�ͻ������� - " + socket.getInetAddress());
                clientThread.start();
            } catch (IOException e) {
            	int tag_tag = 1;
                while(tag_tag<20) {
            	Logger.error("<!>���ܿͻ�������ʱ�����жϷ�����������������Ҫ��������������");
            	tag_tag++;
                }
            }
        }
    }
}
