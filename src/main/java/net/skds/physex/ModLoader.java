package net.skds.physex;

public enum ModLoader {
	FABRIC, NEOFORGE;

	public static final ModLoader CURRENT = getCurrent();

	private static ModLoader getCurrent() {
		if (isClassPresent("net.fabricmc.loader.api.FabricLoader")) {
			return FABRIC;
		}
		if (isClassPresent("net.neoforged.fml.loading.FMLLoader")) {
			return NEOFORGE;
		}
		throw new RuntimeException("Unable to detect mod loader");
	}

	private static boolean isClassPresent(String className) {
		try {
			Class.forName(className, false, ModLoader.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
