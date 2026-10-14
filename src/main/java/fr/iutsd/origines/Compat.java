package fr.iutsd.origines;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * TOUS les appels Minecraft "fragiles d'une version à l'autre" sont ici.
 * Si une mise à jour de Minecraft casse la compilation, c'est (presque) toujours dans ce fichier qu'on corrige.
 */
public final class Compat {
	private Compat() {}

	public static ServerLevel level(ServerPlayer p) {
		return (ServerLevel) p.level();
	}

	/** Dégâts "joueur" infligés à une créature. */
	public static void hurt(ServerLevel lvl, LivingEntity target, Player by, float amount) {
		target.hurtServer(lvl, lvl.damageSources().playerAttack(by), amount);
	}

	/** Recul : pousse la cible à l'opposé de 'from'. */
	public static void knock(LivingEntity target, Entity from, double strength) {
		target.knockback(strength, from.getX() - target.getX(), from.getZ() - target.getZ());
	}

	/** Ajoute une vitesse à une entité et force la synchronisation vers les clients. */
	public static void push(Entity e, double x, double y, double z) {
		e.setDeltaMovement(e.getDeltaMovement().add(x, y, z));
		sync(e);
	}

	public static void sync(Entity e) {
		e.hurtMarked = true;
	}

	public static void sound(ServerLevel l, double x, double y, double z, SoundEvent ev, float vol, float pitch) {
		l.playSound(null, x, y, z, ev, SoundSource.PLAYERS, vol, pitch);
	}

	public static void particles(ServerLevel l, ParticleOptions type, double x, double y, double z, int count, double spread, double speed) {
		l.sendParticles(type, x, y, z, count, spread, spread, spread, speed);
	}

	/** Message dans la barre d'action (au-dessus de la barre d'objets). */
	public static void actionbar(ServerPlayer p, Component c) {
		p.displayClientMessage(c, true);
	}

	public static void chat(ServerPlayer p, Component c) {
		p.sendSystemMessage(c);
	}

	/** Téléportation dans le même monde (dimension). */
	public static void teleport(ServerPlayer p, double x, double y, double z) {
		p.connection.teleport(x, y, z, p.getYRot(), p.getXRot());
	}

	/** Effet visible (icône + particules). */
	public static void effect(LivingEntity e, Holder<MobEffect> eff, int ticks, int amp) {
		e.addEffect(new MobEffectInstance(eff, ticks, amp));
	}

	/** Effet discret (passifs) : pas de particules. */
	public static void passive(LivingEntity e, Holder<MobEffect> eff, int ticks, int amp) {
		e.addEffect(new MobEffectInstance(eff, ticks, amp, true, false, true));
	}

	public static boolean isAdmin(CommandSourceStack s) {
		return s.hasPermission(2);
	}

	public static boolean isFall(DamageSource s) {
		return s.is(DamageTypeTags.IS_FALL);
	}

	public static void give(ServerPlayer p, ItemStack stack) {
		if (!p.getInventory().add(stack)) p.drop(stack, false);
	}

	/** Consomme 1 exemplaire de l'un des objets donnés. Renvoie false s'il n'y en a pas. */
	public static boolean consumeOne(ServerPlayer p, Item... items) {
		var inv = p.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack st = inv.getItem(i);
			if (st.isEmpty()) continue;
			for (Item it : items) {
				if (st.is(it)) {
					st.shrink(1);
					return true;
				}
			}
		}
		return false;
	}
}
