package fr.iutsd.origines.power;

import fr.iutsd.origines.Compat;
import fr.iutsd.origines.Config;
import fr.iutsd.origines.OriginPower;
import fr.iutsd.origines.PlayerState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

/**
 * Lucas, le Stratège Suprême.
 * Adaptation : il regarde chaque coup reçu, apprend, puis se protège du type de danger répété.
 * Contre : réponse immédiate au dernier danger subi.
 */
public final class Lucas extends OriginPower {
	private final int counterCd = Config.ticks("lucas.counter_cooldown_s", 45);
	private final int counterMemory = Config.ticks("lucas.counter_memory_s", 30);
	private final int jokeCd = Config.ticks("lucas.joke_cooldown_s", 30);
	private final double jokeRadius = Config.d("lucas.joke_radius", 12);
	private final double jokeDistance = Config.d("lucas.joke_distance", 9);
	private final int adaptHits = (int) Config.d("lucas.adapt_hits_needed", 3);
	private final int adaptWindow = Config.ticks("lucas.adapt_window_s", 90);
	private final int adaptLife = Config.ticks("lucas.adapt_duration_s", 90);
	private final double adaptReduction = Config.d("lucas.adapt_damage_reduction", 0.5);
	private final int surpriseMemory = Config.ticks("lucas.surprise_memory_s", 300);
	private final int egoWeakness = Config.ticks("lucas.ego_weakness_s", 30);

	private static String category(DamageSource src) {
		if (src == null) return "melee";
		if (src.is(DamageTypeTags.IS_FIRE)) return "feu";
		if (src.is(DamageTypeTags.IS_EXPLOSION)) return "explosion";
		if (src.is(DamageTypeTags.IS_PROJECTILE)) return "projectile";
		if (src.is(DamageTypeTags.IS_FALL)) return "chute";
		if (src.is(DamageTypeTags.IS_DROWNING)) return "noyade";
		if (src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC) || src.is(DamageTypes.WITHER)) return "magie";
		return "melee";
	}

	@Override
	public void tick(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		float hp = p.getHealth();
		if (s.get("last_hp") instanceof Float last && hp < last) {
			onHurt(p, s, last - hp, category(p.getLastDamageSource()), now);
		}
		s.put("last_hp", p.getHealth());
	}

	private void onHurt(ServerPlayer p, PlayerState s, float loss, String cat, long now) {
		s.setL("last_cat_at", now);
		s.put("last_cat", cat);

		// Adaptation en cours : on récupère une partie des dégâts
		if (now < s.getL("adapt_until:" + cat, 0)) {
			p.heal((float) (loss * adaptReduction));
			Compat.particles(Compat.level(p), ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY() + 1, p.getZ(), 6, 0.3, 0.1);
		} else {
			// Surprise : type de dégâts jamais vu depuis longtemps
			long seen = s.getL("seen:" + cat, -999999);
			if (now - seen > surpriseMemory) {
				Compat.effect(p, MobEffects.SLOWNESS, 40, 1);
				say(p, "§cSurprise ! (" + cat + ") Je n'avais pas prévu ça...");
			}
		}
		s.setL("seen:" + cat, now);

		// Apprentissage : N coups du même type dans la fenêtre = adaptation
		long first = s.getL("first:" + cat, 0);
		long count = s.getL("count:" + cat, 0);
		if (count == 0 || now - first > adaptWindow) {
			s.setL("first:" + cat, now);
			count = 0;
		}
		count++;
		if (count >= adaptHits) {
			s.setL("adapt_until:" + cat, now + adaptLife);
			count = 0;
			Compat.chat(p, Component.literal("§cCalculé. §7Lucas s'adapte à : " + cat + "."));
		}
		s.setL("count:" + cat, count);
	}

	@Override
	public void primary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		if (!checkCd(p, s, "counter", "Contre", now)) return;
		long at = s.getL("last_cat_at", -999999);
		s.setCd("counter", now, counterCd);
		if (now - at > counterMemory || !(s.get("last_cat") instanceof String cat)) {
			// Fausse alerte : l'ego en prend un coup
			Compat.effect(p, MobEffects.WEAKNESS, egoWeakness, 0);
			say(p, "§cFausse alerte... l'ego en prend un coup.");
			return;
		}
		switch (cat) {
			case "feu" -> Compat.effect(p, MobEffects.FIRE_RESISTANCE, 1200, 0);
			case "explosion", "projectile" -> Compat.effect(p, MobEffects.RESISTANCE, 160, 1);
			case "chute" -> Compat.effect(p, MobEffects.SLOW_FALLING, 400, 0);
			case "noyade" -> Compat.effect(p, MobEffects.WATER_BREATHING, 600, 0);
			case "magie" -> {
				p.removeAllEffects();
				Compat.effect(p, MobEffects.REGENERATION, 100, 1);
			}
			default -> {
				Compat.effect(p, MobEffects.RESISTANCE, 160, 1);
				Compat.effect(p, MobEffects.SPEED, 160, 0);
			}
		}
		s.setL("last_cat_at", -999999);
		Compat.particles(lvl, ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 12, 0.4, 0.1);
		Compat.sound(lvl, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, 0.7f, 1.3f);
		say(p, "§aPlan B, contre : " + cat + " !");
	}

	@Override
	public void secondary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		if (!checkCd(p, s, "joke", "Blague", now)) return;
		s.setCd("joke", now, jokeCd);
		Vec3 look = p.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(1, 0, 0);
		Vec3 spot = p.position().add(flat.normalize().scale(jokeDistance));
		Compat.sound(lvl, spot.x, spot.y, spot.z, SoundEvents.CREEPER_PRIMED, 1.5f, 1.0f);
		Compat.particles(lvl, ParticleTypes.SMOKE, spot.x, spot.y + 1, spot.z, 15, 0.3, 0.02);
		int n = 0;
		for (Mob m : lvl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(jokeRadius), x -> x instanceof Monster)) {
			m.setTarget(null);
			m.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.0);
			n++;
		}
		say(p, n == 0 ? "§7Personne pour rire de ta blague..." : "§eBlague ! " + n + " monstre(s) vont voir ce qui se passe.");
	}
}
