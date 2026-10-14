package fr.iutsd.origines;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Sauvegarde par monde : origines_iut_data.json à la racine du monde (survit aux déconnexions et redémarrages). */
public final class Store {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<UUID, PlayerState> STATES = new HashMap<>();

	private Store() {}

	private static Path file(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("origines_iut_data.json");
	}

	public static PlayerState get(UUID id) {
		return STATES.computeIfAbsent(id, k -> new PlayerState());
	}

	public static void load(MinecraftServer server) {
		STATES.clear();
		Path f = file(server);
		if (!Files.exists(f)) return;
		try (Reader r = Files.newBufferedReader(f)) {
			JsonObject root = GSON.fromJson(r, JsonObject.class);
			if (root == null) return;
			for (var e : root.entrySet()) {
				JsonObject o = e.getValue().getAsJsonObject();
				PlayerState st = new PlayerState();
				if (o.has("origin") && !o.get("origin").isJsonNull()) st.origin = o.get("origin").getAsString();
				if (o.has("discovered")) {
					for (JsonElement c : o.getAsJsonArray("discovered")) st.discovered.add(c.getAsString());
				}
				STATES.put(UUID.fromString(e.getKey()), st);
			}
		} catch (Exception ex) {
			OriginsMod.LOGGER.error("Impossible de lire les données des joueurs", ex);
		}
	}

	public static void save(MinecraftServer server) {
		try {
			JsonObject root = new JsonObject();
			for (var e : STATES.entrySet()) {
				PlayerState st = e.getValue();
				if (st.origin == null && st.discovered.isEmpty()) continue;
				JsonObject o = new JsonObject();
				if (st.origin != null) o.addProperty("origin", st.origin);
				JsonArray arr = new JsonArray();
				st.discovered.forEach(arr::add);
				o.add("discovered", arr);
				root.add(e.getKey().toString(), o);
			}
			Files.writeString(file(server), GSON.toJson(root));
		} catch (Exception ex) {
			OriginsMod.LOGGER.error("Impossible de sauvegarder les données des joueurs", ex);
		}
	}
}
