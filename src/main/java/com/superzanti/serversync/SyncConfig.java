package com.superzanti.serversync;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.enums.EConfigDefaults;
import com.superzanti.serversync.util.enums.EConfigType;
import com.superzanti.serversync.util.minecraft.config.MinecraftConfigCategory;
import com.superzanti.serversync.util.minecraft.config.MinecraftConfig;
import com.superzanti.serversync.util.minecraft.config.MinecraftConfigElement;
import com.superzanti.serversync.util.minecraft.config.MinecraftConfigReader;
import com.superzanti.serversync.util.minecraft.config.MinecraftConfigWriter;

final class ConfigDefaults extends HashMap<EConfigDefaults, String> {
	private static final long serialVersionUID = 71158792045085436L;
	
	public ConfigDefaults() {
		this.put(EConfigDefaults.SERVER_IP, "127.0.0.1");
		this.put(EConfigDefaults.SERVER_PORT, "12585");
		this.put(EConfigDefaults.LAST_UPDATE, "");
		this.put(EConfigDefaults.PUSH_CLIENT_MODS, "false");
		this.put(EConfigDefaults.REFUSE_CLIENT_MODS, "false");
		this.put(EConfigDefaults.LOG, "false");
	}
}

/**
 * Handles all functionality to do with serversyncs config file and
 * other configuration properties
 * @author Rheimus
 *
 */
public class SyncConfig {
	private static final String CONFIG_LOCATION = "config" + File.separator + "serversync";
	private static final HashMap<EConfigDefaults, String> defaults = new ConfigDefaults();
	private static final String CATEGORY_GENERAL = "general";
	private static final String CATEGORY_RULES = "rules";
	private static final String CATEGORY_CONNECTION = "serverconnection";
	private static final String CATEGORY_OTHER = "misc";
	
	private MinecraftConfig config;
	
	private Path configPath;
	public final EConfigType configType;
	// COMMON //////////////////////////////
	public String SERVER_IP;
	public String LAST_UPDATE;
	public List<String> FILE_IGNORE_LIST = new ArrayList<String>();
	public List<String> CONFIG_INCLUDE_LIST;
	public Locale LOCALE;
	////////////////////////////////////////
	
	// SERVER //////////////////////////////
	public Boolean LOG;
	public int SERVER_PORT;
	public Boolean PUSH_CLIENT_MODS;
	public List<String> DIRECTORY_INCLUDE_LIST;
	////////////////////////////////////////
	
	// CLIENT //////////////////////////////
	public Boolean REFUSE_CLIENT_MODS = false;
	////////////////////////////////////////
	
	public static boolean pullServerConfig = true;
	
	public SyncConfig(EConfigType type) {
		this.FILE_IGNORE_LIST.add("serversync-*.jar");
		this.FILE_IGNORE_LIST.add("Pluscraft*.jar");
		this.FILE_IGNORE_LIST.add("OptiFine*.jar");
		configType = type;
		config = new MinecraftConfig();
		if (configType == EConfigType.SERVER) {			
			configPath = Paths.get(CONFIG_LOCATION + File.separator + "serversync-server.cfg");
		} else {
			configPath = Paths.get(CONFIG_LOCATION + File.separator + "serversync-client.cfg");
		}
		
		if (!Files.exists(configPath.getParent())) {
			try {
				Files.createDirectories(configPath.getParent());
			} catch (IOException e) {
				Logger.debug("创建目录失败: " + configPath.toString());
			}
		}
		
		if (!Files.exists(configPath)) {			
			createConfiguraton();
		} else {			
			readExistingConfiguration();
		}
		init();
	}
	
	private void readExistingConfiguration() {
		try {
			config.readConfig(new MinecraftConfigReader(Files.newBufferedReader(configPath)));
		} catch (IOException e) {
			Logger.debug("读取配置文件失败: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private boolean createConfiguraton() {
		try {
			Files.createFile(configPath);
		} catch (IOException e) {
			Logger.debug("创建配置文件失败: " + e.getMessage());
			return false;
		}
		
		if (configType == EConfigType.SERVER) {
			SERVER_PORT = Integer.parseInt(defaults.get(EConfigDefaults.SERVER_PORT));
			PUSH_CLIENT_MODS = Boolean.parseBoolean(defaults.get(EConfigDefaults.PUSH_CLIENT_MODS));
			LAST_UPDATE = defaults.get(EConfigDefaults.LAST_UPDATE);
			
				ArrayList<String> comments = new ArrayList<String>();
				ArrayList<String> defaultValueList = new ArrayList<>();
				
				
				MinecraftConfigCategory general = new MinecraftConfigCategory(SyncConfig.CATEGORY_GENERAL);
					comments.add("# 设置为true来推送客户端mod Eg.Optifine 请将模组放在  clientmods 文件夹，仅需服务器设置  [default: false]");
					general.add(new MinecraftConfigElement(SyncConfig.CATEGORY_GENERAL, "B", "PUSH_CLIENT_MODS", "false", comments));
					comments.clear();
				
				MinecraftConfigCategory rules = new MinecraftConfigCategory(SyncConfig.CATEGORY_RULES);
					comments.add("# 同步config列表(用于不同步整个config文件夹) 请填写完整文件名");
					rules.add(new MinecraftConfigElement(SyncConfig.CATEGORY_RULES, "S", "CONFIG_INCLUDE_LIST", new ArrayList<String>(), comments));
					comments.clear();

					defaultValueList.add("mods");
					comments.add("# 同步目录  默认包括 mods");
					rules.add(new MinecraftConfigElement(SyncConfig.CATEGORY_RULES, "S", "DIRECTORY_INCLUDE_LIST", new ArrayList<String>(defaultValueList), comments));
					comments.clear();
					defaultValueList.clear();
				
					comments.add("# 忽略文件列表，用于客户端模组，将会自动添加至客户端模组目录");
					rules.add(new MinecraftConfigElement(SyncConfig.CATEGORY_RULES, "S", "FILE_IGNORE_LIST", new ArrayList<String>(), comments));
					comments.clear();
				
				MinecraftConfigCategory serverConnection = new MinecraftConfigCategory(SyncConfig.CATEGORY_CONNECTION);
					comments.add("# 服务器端口  [范围: 1 ~ 49151, default: 12585]");
					serverConnection.add(new MinecraftConfigElement(SyncConfig.CATEGORY_CONNECTION, "I", "SERVER_PORT", "12585", comments));
					comments.clear();
					
				MinecraftConfigCategory other = new MinecraftConfigCategory(SyncConfig.CATEGORY_OTHER);
					comments.add("# 语言 默认简体中文");
					other.add(new MinecraftConfigElement(SyncConfig.CATEGORY_OTHER, "S", "LOCALE", Locale.SIMPLIFIED_CHINESE.toString(), comments));
					comments.clear();
					
					comments.add("# 后台输出日志");
					other.add(new MinecraftConfigElement(SyncConfig.CATEGORY_OTHER, "B", "LOG", "false", comments));
					comments.clear();
					
				config.put(SyncConfig.CATEGORY_GENERAL, general);
				config.put(SyncConfig.CATEGORY_RULES, rules);
				config.put(SyncConfig.CATEGORY_CONNECTION, serverConnection);
				config.put(SyncConfig.CATEGORY_OTHER, other);
				
				try {
					config.writeConfig(new MinecraftConfigWriter(Files.newBufferedWriter(configPath)));
				} catch (IOException e) {
					Logger.debug("Failed to write server config file: " + e.getMessage());
					e.printStackTrace();
				}
			
		} else {
			// Client config
			ArrayList<String> comments = new ArrayList<String>();
			
			MinecraftConfigCategory general = new MinecraftConfigCategory(SyncConfig.CATEGORY_GENERAL);
				comments.add("# 拒绝同步客户端模组  [default: false]");
				general.add(new MinecraftConfigElement(SyncConfig.CATEGORY_GENERAL, "B", "REFUSE_CLIENT_MODS", "false", comments));
				comments.clear();

			MinecraftConfigCategory rules = new MinecraftConfigCategory(SyncConfig.CATEGORY_RULES);
				comments.add("# 同步config列表(用于不同步整个config文件夹) 请填写完整文件名");
				rules.add(new MinecraftConfigElement(SyncConfig.CATEGORY_RULES, "S", "CONFIG_INCLUDE_LIST", new ArrayList<String>(), comments));
				comments.clear();
				
				comments.add("# 客户端模组列表.用于同步时避免被删除(默认含有OptiFine)");
				rules.add(new MinecraftConfigElement(SyncConfig.CATEGORY_RULES, "S", "FILE_IGNORE_LIST", new ArrayList<String>(), comments));
				comments.clear();
			
			MinecraftConfigCategory connection = new MinecraftConfigCategory(SyncConfig.CATEGORY_CONNECTION);
				comments.add("# 同步服务器IP地址  只能为IP  [default: 127.0.0.1]");
				connection.add(new MinecraftConfigElement(SyncConfig.CATEGORY_CONNECTION, "S", "SERVER_IP", "127.0.0.1", comments));
				comments.clear();
				
				comments.add("# 同步服务器端口  [range: 1 ~ 49151, default: 12585]");
				connection.add(new MinecraftConfigElement(SyncConfig.CATEGORY_CONNECTION, "I", "SERVER_PORT", "12585", comments));
				comments.clear();
				
			MinecraftConfigCategory other = new MinecraftConfigCategory(SyncConfig.CATEGORY_OTHER);
				comments.add("# 语言");
				other.add(new MinecraftConfigElement(SyncConfig.CATEGORY_OTHER, "S", "LOCALE", Locale.getDefault().toString(), comments));
				comments.clear();
				
			config.put(SyncConfig.CATEGORY_GENERAL, general);
			config.put(SyncConfig.CATEGORY_RULES, rules);
			config.put(SyncConfig.CATEGORY_CONNECTION, connection);
			config.put(SyncConfig.CATEGORY_OTHER, other);
			
			try {
				config.writeConfig(new MinecraftConfigWriter(Files.newBufferedWriter(configPath)));
			} catch (IOException e) {
				Logger.debug("Failed to write client config file: " + e.getMessage());
				e.printStackTrace();
			}
		}
		return true;
	}
	
	public boolean writeConfigUpdates() {
		try {
			config.writeConfig(new MinecraftConfigWriter(Files.newBufferedWriter(configPath,StandardOpenOption.TRUNCATE_EXISTING)));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void init() {
		try {			
			LOCALE = new Locale(config.getEntryByName("LOCALE").getString());
				
			try {				
				FILE_IGNORE_LIST.addAll(config.getEntryByName("FILE_IGNORE_LIST").getList());
				FILE_IGNORE_LIST.add("serversync*");
			} catch (NullPointerException e) {
				// Specific conversion from old config files
				FILE_IGNORE_LIST.addAll(config.getEntryByName("MOD_IGNORE_LIST").getList());
			}
			
			CONFIG_INCLUDE_LIST = config.getEntryByName("CONFIG_INCLUDE_LIST").getList();
			
			if (configType == EConfigType.SERVER) {				
				LOG = config.getEntryByName("LOG").getBoolean();
				PUSH_CLIENT_MODS = config.getEntryByName("PUSH_CLIENT_MODS").getBoolean();
				DIRECTORY_INCLUDE_LIST = config.getEntryByName("DIRECTORY_INCLUDE_LIST").getList();
				SERVER_PORT = config.getEntryByName("SERVER_PORT").getInt();
			} else if (configType == EConfigType.CLIENT) {				
				SERVER_IP = config.getEntryByName("SERVER_IP").getString();
				SERVER_PORT = config.getEntryByName("SERVER_PORT").getInt();
				REFUSE_CLIENT_MODS = config.getEntryByName("REFUSE_CLIENT_MODS").getBoolean();
			}
		} catch(NullPointerException e) {
			Logger.debug("无法定位配置文件");
		}

		Logger.debug("配置文件加载完毕");
	}
}
