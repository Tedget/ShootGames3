package de.tedget.shootgames.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class GunsLoader {
  private YamlConfiguration config;
  private File configFile;
  Plugin plugin;
  
  public GunsLoader(Plugin main) {
    this.plugin = main;
    load();
  }
  
  public YamlConfiguration getConfig() {
    return this.config;
  }
  
  public void save() {
    try {
      this.config.save(this.configFile);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public void load() {
    this.configFile = new File("plugins/ShootGames/guns.yml");
    if (!this.configFile.exists()) {
      try {
        new File("plugins/ModernWeapons/").mkdirs();
        this.configFile.createNewFile();
        copyResourceYAML(getClass().getResourceAsStream("guns.yml"), this.configFile);
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
    this.config = new YamlConfiguration();
    try {
      this.config.load(this.configFile);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
  }
  
  public void copyResourceYAML(InputStream source, File target) {
    BufferedWriter writer = null;
    BufferedReader reader = new BufferedReader(new InputStreamReader(source));
    try {
      writer = new BufferedWriter(new FileWriter(target));
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    try {
      String buffer = "";
      while ((buffer = reader.readLine()) != null) {
        writer.write(buffer);
        writer.newLine();
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    finally {
      try {
        if (writer != null) {
          writer.close();
        }
        if (reader != null) {
          reader.close();
        }
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
