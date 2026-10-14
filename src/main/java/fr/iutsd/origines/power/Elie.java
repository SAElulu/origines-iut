package fr.iutsd.origines.power;

import fr.iutsd.origines.Compat;
import fr.iutsd.origines.Config;
import fr.iutsd.origines.OriginPower;
import fr.iutsd.origines.PlayerState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;

/** Elie, le Ressort Humain : bonds géants, grimace, chutes amorties. Pas discret du tout. */
public final class Elie extends OriginPower {
	private final int boundCd = Config.ticks("elie.bound_cooldown_s", 6);
	private final int boundJumpAmp = (int) Config.d("elie.bound_jump_boost_level", 5);
	private final int boundWindow = Config.ticks("elie.bound_jump_window_s", 2);
	private final int boundFall = Config.ticks("elie.bound_slow_fall_s", 8);
	private final int crashUses = (int) Config.d("elie.crash_after_uses", 3);
	private final int crashWindow = Config.ticks("elie.crash_window_s", 60);
	private final int crashTicks = Config.ticks("elie.crash_duration_s", 10);
	private final int grimaceCd = Config.ticks("elie.grimace_cooldown_s", 20);
	private final double grimaceRadius = Config.d("elie.grimace_radius", 5);
	private final double safeFallDamage = Config.d("elie.safe_fall_damage", 5);
	private final double noiseRadius = Config.d("elie.noise_radius", 20);

	@Override
	public void primary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		if (!checkCd(p, s, "bound", "Air Jordan", now)) return;
		s.setCd("bound", now, boundCd);
		Compat.effect(p, MobEffects.JUMP_BOOST, boundWindow, boundJumpAmp);
		Compat.effect(p, MobEffects.SLOW_FALLING, boundFall, 0);
		Compat.particles(lvl, ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 15, 0.3, 0.05);
		Compat.sound(lvl, p.getX(), p.getY(), p.getZ(), SoundEvents.RABBIT_JUMP, 1.0f, 0.8f);
		say(p, "§dSAUTE !");

		// Inarrêtable : trop de bonds d'affilée = fatigue
		long first = s.getL("bound_first", 0);
		long uses = s.getL("bound_uses", 0);
		if (uses == 0 || now - first > crashWindow) {
			s.setL("bound_first", now);
			uses = 0;
		}
		uses++;
		s.setL("bound_uses", uses);
		if (uses >= crashUses) {
			s.setL("bound_uses", 0);
			s.setL("crash_at", now + 40);
		}
	}

	@Override
	public void secondary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		if (!checkCd(p, s, "grimace", "Grimace", now)) return;
		s.setCd("grimace", now, grimaceCd);
		int n = 0;
		for (Mob m : lvl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(grimaceRadius), x -> x instanceof Monster)) {
			m.setTarget(null);
			Compat.effect(m, MobEffects.SLOWNESS, 60, 6);
			n++;
		}
		Compat.particles(lvl, ParticleTypes.ANGRY_VILLAGER, p.getX(), p.getY() + 2, p.getZ(), 10, 0.4, 0.0);
		Compat.sound(lvl, p.getX(), p.getY(), p.getZ(), SoundEvents.VILLAGER_NO, 1.0f, 1.4f);
		say(p, n == 0 ? "§7Grimace pour personne..." : "§dGrimace terrifiante ! (" + n + " figé(s))");
	}

	@Override
	public boolean allowDamage(ServerPlayer p, PlayerState s, DamageSource src, float amount, long now) {
		// Ressort : petites et moyennes chutes absorbées
		return !(Compat.isFall(src) && amount <= safeFallDamage);
	}

	@Override
	public void tick(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		long crash = s.getL("crash_at", 0);
		if (crash != 0 && now >= crash) {
			s.setL("crash_at", 0);
			Compat.effect(p, MobEffects.SLOWNESS, crashTicks, 1);
			Compat.effect(p, MobEffects.WEAKNESS, crashTicks, 0);
			say(p, "§cInarrêtable... jusqu'au crash. Souffle !");
		}
		// Aucune discrétion : les monstres le repèrent quand il bouge
		if (now % 20 == p.getId() % 20 && !p.isShiftKeyDown()
				&& p.getDeltaMovement().horizontalDistanceSqr() > 0.005) {
			for (Mob m : lvl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(noiseRadius), x -> x instanceof Monster)) {
				if (m.getTarget() == null && m.hasLineOfSight(p)) m.setTarget(p);
			}
		}
	}
}
