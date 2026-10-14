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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

/** Rémi, le Sergent : commande ses loups et golems, lance un dé, tenue réglementaire. Endurance limitée. */
public final class Remi extends OriginPower {
	private static final Item[][] SETS = {
		{Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS},
		{Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS},
		{Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS},
		{Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS},
		{Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS},
		{Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS},
	};

	private final int orderCd = Config.ticks("remi.order_cooldown_s", 30);
	private final double orderRadius = Config.d("remi.order_radius", 24);
	private final int orderBuff = Config.ticks("remi.order_strength_s", 15);
	private final int diceCd = Config.ticks("remi.dice_cooldown_s", 60);
	private final int sprintLimit = Config.ticks("remi.sprint_limit_s", 8);
	private final int breathTicks = Config.ticks("remi.catch_breath_s", 2);

	@Override
	public void primary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		if (!checkCd(p, s, "order", "Ordre", now)) return;
		var box = p.getBoundingBox().inflate(orderRadius);
		Monster target = null;
		double best = Double.MAX_VALUE;
		for (Monster m : lvl.getEntitiesOfClass(Monster.class, box, Monster::isAlive)) {
			double d = m.distanceToSqr(p);
			if (d < best) {
				best = d;
				target = m;
			}
		}
		if (target == null) {
			say(p, "§7Aucun ennemi en vue, soldat.");
			return;
		}
		List<Mob> troop = lvl.getEntitiesOfClass(Mob.class, box,
			x -> x.isAlive() && (Compat.typeId(x).equals("iron_golem") || (x instanceof TamableAnimal t && t.isTame() && t.isOwnedBy(p))));
		if (troop.isEmpty()) {
			say(p, "§7Pas de troupe : apprivoise un loup ou construis un golem de fer.");
			return;
		}
		s.setCd("order", now, orderCd);
		for (Mob m : troop) {
			m.setTarget(target);
			Compat.effect(m, MobEffects.STRENGTH, orderBuff, 0);
		}
		Compat.sound(lvl, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, 0.6f, 1.2f);
		say(p, "§2À l'attaque ! (" + troop.size() + " soldat(s))");
	}

	@Override
	public void secondary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		if (!checkCd(p, s, "dice", "Dé", now)) return;
		s.setCd("dice", now, diceCd);
		int roll = lvl.getRandom().nextInt(6) + 1;
		String msg;
		switch (roll) {
			case 1 -> msg = "1... raté ! Même le règlement ne t'aide pas.";
			case 2, 3 -> {
				Compat.effect(p, MobEffects.REGENERATION, 200, 0);
				msg = roll + " : un peu de repos réglementaire (régénération).";
			}
			case 4, 5 -> {
				Compat.effect(p, MobEffects.ABSORPTION, 600, 1);
				msg = roll + " : position défensive (absorption).";
			}
			default -> {
				Compat.effect(p, MobEffects.STRENGTH, 400, 0);
				Compat.effect(p, MobEffects.RESISTANCE, 400, 0);
				msg = "6 : COUP CRITIQUE ! Force et Résistance.";
			}
		}
		Compat.particles(lvl, ParticleTypes.CRIT, p.getX(), p.getY() + 2, p.getZ(), 15, 0.4, 0.1);
		lvl.getServer().getPlayerList().broadcastSystemMessage(Component.literal("§2" + p.getName().getString() + " lance le dé : " + msg), false);
	}

	@Override
	public void tick(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		// Endurance limitée : trop de sprint d'affilée = on reprend son souffle
		long run = s.getL("sprint_ticks", 0);
		if (p.isSprinting()) {
			run++;
			if (run > sprintLimit) {
				p.setSprinting(false);
				Compat.effect(p, MobEffects.SLOWNESS, breathTicks, 1);
				say(p, "§7Tu reprends ton souffle...");
				run = 0;
			}
		} else if (run > 0) {
			run = Math.max(0, run - 2);
		}
		s.setL("sprint_ticks", run);

		// Tenue réglementaire : armure complète du même matériau
		if (now % 20 == p.getId() % 20 && fullUniform(p)) {
			Compat.passive(p, MobEffects.RESISTANCE, 60, 0);
		}
	}

	private static boolean fullUniform(ServerPlayer p) {
		for (Item[] set : SETS) {
			if (p.getItemBySlot(EquipmentSlot.HEAD).is(set[0])
				&& p.getItemBySlot(EquipmentSlot.CHEST).is(set[1])
				&& p.getItemBySlot(EquipmentSlot.LEGS).is(set[2])
				&& p.getItemBySlot(EquipmentSlot.FEET).is(set[3])) {
				return true;
			}
		}
		return false;
	}
}
