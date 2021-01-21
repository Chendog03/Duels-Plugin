package me.DuelsPlugin;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener{
	List<DuelObject> queue; //Should start empty on reload.
	List<Player> pool;
	List<Player> fighters;
	
	Location p1Start; 
	Location p2Start;
	Location hubSpawn;
	
	Boolean duelIsOngoing = false;
	
	HashMap<UUID, PermissionAttachment> perms = new HashMap<UUID, PermissionAttachment>();
	
	Main mainInstance;
	PlayerConfigManager configManager;

	@Override
	public void onEnable() {
		queue = new ArrayList<>();
		pool = new ArrayList<>();
		fighters = new ArrayList<>();
		
		getServer().getPluginManager().registerEvents(this, this);
		
		p1Start = loadLocation("p1Start");
		p2Start = loadLocation("p2Start");
		hubSpawn = loadLocation("spawn");
		
		mainInstance = Main.getPlugin(Main.class);
		
		configManager = new PlayerConfigManager(mainInstance);
		
		getServer().createWorld(new WorldCreator("Duel World"));
		
	}
	
	@Override
	public void onDisable() {
		
	}
	
	public void duelStart(List<Player> ... pList) { 
		duelIsOngoing = true;
			
		if (pList == null) { 		//Get first 2 in queue
			if (queue.size() > 0) {
				DuelObject duel = queue.get(0);
				queue.remove(duel);
				
				for (Player p : duel.players) {
					removeFromPool(p);
				}
				fighters.addAll(duel.players);
			}
			
		} else { 						//Use players listed
			for (Player p : pList[0]) {
				fighters.add(p);
			}
			removeFromQueue(fighters);
			
			for (Player p : pList[0]) {
				removeFromPool(p);
			}
		}
		
		for (Player fighter : fighters) {
			fighter.getInventory().addItem(new ItemStack(Material.STONE_SWORD, 1));
			fighter.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE, 1));
			fighter.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS, 1));
			fighter.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS, 1));
			
			if (Integer.valueOf(fighters.indexOf(fighter)).equals(0)) {
				fighter.teleport(p1Start);
			} else {
				fighter.teleport(p2Start);
			}
			
			configManager.increaseGamesPlayed(fighter);
			
			fighter.sendMessage("§l§cDUEL HAS STARTED");
			fighter.sendMessage("§cOpponent(s): " );
			for (Player opponent : fighters) {
				if (opponent != fighter) {
					fighter.sendMessage(opponent.getDisplayName());
				}
			}
			
			perms.get(fighter.getUniqueId()).setPermission("endDuelPerm", true);
		}
	}
	
	public void duelEnd() { 
		duelIsOngoing = false;
		
		for (Player fighter : fighters) {
			Bukkit.broadcastMessage("§l§6" + fighter.getDisplayName() + " won the duel!");
			
			configManager.increaseWins(fighter);
			
			fighter.getInventory().clear();
			fighter.setHealth(20.0);
			fighter.teleport(hubSpawn);
			
			perms.get(fighter.getUniqueId()).unsetPermission("endDuelPerm");
		}
		
		fighters = new ArrayList<>();
		
		if (queue.size() >= 1) {
			duelStart(null);
		}
	}
	
	public void saveLocation(Location location, String name) {
		getConfig().set(name + ".world", location.getWorld().getName());
		getConfig().set(name + ".x", location.getX());
		getConfig().set(name + ".y", location.getY());
		getConfig().set(name + ".z", location.getZ());
		getConfig().set(name + ".yaw", location.getYaw());
		getConfig().set(name + ".pitch", location.getPitch());
		saveConfig();
	}
	
	public Location loadLocation(String name) {
		String world = getConfig().getString(name + ".world");
		Double x = getConfig().getDouble(name + ".x");
		Double y = getConfig().getDouble(name + ".y");
		Double z = getConfig().getDouble(name + ".z");
		Float yaw = (float) getConfig().getDouble(name + ".yaw");
		Float pitch = (float) getConfig().getDouble(name + ".pitch");
		
		return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
	}
	
	public void addToQueue(List<Player> ... pList) {
		DuelObject duel = new DuelObject(pList[0]);
		queue.add(duel);
		for (Player p : pList[0]) {
			p.sendMessage("You have been added to the queue.");
			removeFromPool(p);
		}
		checkQueue();
	}
	
	public void removeFromQueue(List<Player> ... pList) {
		for (DuelObject duel : queue) {
			for (Player p : pList[0]) {
				if (duel.players.contains(p)) {
					if (duel.players.size() < 2) { //If only one player left in duel, will happen unless duel starting with more than 2 players.
						if (pool.size() > 0) {
							duel.players.get(0).sendMessage("Duel partner reloaded due to player leaving server.");
							duel.players.add(pool.get(0)); //Sub in new player waiting.
							duel.players.remove(p);
						} else {
							duel.players.get(0).sendMessage("Returned to pool as opponent quit and no players waiting.");
							queue.remove(duel);
						}
					}
				}
			}
		}
	}
	
	public void addToPool(Player p) {
		if (!(pool.contains(p))) {
			pool.add(p);
			p.sendMessage("You have joined the waiting pool. Current pool size: " + String.valueOf(pool.size()));
		} else {
			p.sendMessage(getConfig().getString("error-already-queued"));
		}
	}
	
	public void removeFromPool(Player p) {
		if (pool.contains(p)) {
			pool.remove(p);
		}
	}

	public void checkPool() {
		if (pool.size() > 1) {
			addToQueue(new ArrayList<>(Arrays.asList(pool.get(0), pool.get(1))));
			checkQueue();
		}
	}
	
	public void checkQueue() {
		if (!duelIsOngoing) {
			if (queue.size() > 0) {
				duelStart(null);
			}
		}
	}

	public void sendDuelRequest(Player p, Player target) {
		List<UUID> duelReqs = configManager.returnDuelRequested(p);
		if (duelReqs != null) {
			if (!(duelReqs.contains(target.getUniqueId()))) {
				configManager.sendDuelRequest(p, target);
				
				duelReqs = configManager.returnDuelRequested(p); // Reload duelReqs since it has been appended
				
				target.sendMessage("§aYou have a new duel request from " + p.getDisplayName());
				target.sendMessage("§aUse /duelAccept <name> or /duelDeny <name> to act on the challenge.");
			    
			    p.sendMessage("Duel requests sent to: ");
			    
			    for (UUID pID : duelReqs) {
			    	p.sendMessage(Bukkit.getPlayer(pID).getDisplayName());
			    }
			} else {
				p.sendMessage("§cDuel request already sent to that player.");
			}
		}
	}
	
	public void acceptDuelRequest(Player p, Player target) {
		configManager.clearDuelRequests(p);
		p.sendMessage("§aYou have accepted the duel request from §l" + target.getDisplayName() + "§r§a.  Cleared your duel reuqests.");
		configManager.clearDuelRequests(target);
		target.sendMessage("§l§a" + p.getDisplayName() + "§r§a has accepted your duel request. Cleared your duel reuqests.");
		
		addToQueue(new ArrayList<>(Arrays.asList(p, target)));
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {	
		
		if(cmd.getName().equals("duel")) {
			
			if(sender instanceof Player) {
				
				Player p = (Player) sender;
				
				if (p.hasPermission("duelPerm")) {
				
					if (args.length == 0) {
						
						for (DuelObject duel : queue) {
							if (duel.players.contains(p)) {
								p.sendMessage(getConfig().getString("error-already-queued"));
								return true;
							}
						}
						
						addToPool(p);
						
						if (queue.size() <= 5) {  
							checkPool();
						}
						
					} else if (args.length == 1) {
						
						Player p2 = Bukkit.getPlayer(args[0]);
						if (p != p2) {
							if ((p2 != null) && (p2.isOnline())) { 
								for (DuelObject duel : queue) {
									if ((duel.players.contains(p)) || (duel.players.contains(p2))) {
										
										p.sendMessage(getConfig().getString("error-already-queued"));
										return true;
									}
								}
								
								sendDuelRequest(p, p2);	
								
							} else {
								
								p.sendMessage(getConfig().getString("error-player-not-online").replaceAll("%player%", args[0]));
								return true;
							}	
						} else {
							
							p.sendMessage("You cannot duel yourself.");
							return true;
						}
					} else if (args.length > 1) {
						p.sendMessage("You can only duel one person. Use /duel to be added to the queue, /duel <name> to challenge an opponent.");
						return true;
					}
					
					if ((queue.size() > 1) && (!duelIsOngoing)) {
						duelStart(null);
					}
				} else {
					p.sendMessage(getConfig().getString("error-permissions").replaceAll("%perm%", "duel"));
				}
				
			} else {
				
				sender.sendMessage("This is a player command only.");
				return true;
			}
			return false;
		} 
		
		if (cmd.getName().equals("duelAccept")) {
			
			if (sender.hasPermission("duelPerm")) {
				
				if (sender instanceof Player) {
					
					Player p = (Player) sender;
					
					List<UUID> duelReqs = configManager.returnDuelRequests(p);
					
					if (args.length == 0) {
						
						if (duelReqs.size() > 0) {
							p.sendMessage("You have duel requests from: ");
							for (UUID targetID : duelReqs) {
								p.sendMessage(Bukkit.getPlayer(targetID).getDisplayName());
							}
							
							p.sendMessage("§aUse /duelAccept <name> or /duelDeny <name> to act on the challenge.");
						} else {
							p.sendMessage(getConfig().getString("error-no-requests"));
						}
					
					} else {
						
						Player target = Bukkit.getPlayer(args[0]);
						if ((target != null) && (target.isOnline())) {
							
							if (duelReqs.contains(target.getUniqueId())) {
								
								acceptDuelRequest(p, target);
								
							} else {
								p.sendMessage(getConfig().getString("error-no-request-from").replaceAll("%target", args[0]));
							}
							
						} else {
							p.sendMessage(getConfig().getString("error-player-not-online").replaceAll("%player%", args[0]));
						}
					}
				}
			}
		}
		
		if (cmd.getName().equals("duelDeny")) {
			if (sender.hasPermission("duelPerm")) {
				
				if (sender instanceof Player) {
					
					Player p = (Player) sender;
					
					List<UUID> duelReqs = configManager.returnDuelRequests(p);
					
					if (args.length == 0) {
						
						if (duelReqs.size() > 0) {
							Player mostRecentReq = Bukkit.getPlayer(duelReqs.get(duelReqs.size()-1)); // Likely index error.
							configManager.removeDuelRequest(mostRecentReq, p);
							
							p.sendMessage(getConfig().getString("info-removed-recent-request").replaceAll("%target%", mostRecentReq.getDisplayName()));
						} else {
							p.sendMessage(getConfig().getString("error-no-requests"));
						}
					} else {
						Player target = Bukkit.getPlayer(args[0]);
						if ((target != null) && (target.isOnline())) {
							
							if (duelReqs.contains(target.getUniqueId())) {
								
								configManager.removeDuelRequest(target, p);
								p.sendMessage("Removed duel request from " + target.getDisplayName());
								
							} else {
								p.sendMessage(getConfig().getString("error-no-request-from").replaceAll("%target%", args[0]));
							}
							
						} else {
							p.sendMessage(getConfig().getString("error-player-not-online").replaceAll("%player%", args[0]));
						}
					}
				}
			}
		}
		
		if(cmd.getName().equals("showQueue")) {
			
			if (sender.hasPermission("showQueuePerm")) {
			
				sender.sendMessage(getConfig().getString("info-display-list").replaceAll("%list%", "Queue"));
				
				for (DuelObject duel : queue) {
					sender.sendMessage("§bDuel number " + String.valueOf(queue.indexOf(duel) + 1));
					for (Player player : duel.players)
						sender.sendMessage("\u00A7" + String.valueOf(duel.players.indexOf(player)) + player.getDisplayName());
				}
				
				return true;
			
			} else {
				sender.sendMessage(getConfig().getString("error-permissions").replaceAll("%perm%", "showQueue"));
			}
			
			return false;
		}
		
		if(cmd.getName().equals("showPool")) {
			
			if (sender.hasPermission("showQueuePerm")) {
			
				sender.sendMessage("Pool as follows: ");
				
				for (Player player : pool) {
					sender.sendMessage(player.getDisplayName());
				}
				
				return true;
			
			} else {
				sender.sendMessage(getConfig().getString("error-permissions").replaceAll("%perm%", "showQueue"));
			}
			
			return false;
		}
		
		if(cmd.getName().equals("setPos1")) {
			
			if (sender instanceof Player) {
				
				Player p = (Player) sender;
					
				if (p.hasPermission("setPosPerm")) {
					
					p1Start = p.getLocation();
					
					saveLocation(p1Start, "p1Start");
					p1Start = loadLocation("p1Start");
					
					sender.sendMessage("New duel location set for Player 1.");
					
//					sender.sendMessage(String.valueOf(p1Start.getX()) + ", " +
//							String.valueOf(p1Start.getY()) + ", " +
//							String.valueOf(p1Start.getZ()));
					
					return true;
					
				} else {
					p.sendMessage(getConfig().getString("error-permissions").replaceAll("%perm%", "setPosition"));
				}
				
				return true;
			}
			return false;
		}
		
		if(cmd.getName().equals("setPos2")) {
			
			if(sender instanceof Player) {
				
				Player p = (Player) sender;
				
				if (p.hasPermission("setPosPerm")) {
				
					p2Start = p.getLocation();
					
					sender.sendMessage("New duel location set for Player 2.");
					
					saveLocation(p2Start, "p2Start");
					p2Start = loadLocation("p2Start");
					
	//				sender.sendMessage(String.valueOf(p2Start.getX()) + ", " +
	//						String.valueOf(p2Start.getY()) + ", " +
	//						String.valueOf(p2Start.getZ()) + ", " +
	//						String.valueOf(p2Start.getWorld()));
					
					return true;
				} else {
					p.sendMessage(getConfig().getString("error-permissions").replaceAll("%perm%", "setPosition"));
				}
				return true;
			}
			return false;
		}
		
		if(cmd.getName().equals("setHubSpawn")) {
			
			if(sender instanceof Player) {
				
				Player p = (Player) sender;
				
				if (p.hasPermission("setPosPerm")) {
				
					hubSpawn = p.getLocation(); 
					
					sender.sendMessage("New spawn location set.");
					
					saveLocation(hubSpawn, "spawn");
					hubSpawn = loadLocation("spawn");
					
	//				sender.sendMessage(String.valueOf(hubSpawn.getX()) + ", " +
	//						String.valueOf(hubSpawn.getY()) + ", " +
	//						String.valueOf(hubSpawn.getZ()) + ", " +
	//						String.valueOf(hubSpawn.getWorld()));
				
					return true;
					
				} else {
					p.sendMessage(getConfig().getString("error-permissions").replaceAll("%perm%", "setPosition"));
				}
				return true;
			}
			return false;
		}
		
		if(cmd.getName().equals("showDuel")) {
			
			if (sender.hasPermission("duelPerm")) {
				
				if (fighters.size() > 0) {
					
					sender.sendMessage("Current duel is between: ");
					
					String message = fighters.get(0).getDisplayName();
					
					for (Player fighter : fighters.subList(1, fighters.size())) {
						message += " and " + fighter.getDisplayName();
					}
					
					sender.sendMessage(message);
					return true;
				}
			} else {
				sender.sendMessage(getConfig().getString("error-permissions").replaceAll("%perm%", "duel"));
			}
		}
		
		if(cmd.getName().equals("startDuel")) {
			if (sender.hasPermission("startDuelPerm")) {
				if (duelIsOngoing) {
					duelEnd();
				}
				
				if (args.length == 0) {
					duelStart(null);
				} else {
				
					List<Player> pList = new ArrayList<Player>();
					
					for (String x : args) {
						Player p = Bukkit.getPlayer(x);
						if ((p != null) && (p.isOnline())) {
							pList.add(p);
						} else {
							sender.sendMessage(getConfig().getString("error-player-not-online").replaceAll("%player%", args[0]));
							return true;
						}
					}
					
					duelStart(pList);
				}
				
			} else {
				sender.sendMessage(getConfig().getString("error-permissions").replaceAll("%perm%", "startDuel/endDuel"));
			}
			return false;
		}
		
		if(cmd.getName().equals("endDuel")) {
			if (sender.hasPermission("endDuelPerm")) {
				if (duelIsOngoing) {
					
					duelEnd();
				}
			} else {
				sender.sendMessage(getConfig().getString("error-permissions").replaceAll("%perm%", "startDuel/endDuel"));
			}
		}
		
		if (cmd.getName().equals("showStats")) {
			if (sender.hasPermission("duelPerm")) {
				if (sender instanceof Player) {
					Player p = (Player) sender;
					configManager.showStats(p);
				}
			}
		}
		
		return false;
	}
	
	@EventHandler
	public void onEntityDamageEvent(EntityDamageEvent evt) {
		Entity entity = evt.getEntity();
		
		if (entity instanceof Player) {
			Player p = (Player) entity;
			
			if (p.getHealth() - evt.getFinalDamage() <= 0) {
				evt.setCancelled(true);
				
				configManager.increaseDeaths(p);
				
				p.setHealth(20.0);
				p.getInventory().clear();
				p.teleport(hubSpawn);
				
				if (fighters.contains(p)) {
					perms.get(p.getUniqueId()).unsetPermission("endDuelPerm");
					
					fighters.remove(p);
					
					if (fighters.size() < 2) {
						duelEnd();
					}
				}
			}
		}
	}
	  
	@EventHandler
	public void onLogin(PlayerLoginEvent evt) {
		Player p = evt.getPlayer();
		
		System.out.println(p.getDisplayName().toString() + " has logged in."); 
	    p.teleport(hubSpawn);
	    
	    PermissionAttachment attachment = p.addAttachment(mainInstance);
	    perms.put(p.getUniqueId(), attachment);
	    
	    PermissionAttachment pperms = perms.get(p.getUniqueId());
	    pperms.setPermission("duelPerm", true);
	    
	    configManager.createUser(p);
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent evt) {
		Player p = evt.getPlayer();
		
		p.teleport(hubSpawn);
	
		System.out.println(p.getDisplayName().toString() + " has logged out."); 
		
		removeFromQueue(new ArrayList<>(Arrays.asList(p)));
		removeFromPool(p);
		
		if (fighters.contains(p)) {
			fighters.remove(p);
			p.getInventory().clear();
			perms.get(p.getUniqueId()).setPermission("endDuelPerm", true);
		}
		
		configManager.clearDuelRequests(p);
	}
}

