package fr.iutsd.origines.power;

import fr.iutsd.origines.Compat;
import fr.iutsd.origines.Config;
import fr.iutsd.origines.OriginPower;
import fr.iutsd.origines.PlayerState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Items;

/** Anouchka, la Vagabonde : fumée noire, vision des ombres, force dans le noir, exploration récompensée. */
public final class Anouchka extends OriginPower {
	private final int smokeCd = Config.ticks("anouchka.smoke_cooldown_s", 30);
	private final int smokeLife = Config.ticks("anouchka.smoke_duration_s", 8);
	private final double smokeRadius = Config.d("anouchka.smoke_radius", 6);
	private final int eyeCd = Config.ticks("anouchka.shadows_cooldown_s", 60);
	private final double eyeRadius = Config.d("anouchka.shadows_radius", 32);
	private final int eyeTicks = Config.ticks("anouchka.shadows_glow_s", 6);
	private final int darkLight = (int) Config.d("anouchka.dark_light_level_max", 7);
	private final int biomeXp = (int) Config.d("anouchka.new_biome_xp", 10);
	private final int cabinTicks = Config.ticks("anouchka.cabin_fever_after_s", 300);
	private final double cabinRadius = Config.d("anouchka.cabin_fever_radius", 40);

	@Override
	public void primary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		if (!checkCd(p, s, "smoke", "Fumée", now)) return;
		if (!Compat.consumeOne(p, Items.COAL, Items.CHARCOAL)) {
			say(p, "§cIl te faut 1 charbon pour la fumée noire.");
			return;
		}
		s.setCd("smoke", now, smokeCd);
		s.setL("smoke_until", now + smokeLife);
		Compat.effect(p, MobEffects.INVISIBILITY, smokeLife, 0);
		Compat.sound(lvl, p.getX(), p.getY(), p.getZ(), SoundEvents.FIRE_EXTINGUISH, 1.0f, 0.6f);
		say(p, "§8Fumée noire...");
	}

	@Override
	public void secondary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		if (!checkCd(p, s, "eye", "Ombres", now)) return;
		s.setCd("eye", now, eyeCd);
		int n = 0;
		for (Monster m : lvl.getEntitiesOfClass(Monster.class, p.getBoundingBox().inflate(eyeRadius))) {
			Compat.effect(m, MobEffects.GLOWING, eyeTicks, 0);
			n++;
		}
		Compat.sound(lvl, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDERMAN_AMBIENT, 1.0f, 0.7f);
		say(p, n == 0 ? "§7Les ombres sont silencieuses..." : "§5Les ombres te murmurent : " + n + " présence(s).");
	}

	@Override
	public void tick(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		// Nuage de fumée actif : les monstres autour perdent leur cible
		if (now < s.getL("smoke_until", 0) && now % 5 == 0) {
			Compat.particles(lvl, ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 12, smokeRadius / 3, 0.02);
			for (Mob m : lvl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(smokeRadius), x -> x instanceof Monster)) {
				m.setTarget(null);
				Compat.effect(m, MobEffects.SLOWNESS, 40, 1);
			}
		}
		if (now % 20 != p.getId() % 20) return;

		BlockPos pos = p.blockPosition();
		int light = lvl.getMaxLocalRawBrightness(pos);
		boolean sun = lvl.canSeeSky(pos) && light >= 13;

		// Enfant de la nuit
		if (light <= darkLight) {
			Compat.passive(p, MobEffects.NIGHT_VISION, 320, 0);
			Compat.passive(p, MobEffects.SPEED, 60, 0);
		}
		// Héliophobie
		if (sun && p.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
			Compat.passive(p, MobEffects.WEAKNESS, 60, 0);
			if (now % 200 < 20) say(p, "§eLe soleil te brûle les yeux... (un casque aide)");
		}
		// Carnet d'exploration : nouveau biome
		String biome = lvl.getBiome(pos).getRegisteredName();
		if (s.discovered.add("biome:" + biome)) {
			p.giveExperiencePoints(biomeXp);
			Compat.effect(p, MobEffects.SPEED, 600, 1);
			String shown = biome.contains(":") ? biome.substring(biome.indexOf(':') + 1) : biome;
			Compat.chat(p, net.minecraft.network.chat.Component.literal("§5Carnet de voyage : nouveau biome découvert (" + shown + ") !"));
		}
		// Besoin de liberté
		double[] a = (double[]) s.get("anchor");
		if (a == null || Math.hypot(p.getX() - a[0], p.getZ() - a[2]) > cabinRadius) {
			s.put("anchor", new double[]{p.getX(), p.getY(), p.getZ()});
			s.setL("anchor_time", now);
		} else if (now - s.getL("anchor_time", now) > cabinTicks) {
			Compat.passive(p, MobEffects.HUNGER, 100, 0);
			if (now % 600 < 20) say(p, "§7Tu étouffes ici... bouge !");
		}
	}
}
