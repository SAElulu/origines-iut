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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
		double dx = target.getX() - from.getX();
		double dz = target.getZ() - from.getZ();
		double len = Math.sqrt(dx * dx + dz * dz);
		if (len < 1.0E-4) {
			dx = 0;
			dz = 1;
			len = 1;
		}
		push(target, dx / len * strength * 0.6, 0.3, dz / len * strength * 0.6);
	}

	/** Ajoute une vitesse à une entité et force la synchronisation vers les clients. */
	public static void push(Entity e, double x, double y, double z) {
		e.setDeltaMovement(e.getDeltaMovement().add(x, y, z));
		sync(e);
	}

	public static void sync(Entity e) {
		setFlag(e, "hurtMarked");
		setFlag(e, "hasImpulse");
		if (e instanceof ServerPlayer sp) {
			// Les joueurs gèrent leurs déplacements eux-mêmes : il faut leur envoyer la vitesse.
			try {
				Class<?> c = Class.forName("net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket");
				Object pkt;
				try {
					pkt = c.getConstructor(Entity.class).newInstance(e);
				} catch (NoSuchMethodException ex) {
					pkt = c.getConstructor(int.class, Vec3.class).newInstance(e.getId(), e.getDeltaMovement());
				}
				sp.connection.send((Packet<?>) pkt);
			} catch (Throwable t) {
				OriginsMod.LOGGER.warn("Synchronisation de vitesse impossible : {}", t.toString());
			}
		}
	}

	private static void setFlag(Object o, String field) {
		try {
			Field f = Entity.class.getDeclaredField(field);
			f.setAccessible(true);
			f.setBoolean(o, true);
		} catch (Throwable ignored) {
			// champ absent dans cette version : sans importance
		}
	}

	/** Appelle la première méthode publique qui porte l'un de ces noms et accepte ces arguments. */
	private static boolean callAny(Object target, String[] names, Object... args) {
		for (Method m : target.getClass().getMethods()) {
			if (m.getParameterCount() != args.length) continue;
			boolean nameOk = false;
			for (String n : names) {
				if (m.getName().equals(n)) nameOk = true;
			}
			if (!nameOk) continue;
			Class<?>[] pt = m.getParameterTypes();
			boolean ok = true;
			for (int i = 0; i < pt.length; i++) {
				Class<?> t = pt[i] == boolean.class ? Boolean.class : pt[i];
				if (!t.isInstance(args[i])) {
					ok = false;
					break;
				}
			}
			if (!ok) continue;
			try {
				m.setAccessible(true);
				m.invoke(target, args);
				return true;
			} catch (Throwable ignored) {
				// on essaie la suivante
			}
		}
		return false;
	}

	public static void sound(ServerLevel l, double x, double y, double z, SoundEvent ev, float vol, float pitch) {
		l.playSound(null, x, y, z, ev, SoundSource.PLAYERS, vol, pitch);
	}

	public static void particles(ServerLevel l, ParticleOptions type, double x, double y, double z, int count, double spread, double speed) {
		l.sendParticles(type, x, y, z, count, spread, spread, spread, speed);
	}

	/** Message au-dessus de la barre d'objets. Renvoie false si cette version de Minecraft ne le permet pas. */
	public static boolean overlay(ServerPlayer p, Component c) {
		return callAny(p, new String[]{"sendOverlayMessage"}, c)
			|| callAny(p, new String[]{"displayClientMessage", "sendSystemMessage"}, c, Boolean.TRUE);
	}

	/** Barre d'action, ou à défaut le chat. */
	public static void actionbar(ServerPlayer p, Component c) {
		if (!overlay(p, c)) chat(p, c);
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

	/** Opérateur du serveur ? (les permissions ont changé de forme dans Minecraft 26.x, donc recherche adaptative) */
	public static boolean isAdmin(CommandSourceStack s) {
		try {
			Object perms = s.getClass().getMethod("permissions").invoke(s);
			Class<?> pc = null;
			for (String cn : new String[]{"net.minecraft.server.permissions.Permissions", "net.minecraft.commands.Permissions"}) {
				try {
					pc = Class.forName(cn);
					break;
				} catch (ClassNotFoundException ignored) {
					// suivant
				}
			}
			if (perms != null && pc != null) {
				for (String fn : new String[]{"COMMANDS_GAMEMASTER", "COMMANDS_MODERATOR", "COMMANDS_ADMIN"}) {
					Object level;
					try {
						level = pc.getField(fn).get(null);
					} catch (NoSuchFieldException ex) {
						continue;
					}
					for (Method m : perms.getClass().getMethods()) {
						if (m.getName().equals("hasPermission") && m.getParameterCount() == 1
								&& m.getParameterTypes()[0].isInstance(level)) {
							m.setAccessible(true);
							return (boolean) m.invoke(perms, level);
						}
					}
				}
			}
		} catch (Throwable t) {
			OriginsMod.LOGGER.warn("Vérification des permissions impossible : {}", t.toString());
		}
		return s.getEntity() == null; // à défaut : console seulement
	}

	/** Identifiant court d'une créature : "villager", "iron_golem"... */
	public static String typeId(Entity e) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
	}

	/** Identifiant court d'un bloc : "diamond_ore", "stone"... */
	public static String blockPath(BlockState st) {
		return BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath();
	}

	public static boolean isFall(DamageSource s) {
		return s.is(DamageTypeTags.IS_FALL);
	}

	public static void give(ServerPlayer p, ItemStack stack) {
		if (!p.getInventory().add(stack) && !stack.isEmpty()) {
			level(p).addFreshEntity(new ItemEntity(level(p), p.getX(), p.getY() + 0.5, p.getZ(), stack));
		}
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
