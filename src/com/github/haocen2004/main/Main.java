package com.github.haocen2004.main;

import java.io.*;

import org.json.*;

import com.github.haocen2004.net.Download;

public class Main {

	public static void main(String[] args) {
		System.out.println("Booting...");

		File m = new File(".\\.minecraft\\mods");
		if (!m.exists())
			m.mkdirs();

		File config = new File(".\\config.json");
		if (!config.exists()) {
			try {

				System.out.println("Config.json not found!");

				config.createNewFile();
				FileWriter cfgout = new FileWriter(config);

				JSONObject configs = new JSONObject();

				configs.put("Version", 1);
				configs.put("URL", "https://github.com/Haocen2004/HellocraftUpdater/raw/master/files.json");

				cfgout.write(configs.toString());

				cfgout.close();

				System.out.println("Finished.");

			} catch (IOException e) {
				System.out.println("Failed.");
				e.printStackTrace();
			}
		}

		try {
			FileReader cfgin = new FileReader(config);
			BufferedReader cfgins = new BufferedReader(cfgin);

			JSONObject configs = new JSONObject(cfgins.readLine());

			Download.single(configs.getString("URL"), ".\\files.json");

			File f = new File(".\\files.json");
			cfgin = new FileReader(f);
			cfgins = new BufferedReader(cfgin);
			JSONObject files = new JSONObject(cfgins.readLine());
			JSONArray add = files.getJSONArray("add");
			int i = 0;
			JSONObject file;
			File dlf = null;
			while (i < add.length()) {

				file = add.getJSONObject(i);

				if (file.getString("type").equalsIgnoreCase("mod")) {
					
					dlf = new File(".\\.minecraft\\mods\\" + file.getString("name"));
					if (dlf.exists()) dlf.delete();
					
					Download.single(file.getString("url"), ".\\.minecraft\\mods\\" + file.getString("name"));
				}
				if (file.getString("type").equalsIgnoreCase("lib")) {
					
					dlf = new File(".\\.minecraft\\libraries\\" + file.getString("name"));
					if (dlf.exists()) dlf.delete();
					
					Download.single(file.getString("url"), ".\\.minecraft\\libraries\\" + file.getString("name"));
				}
				i++;
			}
			JSONArray remove = files.getJSONArray("remove");
			i = 0;
			file = null;
			File delf = null;
			while (i < remove.length()) {

				file = remove.getJSONObject(i);

				if (file.getString("type").equals("mod")) {

					delf = new File(".\\.minecraft\\mods\\" + file.getString("name"));
					if (delf.exists()) delf.delete();

				}

				i++;
			}
			
			
			cfgins.close();

		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}

		System.out.println("Over..");
	}

}
