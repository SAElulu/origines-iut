package fr.iutsd.origines.power;

import fr.iutsd.origines.Compat;
import fr.iutsd.origines.Config;
import fr.iutsd.origines.OriginPower;
import fr.iutsd.origines.PlayerState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

/** Yago, l'Ambassadeur du Carnaval : Batucada (fête qui fait des bébés animaux), confettis, prix des villageois. */
public final class Yago extends OriginPower {
	private final int batucadaCd = Config.ticks("yago.batucada_cooldown_s", 90);
	private final int partyTicks = Config.ticks("yago.batucada_duration_s", 10);
	private final double partyRadius = Config.d("yago.batucada_radius", 12);
	private final int noisyTicks = Config.ticks("yago.noisy_after_party_s", 30);
	private final double noisyRadius = Config.d("yago.noisy_radius", 24);
	private final int confettiCd = Config.ticks("yago.confetti_cooldown_s", 25);
	private final double confettiRadius = Config.d("yago.confetti_radius", 5);
	private final int hangoverNausea = Config.ticks("yago.hangover_nausea_s", 5);
	private final int hangoverSlow = Config.ticks("yago.hangover_slowness_s", 20);

	@Override
	public void primary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		if (!checkCd(p, s, "batucada", "Batucada", now)) return;
		s.setCd("batucada", now, batucadaCd);
		var box = p.getBoundingBox().inflate(partyRadius);
		for (Player pl : lvl.getEntitiesOfClass(Player.class, box)) {
			Compat.effect(pl, MobEffects.SPEED, partyTicks, 0);
			Compat.effect(pl, MobEffects.HASTE, partyTicks, 0);
		}
		int babies = 0;
		for (Animal a : lvl.getEntitiesOfClass(Animal.class, box, x -> x.getAge() == 0)) {
			a.setInLove(p);
			babies++;
		}
		Compat.particles(lvl, ParticleTypes.NOTE, p.getX(), p.getY() + 2, p.getZ(), 30, partyRadius / 3, 1.0);
		Compat.sound(lvl, p.getX(), p.getY(), p.getZ(), SoundEvents.BELL_BLOCK, 1.5f, 1.0f);
		s.setL("party_until", now + partyTicks);
		s.setL("hangover_at", now + partyTicks);
		s.setL("noisy_until", now + noisyTicks);
		say(p, "§eBATUCADA !" + (babies > 0 ? " " + babies + " animal(aux) tombent amoureux..." : ""));
	}

	@Override
	public void secondary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		if (!checkCd(p, s, "confetti", "Confettis", now)) return;
		s.setCd("confetti", now, confettiCd);
		int n = 0;
		for (Mob m : lvl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(confettiRadius), x -> x instanceof Monster)) {
			m.setTarget(null);
			Compat.effect(m, MobEffects.SLOWNESS, 60, 5);
			n++;
		}
		Compat.particles(lvl, ParticleTypes.FIREWORK, p.getX(), p.getY() + 1, p.getZ(), 40, confettiRadius / 3, 0.1);
		Compat.sound(lvl, p.getX(), p.getY(), p.getZ(), SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f);
		say(p, n == 0 ? "§7Confettis pour personne..." : "§dConfettis ! " + n + " monstre(s) dansent.");
	}

	@Override
	public void tick(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		// Impossible de se taire : après la fête, les monstres viennent râler
		if (now < s.getL("noisy_until", 0) && now % 20 == 0) {
			for (Mob m : lvl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(noisyRadius), x -> x instanceof Monster)) {
				if (m.getTarget() == null) m.setTarget(p);
			}
		}
		// Gueule de bois à la fin de la fête
		long h = s.getL("hangover_at", 0);
		if (h != 0 && now >= h) {
			s.setL("hangover_at", 0);
			Compat.effect(p, MobEffects.NAUSEA, hangoverNausea, 0);
			Compat.effect(p, MobEffects.SLOWNESS, hangoverSlow, 0);
			say(p, "§7Gueule de bois...");
		}
		// Ambassadeur : un villageois proche = prix du Héros du village
		if (now % 20 == p.getId() % 20) {
			boolean near = !lvl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(10),
				x -> Compat.typeId(x).equals("villager") || Compat.typeId(x).equals("wandering_trader")).isEmpty();
			if (near) Compat.passive(p, MobEffects.HERO_OF_THE_VILLAGE, 100, 0);
		}
	}
}
