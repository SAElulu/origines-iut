package fr.iutsd.origines;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** État d'un joueur : partie sauvegardée (origine, découvertes) + partie temporaire (recharges, compteurs). */
public final class PlayerState {
	/** Id de l'origine choisie, ou null. Sauvegardé. */
	public String origin;
	/** Progression sauvegardée : objets collectés (Timothe), biomes visités (Anouchka). */
	public final Set<String> discovered = new LinkedHashSet<>();

	private final Map<String, Long> cooldowns = new HashMap<>();
	private final Map<String, Object> tmp = new HashMap<>();

	public long remaining(String key, long now) {
		Long ready = cooldowns.get(key);
		return ready == null ? 0 : Math.max(0, ready - now);
	}

	public void setCd(String key, long now, long ticks) {
		cooldowns.put(key, now + ticks);
	}

	public long getL(String key, long def) {
		Object o = tmp.get(key);
		return o instanceof Long l ? l : def;
	}

	public void setL(String key, long value) {
		tmp.put(key, value);
	}

	public Object get(String key) {
		return tmp.get(key);
	}

	public void put(String key, Object value) {
		tmp.put(key, value);
	}

	public void remove(String key) {
		tmp.remove(key);
	}

	/** Remet à zéro tout ce qui est temporaire (changement d'origine). */
	public void resetTransient() {
		cooldowns.clear();
		tmp.clear();
	}
}
