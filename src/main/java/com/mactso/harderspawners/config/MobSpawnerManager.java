package com.mactso.harderspawners.config;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class MobSpawnerManager {
	public static Hashtable<String, MobSpawnerBreakPercentageItem> mobBreakPercentageHashtable = new Hashtable<>();
	private static String defaultMobBreakPercentageString = "harderspawners:default:0.02";
	private static String defaultMobBreakPercentageKey = defaultMobBreakPercentageString;

	public static MobSpawnerBreakPercentageItem getMobSpawnerBreakPercentage(String key) {
		String iKey = key;
		if (mobBreakPercentageHashtable.isEmpty()) {
			mobBreakPercentageInit();
		}

		MobSpawnerBreakPercentageItem t = mobBreakPercentageHashtable.get(iKey);

		return t;
	}

	//returns a string of all mobs break percentages as one long string.
	public static String getMobSpawnerBreakPercentageAsString() {
		String returnString="";
		double breakPercentage;
		 
		for (String key:mobBreakPercentageHashtable.keySet()) {
			breakPercentage = mobBreakPercentageHashtable.get(key).breakPercentage;
			String tempString = key+","+breakPercentage+";";
			returnString += tempString;
		}
		return returnString;
	
	}

	public static void mobBreakPercentageInit() {
		
		List <String> dTL6464 = new ArrayList<>();
		
    	System.out.println ("HarderSpawners: Initialization Commencing.");

		int i = 0;
		String mobBreakPercentageLine6464 = "";
		// Forge Issue 6464 patch.
		StringTokenizer st6464 = new StringTokenizer(MyConfig.getDefaultMobBreakPercentageValues6464(), ";");
		while (st6464.hasMoreElements()) {
			mobBreakPercentageLine6464 = st6464.nextToken().trim();
			if (mobBreakPercentageLine6464.isEmpty()) continue;
			dTL6464.add(mobBreakPercentageLine6464);  
			i++;
		}

		MyConfig.setDefaultMobBreakPercentageValues(dTL6464.toArray(new String[i]));
		
		i = 0;
		mobBreakPercentageHashtable.clear();
		while (i < MyConfig.getDefaultMobBreakPercentageValues().length) {
			try {
				StringTokenizer st = new StringTokenizer(MyConfig.getDefaultMobBreakPercentageValues()[i], ",");
				String modAndMob = st.nextToken();
				String key = modAndMob;
				String breakPercentage = st.nextToken();
				double numericBreakPercentage = Double.parseDouble (breakPercentage.trim());
				if ((numericBreakPercentage < 0.0) || (numericBreakPercentage > 100.0)) {
					numericBreakPercentage = 0.02;
				}

				mobBreakPercentageHashtable.put(key, new MobSpawnerBreakPercentageItem(numericBreakPercentage));
				if (!modAndMob.contentEquals("harderspawners:default") &&
				    !ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(modAndMob))
				   )  {
					System.out.println("Harder Spawners Mob: " + modAndMob + " not in Forge Registry.  Mispelled?");
				}
			} catch (Exception e) {
				System.out.println("Harder Spawners :  Mob Config : " + MyConfig.getDefaultMobBreakPercentageValues()[i]);
			}
			i++;
		}
    	System.out.println ("HarderSpawners: Initialization complete.");

	}

	// keeps track of the spawner break percentage by mod:mob key.
	public static class MobSpawnerBreakPercentageItem {
		double breakPercentage;
		
		public MobSpawnerBreakPercentageItem(double breakPercentage) {
			this.breakPercentage = breakPercentage;
 
		}

		public double getSpawnerBreakPercentage() {
			return breakPercentage;
		}
		
	}

}


