package me.DuelsPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class PlayerConfigManager {
	
//	UUID playerID;
//	FileConfiguration pConfig;
	Main main;
	
	public PlayerConfigManager(Main m){

//		UUID playerID = u;
		main = m;

//        FileConfiguration pConfig = YamlConfiguration.loadConfiguration(new File(main.getDataFolder(), playerID + ".yml"));

	}
	
	public void createUser(Player player){
		
		File pFile = new File(main.getDataFolder(), player.getUniqueId() + ".yml");

        if (!(pFile.exists()) ) {

            YamlConfiguration pConfig = YamlConfiguration.loadConfiguration(pFile);

            pConfig.set("Player.Info.UniqueID", player.getUniqueId().toString());
            
            pConfig.set("Player.Stats.Kills", "0");
            pConfig.set("Player.Stats.Deaths", "0");
            pConfig.set("Player.Stats.Wins", "0");
            pConfig.set("Player.Stats.Played", "0");
            
            pConfig.set("Player.DuelRequests", "");
            pConfig.set("Player.DuelRequested", "");
            
            // Don't use saveConfig because the file path has not been created for pConfig file.
            try {
            	pConfig.save(pFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
	
	public YamlConfiguration loadConfig(Player p) {
		File pFile = new File(main.getDataFolder(), p.getUniqueId() + ".yml");
		YamlConfiguration pConfig = YamlConfiguration.loadConfiguration(pFile);
		
		return pConfig;
	}
	
	public void saveConfig(YamlConfiguration pConfig, Player p) {
		File pFile = new File(main.getDataFolder(), p.getUniqueId() + ".yml");
		try {
        	pConfig.save(pFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void increaseGamesPlayed(Player p) {
		YamlConfiguration pConfig = loadConfig(p);
		int stat = Integer.valueOf((String) pConfig.get("Player.Stats.Played"));
		pConfig.set("Player.Stats.Played", String.valueOf(++stat));
		saveConfig(pConfig, p);
	}
	
	public void increaseKills(Player p) {
		YamlConfiguration pConfig = loadConfig(p);
		int stat = Integer.valueOf((String) pConfig.get("Player.Stats.Kills"));
		pConfig.set("Player.Stats.Kills", String.valueOf(++stat));
		saveConfig(pConfig, p);
	}
	
	public void increaseDeaths(Player p) {
		YamlConfiguration pConfig = loadConfig(p);
		int stat = Integer.valueOf((String) pConfig.get("Player.Stats.Deaths"));
		pConfig.set("Player.Stats.Deaths", String.valueOf(++stat));
		saveConfig(pConfig, p);
	}
	
	public void increaseWins(Player p) {
		YamlConfiguration pConfig = loadConfig(p);
		int stat = Integer.valueOf((String) pConfig.get("Player.Stats.Wins"));
		pConfig.set("Player.Stats.Wins", String.valueOf(++stat));
		saveConfig(pConfig, p);
	}
	
	public void showStats(Player p) {
		YamlConfiguration pConfig = loadConfig(p);
		String playerWins = String.valueOf(pConfig.get("Player.Stats.Wins"));
		String playerDeaths = String.valueOf(pConfig.get("Player.Stats.Deaths"));
		String playerPlayed = String.valueOf(pConfig.get("Player.Stats.Played"));
		
		p.sendMessage("§l§nStats");
		p.sendMessage("§aWins: " + playerWins);
		p.sendMessage("§4Deaths: " + playerDeaths);
		p.sendMessage("§9Played: " + playerPlayed);
	}
	
	public void sendDuelRequest(Player p, Player target) {
		YamlConfiguration targetConfig = loadConfig(target);
		targetConfig.set("Player.DuelRequests." + p.getDisplayName(), String.valueOf(p.getUniqueId()));
		saveConfig(targetConfig, target);
		
		YamlConfiguration pConfig = loadConfig(p);
		pConfig.set("Player.DuelRequested." + target.getDisplayName(), String.valueOf(target.getUniqueId()));
		saveConfig(pConfig, p);
	}
	
	public void removeDuelRequest(Player p, Player target) {
		
		List<UUID> IDList = returnDuelRequests(target);
		
		if (IDList.contains(p.getUniqueId())) {
			YamlConfiguration targetConfig = loadConfig(target);
			YamlConfiguration pConfig = loadConfig(p);
			
			targetConfig.set("Player.DuelRequests", targetConfig.getStringList("Player.DuelRequests").remove(String.valueOf(p.getUniqueId())));
			pConfig.set("Player.DuelRequested", pConfig.getStringList("Player.DuelRequested").remove(String.valueOf(target.getUniqueId())));
		
			saveConfig(targetConfig, target);
			saveConfig(pConfig, p);
		}
	}
	
	public List<UUID> returnDuelRequests(Player p) {
		YamlConfiguration pConfig = loadConfig(p);
		List<UUID> IDList = new ArrayList<>();
		
		if (!(pConfig.getConfigurationSection("Player.DuelRequests") == null)) {
			Set<String> pList = pConfig.getConfigurationSection("Player.DuelRequests").getKeys(false); 
			
			for (String pName : pList) {
				IDList.add(Bukkit.getPlayer(pName).getUniqueId());
			}
		}
		
		return IDList;
	}
	
	public List<UUID> returnDuelRequested(Player p) {
		YamlConfiguration pConfig = loadConfig(p);
		List<UUID> IDList = new ArrayList<>();
		
		if (!(pConfig.getConfigurationSection("Player.DuelRequested") == null)) {
			Set<String> pList = pConfig.getConfigurationSection("Player.DuelRequested").getKeys(false); 
			
			for (String pName : pList) {
				IDList.add(Bukkit.getPlayer(pName).getUniqueId());
			}
		}
		
		return IDList;
	}

	public void clearDuelRequests(Player p) {
		YamlConfiguration pConfig = loadConfig(p);
		if (pConfig.getConfigurationSection("Player.DuelRequests") != null) {
			Set<String> duelReqs = pConfig.getConfigurationSection("Player.DuelRequests").getKeys(false);
			
			if (duelReqs != null) {
				for (String target : duelReqs) {
					removeDuelRequest(Bukkit.getPlayer(target), p);
				}
			}
			
	//		pConfig.set("Player.DuelRequests", null); // This should have no use.
		}
		
		
		if (pConfig.getConfigurationSection("Player.DuelRequested") != null) {
		
			Set<String> duelReqd = pConfig.getConfigurationSection("Player.DuelRequested").getKeys(false);
			
			if (duelReqd != null) {
				for (String target : duelReqd) {
					removeDuelRequest(p, Bukkit.getPlayer(target));
				}
			}
			
	//		pConfig.set("Player.DuelRequested", null); // This should have no use.
			saveConfig(pConfig, p);
		}
	}
	
}
