package com.imraginbro.wurm.mods.customrmi;

import java.util.Properties;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;

public class CustomRMImod implements WurmServerMod, Configurable, Initable {

	private boolean customRMIactive = true;
	private boolean steamIdLoggeractive = true;
	private boolean guardTowersLoader = false;

	@Override
	public void configure(Properties prop) {
		this.customRMIactive = Boolean.valueOf(prop.getProperty("customRMIactive", Boolean.toString(customRMIactive)));
		this.steamIdLoggeractive = Boolean.valueOf(prop.getProperty("steamIdLoggeractive", Boolean.toString(steamIdLoggeractive)));
		this.guardTowersLoader = Boolean.valueOf(prop.getProperty("guardTowersLoader", Boolean.toString(guardTowersLoader)));
	}

	@Override
	public void init() {
		ClassPool classPool = HookManager.getInstance().getClassPool();
		if (steamIdLoggeractive) {
			try {
				CtClass loginHandler = classPool.get("com.wurmonline.server.LoginHandler");
				CtMethod handleLoginMethod = loginHandler.getDeclaredMethod("handleLogin");
				handleLoginMethod.insertAfter("com.imraginbro.wurm.mods.customrmi.CustomMethods.updateSteamDB(name, steamIDAsString);");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		boolean guardTowerInjSuc = false;
		if (guardTowersLoader) {
			try {
				CtClass arrListClass = ClassPool.getDefault().get("java.util.ArrayList");
				CtClass ZonesClass = classPool.get("com.wurmonline.server.zones.Zones");
				CtField guardTowerListField = new CtField(arrListClass, "guardTowerList", ZonesClass);
				guardTowerListField.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
				ZonesClass.addField(guardTowerListField, CtField.Initializer.byNew(arrListClass));
				CtMethod addGuardTowerMethod = ZonesClass.getDeclaredMethod("addGuardTower");
				CtMethod removeGuardTowerMethod = ZonesClass.getDeclaredMethod("removeGuardTower");
				removeGuardTowerMethod.insertBefore("guardTowerList.remove(tower);");
				addGuardTowerMethod.insertBefore("guardTowerList.add(tower);");
				guardTowerInjSuc = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (customRMIactive) {
			try {

				CtClass webIfaceImpl = classPool.get("com.wurmonline.server.webinterface.WebInterfaceImpl");
				CtMethod CPmessagePlayerImpl = CtNewMethod.make("public String CPmessagePlayer(String intraServerPassword, long wurmID, byte color, String str) throws java.rmi.RemoteException { return com.imraginbro.wurm.mods.customrmi.CustomMethods.CPmessagePlayer(intraServerPassword, wurmID, color, str); }", webIfaceImpl);
				CtMethod CPchangePowerImpl = CtNewMethod.make("public String CPchangePower(String intraServerPassword, long wurmID, byte powerLevel) throws java.rmi.RemoteException { return com.imraginbro.wurm.mods.customrmi.CustomMethods.CPchangePower(intraServerPassword, wurmID, powerLevel); }", webIfaceImpl);
				CtMethod CPgiveItemImpl = CtNewMethod.make("public String CPgiveItem(String intraServerPassword, long wurmID, int itemTemplateID, float itemQuality, byte itemRarity, String creator, int itemAmount) throws java.rmi.RemoteException { return com.imraginbro.wurm.mods.customrmi.CustomMethods.CPgiveItem(intraServerPassword, wurmID, itemTemplateID, itemQuality, itemRarity, creator, itemAmount); }", webIfaceImpl);
				CtMethod CPkickPlayerImpl = CtNewMethod.make("public String CPkickPlayer(String intraServerPassword, long wurmID, String str) throws java.rmi.RemoteException { return com.imraginbro.wurm.mods.customrmi.CustomMethods.CPkickPlayer(intraServerPassword, wurmID, str); }", webIfaceImpl);
				CtMethod CPgetAllStructuresImpl = CtNewMethod.make("public java.util.Map CPgetAllStructures(String intraServerPassword) throws java.rmi.RemoteException { return com.imraginbro.wurm.mods.customrmi.CustomMethods.CPgetAllStructures(intraServerPassword); }", webIfaceImpl);
				CtMethod CPisPlayerOnlineImpl = CtNewMethod.make("public boolean CPisPlayerOnline(String intraServerPassword, long wurmID) throws java.rmi.RemoteException { return com.imraginbro.wurm.mods.customrmi.CustomMethods.CPisPlayerOnline(intraServerPassword, wurmID); }", webIfaceImpl);
				CtMethod CPgetSteamDBInfoImpl = CtNewMethod.make("public java.util.Map CPgetSteamDBInfo(String intraServerPassword) throws java.rmi.RemoteException { return com.imraginbro.wurm.mods.customrmi.CustomMethods.CPgetSteamDBInfo(intraServerPassword); }", webIfaceImpl);

				CtClass webIface = classPool.get("com.wurmonline.server.webinterface.WebInterface");
				CtMethod CPmessagePlayer = CtNewMethod.make("String CPmessagePlayer(String intraServerPassword, long wurmID, byte color, String str) throws java.rmi.RemoteException;", webIface);
				CtMethod CPchangePower = CtNewMethod.make("String CPchangePower(String intraServerPassword, long wurmID, byte powerLevel) throws java.rmi.RemoteException;", webIface);
				CtMethod CPgiveItem = CtNewMethod.make("String CPgiveItem(String intraServerPassword, long wurmID, int itemTemplateID, float itemQuality, byte itemRarity, String creator, int itemAmount) throws java.rmi.RemoteException;", webIface);
				CtMethod CPkickPlayer = CtNewMethod.make("String CPkickPlayer(String intraServerPassword, long wurmID, String str) throws java.rmi.RemoteException;", webIface);
				CtMethod CPgetAllStructures = CtNewMethod.make("java.util.Map CPgetAllStructures(String intraServerPassword) throws java.rmi.RemoteException;", webIface);
				CtMethod CPisPlayerOnline = CtNewMethod.make("boolean CPisPlayerOnline(String intraServerPassword, long wurmID) throws java.rmi.RemoteException;", webIface);
				CtMethod CPgetSteamDBInfo = CtNewMethod.make("java.util.Map CPgetSteamDBInfo(String intraServerPassword) throws java.rmi.RemoteException;", webIface);

				if (guardTowersLoader && guardTowerInjSuc) {
					CtMethod CPgetAllGuardTowersImpl = CtNewMethod.make("public java.util.Map CPgetAllGuardTowers(String intraServerPassword) throws java.rmi.RemoteException { return com.imraginbro.wurm.mods.customrmi.CustomMethods.CPgetAllGuardTowers(intraServerPassword, com.wurmonline.server.zones.Zones.guardTowerList); }", webIfaceImpl);
					webIfaceImpl.addMethod(CPgetAllGuardTowersImpl);
					CtMethod CPgetAllGuardTowers = CtNewMethod.make("java.util.Map CPgetAllGuardTowers(String intraServerPassword) throws java.rmi.RemoteException;", webIface);
					webIface.addMethod(CPgetAllGuardTowers);
				}

				webIfaceImpl.addMethod(CPmessagePlayerImpl);
				webIfaceImpl.addMethod(CPchangePowerImpl);
				webIfaceImpl.addMethod(CPgiveItemImpl);
				webIfaceImpl.addMethod(CPkickPlayerImpl);
				webIfaceImpl.addMethod(CPgetAllStructuresImpl);
				webIfaceImpl.addMethod(CPisPlayerOnlineImpl);
				webIfaceImpl.addMethod(CPgetSteamDBInfoImpl);

				webIface.addMethod(CPmessagePlayer);
				webIface.addMethod(CPchangePower);
				webIface.addMethod(CPgiveItem);
				webIface.addMethod(CPkickPlayer);
				webIface.addMethod(CPgetAllStructures);
				webIface.addMethod(CPisPlayerOnline);
				webIface.addMethod(CPgetSteamDBInfo);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
