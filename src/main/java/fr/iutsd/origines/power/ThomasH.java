package fr.iutsd.origines.power;

import fr.iutsd.origines.Compat;
import fr.iutsd.origines.Config;
import fr.iutsd.origines.Origin;
import fr.iutsd.origines.OriginPower;
import fr.iutsd.origines.PlayerState;
import fr.iutsd.origines.Talisman;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Thomas_H, le Pongiste : Smash (corps à corps) et Renvoi (retour de balle sur les projectiles). */
public final class ThomasH extends OriginPower {
	private final int smashCd = Config.ticks("thomas_h.smash_cooldown_s", 10);
	private final float smashDamage = (float) Config.d("thomas_h.smash_damage", 6);
	private final double smashRange = Config.d("thomas_h.smash_range", 3.5);
	private final int smashFood = (int) Config.d("thomas_h.smash_food_cost", 2);
	private final int returnCd = Config.ticks("thomas_h.return_cooldown_s", 12);
	private final int returnWindow = Config.ticks("thomas_h.return_window_s", 1.5);
	private final double returnRadius = Config.d("thomas_h.return_radius", 4);

	private static boolean heavyChest(ServerPlayer p) {
		ItemStack c = p.getItemBySlot(EquipmentSlot.CHEST);
		return c.is(Items.IRON_CHESTPLATE) || c.is(Items.DIAMOND_CHESTPLATE) || c.is(Items.NETHERITE_CHESTPLATE);
	}

	@Override
	public void primary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		if (heavyChest(p)) {
			say(p, "§cLa raquette demande de la souplesse : retire ton plastron lourd !");
			return;
		}
		if (!checkCd(p, s, "smash", "Smash", now)) return;
		if (!spendFood(p, smashFood)) return;
		s.setCd("smash", now, smashCd);

		Vec3 look = p.getLookAngle();
		int hits = 0;
		for (LivingEntity t : lvl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(smashRange),
				x -> x != p && x.isAlive() && !(x instanceof Player))) {
			Vec3 to = t.position().add(0, t.getBbHeight() / 2, 0).subtract(p.getEyePosition());
			if (to.length() > smashRange + 0.5 || to.normalize().dot(look) < 0.4) continue;
			Compat.hurt(lvl, t, p, smashDamage);
			Compat.knock(t, p, 1.4);
			hits++;
		}
		Vec3 front = p.getEyePosition().add(look.scale(1.5));
		Compat.particles(lvl, ParticleTypes.SWEEP_ATTACK, front.x, front.y, front.z, 1, 0.0, 0.0);
		Compat.sound(lvl, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
		if (hits == 0) say(p, "§7Smash dans le vide...");
	}

	@Override
	public void secondary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		if (heavyChest(p)) {
			say(p, "§cLa raquette demande de la souplesse : retire ton plastron lourd !");
			return;
		}
		if (!checkCd(p, s, "return", "Renvoi", now)) return;
		s.setCd("return", now, returnCd);
		s.setL("return_until", now + returnWindow);
		say(p, "§bRaquette en garde : renvoie tout !");
		Compat.sound(lvl, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8f, 1.5f);
	}

	@Override
	public void tick(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		// Renvoi : fenêtre active
		if (now < s.getL("return_until", 0)) {
			Vec3 look = p.getLookAngle();
			for (Projectile pr : lvl.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(returnRadius),
					x -> x.getOwner() != p && x.getDeltaMovement().lengthSqr() > 0.01)) {
				double speed = Math.max(0.9, pr.getDeltaMovement().length() * 1.3);
				pr.setDeltaMovement(look.scale(speed));
				pr.setOwner(p);
				Compat.sync(pr);
				Compat.particles(lvl, ParticleTypes.CRIT, pr.getX(), pr.getY(), pr.getZ(), 8, 0.2, 0.1);
				Compat.sound(lvl, pr.getX(), pr.getY(), pr.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 1.3f);
			}
		}
		// Jeu de jambes (passif)
		if (now % 20 == 0 && Talisman.holding(p, Origin.THOMAS_H) && !heavyChest(p)) {
			Compat.passive(p, MobEffects.SPEED, 60, 0);
		}
	}
}
