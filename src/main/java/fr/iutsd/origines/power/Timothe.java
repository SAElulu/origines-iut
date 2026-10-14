package fr.iutsd.origines.power;

import fr.iutsd.origines.Compat;
import fr.iutsd.origines.Config;
import fr.iutsd.origines.Origin;
import fr.iutsd.origines.OriginPower;
import fr.iutsd.origines.PlayerState;
import fr.iutsd.origines.Talisman;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Timothe, le Collectionneur : Dunk, collection d'objets (progression), animaux apprivoisés. */
public final class Timothe extends OriginPower {
	private final int dunkCd = Config.ticks("timothe.dunk_cooldown_s", 20);
	private final double dunkLift = Config.d("timothe.dunk_lift", 0.95);
	private final double dunkForward = Config.d("timothe.dunk_forward", 0.6);
	private final double dunkRadius = Config.d("timothe.dunk_radius", 3.5);
	private final float dunkBase = (float) Config.d("timothe.dunk_base_damage", 3);
	private final float dunkMaxExtra = (float) Config.d("timothe.dunk_max_extra_damage", 5);
	private final int newItemXp = (int) Config.d("timothe.new_item_xp", 2);
	private final int loserTicks = Config.ticks("timothe.death_weakness_s", 180);

	@Override
	public void primary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		if (!checkCd(p, s, "dunk", "Dunk", now)) return;
		if (s.getL("dunk_start", 0) != 0) return;
		s.setCd("dunk", now, dunkCd);
		s.setL("dunk_start", now);
		s.put("dunk_maxy", p.getY());
		Vec3 look = p.getLookAngle();
		Compat.push(p, look.x * dunkForward, dunkLift, look.z * dunkForward);
		Compat.sound(lvl, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f);
		say(p, "§6DUNK !");
	}

	@Override
	public boolean allowDamage(ServerPlayer p, PlayerState s, DamageSource src, float amount, long now) {
		// Pas de dégâts de chute pendant un dunk
		return !(Compat.isFall(src) && (s.getL("dunk_start", 0) != 0 || now - s.getL("dunk_end", -1000) < 40));
	}

	@Override
	public void tick(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		dunk(p, s, lvl, now);
		if (now % 10 == p.getId() % 10) collect(p, s);
		if (now % 40 == 0) petAura(p, lvl);
	}

	private void dunk(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		long start = s.getL("dunk_start", 0);
		if (start == 0) return;
		Object my = s.get("dunk_maxy");
		double maxY = Math.max(my instanceof Double d ? d : p.getY(), p.getY());
		s.put("dunk_maxy", maxY);
		if (now - start > 120) {
			s.setL("dunk_start", 0);
			return;
		}
		if (now - start > 6 && p.onGround()) {
			double height = maxY - p.getY();
			float dmg = dunkBase + (float) Math.min(dunkMaxExtra, height * 0.5);
			int hits = 0;
			for (LivingEntity t : lvl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(dunkRadius),
					x -> x != p && x.isAlive() && x instanceof Monster)) {
				Compat.hurt(lvl, t, p, dmg);
				Compat.knock(t, p, 1.2);
				hits++;
			}
			Compat.particles(lvl, ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.2, p.getZ(), 1, 0.0, 0.0);
			Compat.particles(lvl, ParticleTypes.POOF, p.getX(), p.getY() + 0.2, p.getZ(), 25, 0.8, 0.05);
			Compat.sound(lvl, p.getX(), p.getY(), p.getZ(), SoundEvents.ANVIL_LAND, 0.8f, 0.7f);
			s.setL("dunk_start", 0);
			s.setL("dunk_end", now);
			if (hits == 0) {
				Compat.effect(p, MobEffects.SLOWNESS, 80, 0);
				say(p, "§cAir-ball ! Tu boudes un peu...");
			} else {
				say(p, "§6Dunk monumental ! (" + hits + " touché(s))");
			}
		}
	}

	/** Collection : chaque nouvel objet (jamais possédé) donne XP + Hype, et des paliers donnent des récompenses. */
	private void collect(ServerPlayer p, PlayerState s) {
		var inv = p.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack st = inv.getItem(i);
			if (st.isEmpty() || Talisman.is(st, Origin.TIMOTHE)) continue;
			String id = BuiltInRegistries.ITEM.getKey(st.getItem()).toString();
			if (s.discovered.add("item:" + id)) onNewItem(p, s);
		}
	}

	private void onNewItem(ServerPlayer p, PlayerState s) {
		int count = (int) s.discovered.stream().filter(x -> x.startsWith("item:")).count();
		p.giveExperiencePoints(newItemXp);
		Compat.effect(p, MobEffects.SPEED, 200, 0);
		say(p, "§6Collection : " + count + " objets !");
		switch (count) {
			case 25 -> reward(p, new ItemStack(Items.GOLDEN_APPLE, 2), "25 objets");
			case 50 -> reward(p, new ItemStack(Items.DIAMOND, 3), "50 objets");
			case 100 -> reward(p, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1), "100 objets");
			case 150 -> reward(p, new ItemStack(Items.TOTEM_OF_UNDYING, 1), "150 objets");
			default -> { }
		}
	}

	private void reward(ServerPlayer p, ItemStack st, String why) {
		Compat.give(p, st);
		Compat.chat(p, net.minecraft.network.chat.Component.literal("§6Palier de collection (" + why + ") : récompense !"));
	}

	/** Animaux apprivoisés proches : régénération. */
	private void petAura(ServerPlayer p, ServerLevel lvl) {
		for (TamableAnimal pet : lvl.getEntitiesOfClass(TamableAnimal.class, p.getBoundingBox().inflate(16),
				t -> t.isTame() && t.isOwnedBy(p))) {
			Compat.passive(pet, MobEffects.REGENERATION, 100, 0);
		}
	}

	@Override
	public void onRespawn(ServerPlayer p, PlayerState s, long now) {
		Compat.effect(p, MobEffects.WEAKNESS, loserTicks, 0);
		say(p, "§cMauvais perdant : tu es affaibli un moment...");
	}
}
