package com.imraginbro.wurm.mods.customrmi;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.MethodInfo;

public class codePrinter {

	public static void main(String[] args) throws NotFoundException {
		ClassPool classPool = HookManager.getInstance().getClassPool();
		CtClass customMethods = classPool.get("com.imraginbro.wurm.mods.customrmi.CustomMethods");
		CtMethod[] methods = customMethods.getDeclaredMethods();
		for (int i = 0; i < methods.length; i++) {
			CtMethod m = methods[i];
			if (!m.getName().contains("CP")) {
				continue;
			}
			MethodInfo mInfo = m.getMethodInfo();
			int paramsCount = m.getParameterTypes().length;
			
			String str = "public " + m.getReturnType().getName() + " " + mInfo.getName() + "(";
			for (int z=0; z < paramsCount; z++) {
				final CtClass paramType = m.getParameterTypes()[z];
				str += paramType.getName() + " var_" + z;
				if (z < paramsCount-1) {
					str += ", ";
				}
			}
			str += ") throws java.rmi.RemoteException;";
			System.out.println(str);
		}
	}

}
