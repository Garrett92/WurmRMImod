package com.imraginbro.wurm.mods.customrmi;

import java.io.File;
import java.rmi.AccessException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.wurmonline.server.LoginHandler;
import com.wurmonline.server.Players;
import com.wurmonline.server.Servers;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.players.PlayerInfo;
import com.wurmonline.server.players.PlayerInfoFactory;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.structures.Structures;

public class CustomMethods {

	static Connection conn = null;
	static Statement stmt = null;

	public static Statement connectSteamDB() {
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:mods"+File.separator+"customRMImod"+File.separator+"steamID.db");
			stmt = conn.createStatement();
		} catch(Exception e) { }
		return stmt;
	}

	public static void closeSteamDB() {
		if (stmt != null) {
			try {
				stmt.close();
				stmt = null;
			} catch (Exception e) { } 
		}
		if (conn != null) { 
			try {
				conn.close();
				conn = null;
			} catch (Exception e) { } 
		}
	}

	public static boolean executeStatement(String sql) {
		try {
			if (stmt != null) {
				stmt.executeUpdate(sql);
				return true;
			}
		} catch(Exception e) { }
		return false;
	}

	public static void updateSteamDB(String playerName, String steamID64) {
		connectSteamDB();
		String sql = "CREATE TABLE IF NOT EXISTS STEAMIDS (WURMID BIGINT PRIMARY KEY, NAME VARCHAR(50), STEAMID BIGINT);";
		if (executeStatement(sql)) {
			long playerID = Players.getInstance().getWurmIdByPlayerName(LoginHandler.raiseFirstLetter(playerName));
			sql = "INSERT INTO STEAMIDS (WURMID, NAME, STEAMID) VALUES ("+playerID+", '"+playerName+"', "+steamID64+");";
			if (!executeStatement(sql)) {
				sql = "UPDATE STEAMIDS SET NAME = '"+playerName+"', STEAMID = "+steamID64+" WHERE WURMID = "+playerID+";";
				executeStatement(sql);
			}
		}
		closeSteamDB();
		System.out.println("[CustomRMI-SteamDB] " + playerName + " is logging in with steamID: " + steamID64);
	}

	public static void validateIntraServerPassword(String intraServerPassword) throws AccessException {
		if (!Servers.localServer.INTRASERVERPASSWORD.equals(intraServerPassword)) {
			throw new AccessException("Access denied.");
		}
	}

	public static Map<Long, String[]> CPgetSteamDBInfo(String intraServerPassword) {
		connectSteamDB();
		String sql = "SELECT * FROM STEAMIDS;";
		try {
			validateIntraServerPassword(intraServerPassword);
			if (stmt != null) {
				Map<Long, String[]> toReturn = new HashMap<Long, String[]>();
				ResultSet rs = stmt.executeQuery(sql);
				while (rs.next()) {
					final Long wurmID = rs.getLong("WURMID");
					final String steamID = rs.getString("STEAMID");
					final String playerName = rs.getString("NAME");
					final String[] built = {playerName,steamID};
					toReturn.put(wurmID, built);
				}
				return toReturn;
			}
		} catch (Exception e) { }
		closeSteamDB();
		return null;
	}

	public static String CPmessagePlayer(String intraServerPassword, long wurmID, byte color, String str) { 
		try {
			validateIntraServerPassword(intraServerPassword);
			Player player = Players.getInstance().getPlayer(wurmID);
			player.getCommunicator().sendNormalServerMessage(str, color);
			return "Message sent to "+player.getName()+"!";
		} catch(Exception e) { 
			return "ERROR=" + e.toString(); 
		} 
	}

	public static String CPchangePower(String intraServerPassword, long wurmID, byte powerLevel) { 
		try {
			validateIntraServerPassword(intraServerPassword);
			Player player = Players.getInstance().getPlayer(wurmID);
			player.setPower(powerLevel);
			player.save();
			player.getCommunicator().sendAlertServerMessage("Your admin power level was changed to " + powerLevel + ".");
			return "Power changed for "+player.getName()+"!";
		} catch(Exception e) { 
			return "ERROR=" + e.toString(); 
		} 
	}

	public static String CPgiveItem(String intraServerPassword, long wurmID, int itemTemplateID, float itemQuality, byte itemRarity, String creator, int itemAmount) { 
		try {
			validateIntraServerPassword(intraServerPassword);
			Item tempItem = null;
			Player player = Players.getInstance().getPlayer(wurmID);
			for (int i = 0; i < itemAmount; i++) {
				tempItem = ItemFactory.createItem(itemTemplateID, itemQuality, itemRarity, creator);
				player.getInventory().insertItem(tempItem);
			}
			player.getCommunicator().sendAlertServerMessage(itemAmount + "x " + tempItem.getName() + " was added to your inventory!");
			return itemAmount + "x of " + tempItem.getName() + " was added to "+player.getName()+"'s inventory";
		} catch(Exception e) { 
			return "ERROR=" + e.toString(); 
		} 
	}

	public static String CPkickPlayer(String intraServerPassword, long wurmID, String str) { 
		try {
			validateIntraServerPassword(intraServerPassword);
			Player player = Players.getInstance().getPlayer(wurmID);
			player.logoutIn(5, str);
			player.getCommunicator().sendAlertServerMessage("You have been kicked from the server. Reason: " + str);
			return "Kicked player "+player.getName()+"!";
		} catch(Exception e) {
			return "ERROR=" + e.toString(); 
		}
	}

	public static Map<Long, String> CPgetAllStructures(String intraServerPassword) {
		try {
			validateIntraServerPassword(intraServerPassword);
			Structure[] allStructures = Structures.getAllStructures();
			Map<Long, String> toReturn = new HashMap<Long, String>();
			for (int i = 0; i < allStructures.length; i++) {
				toReturn.put(allStructures[i].getWurmId(), allStructures[i].getName());
			} 
			return toReturn;
		} catch(Exception e) {
			return null; 
		}
	}

	@SuppressWarnings("rawtypes")
	public static Map<Long, String[]> CPgetAllGuardTowers(String intraServerPassword, ArrayList list) {
		try {
			validateIntraServerPassword(intraServerPassword);
			Map<Long, String[]> toReturn = new HashMap<Long, String[]>();
			for (int i = 0; i < list.size(); i++) {
				final Item gt = (Item) list.get(i);
				String name = "";
				if (gt.getLastOwnerId() > 0) {
					name = PlayerInfoFactory.getPlayerInfoWithWurmId(gt.getLastOwnerId()).getName();
				}
				toReturn.put(gt.getWurmId(), new String[] {Integer.toString(gt.getTileX()), Integer.toString(gt.getTileY()), Long.toString(gt.getLastOwnerId()), name, Float.toString(gt.getCurrentQualityLevel()), Float.toString(gt.getDamage())});
			}
			return toReturn;
		} catch(Exception e) {
			System.out.println(e.getMessage());
			return null; 
		}
	}

	public static boolean CPisPlayerOnline(String intraServerPassword, long wurmID) {
		try {
			validateIntraServerPassword(intraServerPassword);
			PlayerInfo pinfo = PlayerInfoFactory.createPlayerInfo(Players.getInstance().getNameFor(wurmID));
			return pinfo.isOnlineHere();
		} catch(Exception e) {
			return false;
		}
	}

}
