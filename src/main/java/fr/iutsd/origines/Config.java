package fr.iutsd.origines;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * Configuration d'équilibrage : config/origines_iut.json
 * Chaque valeur est créée avec sa valeur par défaut au premier lancement,
 * puis modifiable à la main (redémarrer le serveur pour appliquer).
 * Convention : les clés finissant par _s sont en secondes.
 */
public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("origines_iut.json");
	private static final Map<String, Double> VALUES = new TreeMap<>();
	private static boolean loaded = false;

	private Config() {}

	private static void load() {
		if (loaded) return;
		loaded = true;
		if (!Files.exists(FILE)) return;
		try (Reader r = Files.newBufferedReader(FILE)) {
			JsonObject o = GSON.fromJson(r, JsonObject.class);
			if (o != null) {
				for (var e : o.entrySet()) {
					if (e.getValue().isJsonPrimitive()) VALUES.put(e.getKey(), e.getValue().getAsDouble());
				}
			}
		} catch (Exception ex) {
			OriginsMod.LOGGER.error("Config illisible, valeurs par défaut utilisées", ex);
		}
	}

	public static double d(String key, double def) {
		load();
		return VALUES.computeIfAbsent(key, k -> def);
	}

	/** Durée en secondes dans la config, convertie en ticks. */
	public static int ticks(String key, double seconds) {
		return (int) Math.round(d(key, seconds) * 20.0);
	}

	public static boolean flag(String key, boolean def) {
		return d(key, def ? 1 : 0) >= 0.5;
	}

	public static void flush() {
		try {
			Files.createDirectories(FILE.getParent());
			JsonObject o = new JsonObject();
			VALUES.forEach((k, v) -> o.addProperty(k, v));
			Files.writeString(FILE, GSON.toJson(o));
		} catch (IOException ex) {
			OriginsMod.LOGGER.error("Impossible d'écrire la config", ex);
		}
	}
}
