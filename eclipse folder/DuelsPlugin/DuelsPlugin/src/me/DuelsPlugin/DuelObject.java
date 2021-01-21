package me.DuelsPlugin;

import java.util.*;

import org.bukkit.entity.Player;

public class DuelObject {
	
	public UUID id;
	public List<Player> players;

	public DuelObject(List<Player> ... pList) {
		players = new ArrayList<>();
		
		for (Player p : pList[0]) {
			players.add(p);
			System.out.println(p.getDisplayName());
		}
		
		id = UUID.randomUUID();
	}
}
