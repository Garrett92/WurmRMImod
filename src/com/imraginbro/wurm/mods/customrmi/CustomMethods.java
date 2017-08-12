package com.imraginbro.wurm.mods.customrmi;

import java.io.File;
import java.rmi.AccessException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.wurmonline.server.LoginHandler;
import com.wurmonline.server.Message;
import com.wurmonline.server.Players;
import com.wurmonline.server.Server;
import com.wurmonline.server.Servers;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.players.PlayerInfo;
import com.wurmonline.server.players.PlayerInfoFactory;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.structures.Structures;

public class CustomMethods {

	static LinkedList<Message> messagesLog = new LinkedList<Message>();

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

	public static Map<Long, String[]> CPgetSteamDBInfo(String var_intraServerPassword) {
		connectSteamDB();
		String sql = "SELECT * FROM STEAMIDS;";
		try {
			validateIntraServerPassword(var_intraServerPassword);
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

	public static String CPchangePower(String var_intraServerPassword, long var_wurmID, byte var_powerLevel) { 
		try {
			validateIntraServerPassword(var_intraServerPassword);
			Player player = Players.getInstance().getPlayer(var_wurmID);
			player.setPower(var_powerLevel);
			player.save();
			player.getCommunicator().sendAlertServerMessage("Your admin power level was changed to " + var_powerLevel + ".");
			return "Power changed for "+player.getName()+"!";
		} catch(Exception e) { 
			return "ERROR=" + e.toString(); 
		} 
	}

	public static String CPgiveItem(String var_intraServerPassword, long var_wurmID, int var_itemTemplateID, float var_itemQuality, byte var_itemRarity, String var_creator, int var_itemAmount) { 
		try {
			validateIntraServerPassword(var_intraServerPassword);
			Item tempItem = null;
			Player player = Players.getInstance().getPlayer(var_wurmID);
			for (int i = 0; i < var_itemAmount; i++) {
				tempItem = ItemFactory.createItem(var_itemTemplateID, var_itemQuality, var_itemRarity, var_creator);
				player.getInventory().insertItem(tempItem);
			}
			player.getCommunicator().sendAlertServerMessage(var_itemAmount + "x " + tempItem.getName() + " was added to your inventory!");
			return var_itemAmount + " of " + tempItem.getName() + " was added to "+player.getName()+"'s inventory";
		} catch(Exception e) { 
			return "ERROR=" + e.toString(); 
		} 
	}

	public static String CPkickPlayer(String var_intraServerPassword, long var_wurmID, String var_str) { 
		try {
			validateIntraServerPassword(var_intraServerPassword);
			Player player = Players.getInstance().getPlayer(var_wurmID);
			player.logoutIn(5, var_str);
			player.getCommunicator().sendAlertServerMessage("You have been kicked from the server. Reason: " + var_str);
			return "Kicked player "+player.getName()+"!";
		} catch(Exception e) {
			return "ERROR=" + e.toString(); 
		}
	}

	public static Map<Long, String> CPgetAllStructures(String var_intraServerPassword) {
		try {
			validateIntraServerPassword(var_intraServerPassword);
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
	public static Map<Long, String[]> CustomgetAllGuardTowers(String var_intraServerPassword, ArrayList var_list) {
		try {
			validateIntraServerPassword(var_intraServerPassword);
			Map<Long, String[]> toReturn = new HashMap<Long, String[]>();
			for (int i = 0; i < var_list.size(); i++) {
				final Item gt = (Item) var_list.get(i);
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

	public static boolean CPisPlayerOnline(String var_intraServerPassword, long var_wurmID) {
		try {
			validateIntraServerPassword(var_intraServerPassword);
			PlayerInfo pinfo = PlayerInfoFactory.createPlayerInfo(Players.getInstance().getNameFor(var_wurmID));
			return pinfo.isOnlineHere();
		} catch(Exception e) {
			return false;
		}
	}

	public static String CPmessagePlayer(String var_intraServerPassword, long var_wurmID, byte var_type, String var_str) { 
		try {
			validateIntraServerPassword(var_intraServerPassword);
			Player player = Players.getInstance().getPlayer(var_wurmID);
			player.getCommunicator().sendNormalServerMessage(var_str, var_type);
			return "Message sent to "+player.getName()+"!";
		} catch(Exception e) { 
			return "ERROR=" + e.toString(); 
		} 
	}

	public static String CPmessagePlayerCustom(String var_intraServerPassword, long var_wurmID, String var_windowTitle, String var_senderName, String var_message, int var_red, int var_green, int var_blue) {
		try {
			validateIntraServerPassword(var_intraServerPassword);
			Player player = Players.getInstance().getPlayer(var_wurmID);
			if (!player.hasLink()) {
				return "Player does not have link to server (may have recently disconnected)!";
			}
			Message mess = new Message(player, (byte)3, var_windowTitle, "<" + var_senderName + "> " + var_message);
			mess.setColorR(var_red);
			mess.setColorG(var_green);
			mess.setColorB(var_blue);
			mess.setReceiver(var_wurmID);
			Server.getInstance().addMessage(mess);
			return "Message sent to "+player.getName()+"!";
		} catch(Exception e) {
			return "ERROR=" + e.toString(); 
		}
	}

	public static String CPmessagePlayerWarnPM(String var_intraServerPassword, long var_wurmID, String var_windowTitle, String var_warnMessage) { 
		try {
			validateIntraServerPassword(var_intraServerPassword);
			Player player = Players.getInstance().getPlayer(var_wurmID);
			if (!player.hasLink()) {
				return "Player does not have link to server (may have recently disconnected)!";
			}
			player.showPMWarn(var_windowTitle, var_warnMessage);
			return "Message sent to "+player.getName()+"!";
		} catch(Exception e) { 
			return "ERROR=" + e.toString(); 
		} 
	}

	public static String CPmessagePlayerPM(String var_intraServerPassword, long var_wurmID, String var_senderName, String var_windowTitle, String var_message) { 
		try {
			validateIntraServerPassword(var_intraServerPassword);
			Player player = Players.getInstance().getPlayer(var_wurmID);
			if (!player.hasLink()) {
				return "Player does not have link to server (may have recently disconnected)!";
			}
			player.showPM(var_senderName, var_windowTitle, var_message, false);;
			return "Message sent to "+player.getName()+"!";
		} catch(Exception e) { 
			return "ERROR=" + e.toString(); 
		} 
	}

	public static String CPplayerSendPopup(String var_intraServerPassword, long var_wurmID, String var_title, String var_message) {
		try {
			validateIntraServerPassword(var_intraServerPassword);
			Player player = Players.getInstance().getPlayer(var_wurmID);
			if (!player.hasLink()) {
				return "Player does not have link to server (may have recently disconnected)!";
			}
			player.sendPopup(var_title, var_message);
			return "Popup sent to "+player.getName()+"!";
		} catch(Exception e) { 
			return "ERROR=" + e.toString(); 
		} 
	}
	
	/*public static String CPtest() {

		Players.getInstance().getPlayer(1).mute(mute, reason, expiry);
		//Players.getInstance().getPlayer(0).
		Connection dbcon = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			dbcon = DbConnector.getLoginDbCon();
			ps = dbcon.prepareStatement("");
			rs = ps.executeQuery();

		} catch (Exception e) {
			return "ERROR=" + e.toString(); 
		} finally {
			DbUtilities.closeDatabaseObjects(ps, rs);
            DbConnector.returnConnection(dbcon);
		}
		return "";
	}*/

}
