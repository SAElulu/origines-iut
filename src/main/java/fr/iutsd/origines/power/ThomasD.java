package fr.iutsd.origines.power;

import fr.iutsd.origines.Compat;
import fr.iutsd.origines.Config;
import fr.iutsd.origines.OriginPower;
import fr.iutsd.origines.PlayerState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;

/** Thomas_D, l'Archiviste : Sauvegarde / Chargement, bonus d'XP sur les minerais, mise à jour forcée. */
public final class ThomasD extends OriginPower {
	private final int saveCd = Config.ticks("thomas_d.save_cooldown_s", 30);
	private final int saveLife = Config.ticks("thomas_d.save_lifetime_s", 600);
	private final int loadCd = Config.ticks("thomas_d.load_cooldown_s", 90);
	private final int loadTicks = Config.ticks("thomas_d.load_channel_s", 5);
	private final int loadXpCost = (int) Config.d("thomas_d.load_xp_level_cost", 3);
	private final int oreXp = (int) Config.d("thomas_d.ore_bonus_xp", 3);
	private final int updateInterval = Config.ticks("thomas_d.update_interval_s", 1200);
	private final int updateTicks = Config.ticks("thomas_d.update_freeze_s", 5);

	@Override
	public void primary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		if (!checkCd(p, s, "save", "Sauvegarde", now)) return;
		s.setCd("save", now, saveCd);
		s.put("save_pos", new double[]{p.getX(), p.getY(), p.getZ()});
		s.put("save_dim", lvl.dimension());
		s.setL("save_time", now);
		Compat.particles(lvl, ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.5);
		Compat.sound(lvl, p.getX(), p.getY(), p.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
		say(p, "§aPartie sauvegardée !");
	}

	@Override
	public void secondary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		double[] pos = (double[]) s.get("save_pos");
		if (pos == null) {
			say(p, "§cAucune sauvegarde. Clic droit pour en faire une.");
			return;
		}
		if (now - s.getL("save_time", 0) > saveLife) {
			s.remove("save_pos");
			say(p, "§cTa sauvegarde a expiré.");
			return;
		}
		if (!lvl.dimension().equals(s.get("save_dim"))) {
			say(p, "§cLa sauvegarde est dans une autre dimension.");
			return;
		}
		if (p.experienceLevel < loadXpCost) {
			say(p, "§cIl faut " + loadXpCost + " niveaux d'XP pour charger.");
			return;
		}
		if (!checkCd(p, s, "load", "Chargement", now)) return;
		if (s.getL("loading", 0) != 0) return;
		s.setL("loading", now);
		s.put("load_from", new double[]{p.getX(), p.getY(), p.getZ()});
		s.put("load_hp", p.getHealth());
		say(p, "§eChargement... ne bouge pas !");
	}

	@Override
	public void tick(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		trackHurt(p, s, now);
		channel(p, s, lvl, now);
		forcedUpdate(p, s, lvl, now);
	}

	private void trackHurt(ServerPlayer p, PlayerState s, long now) {
		float hp = p.getHealth();
		if (s.get("last_hp") instanceof Float last && hp < last) s.setL("hurt_at", now);
		s.put("last_hp", hp);
	}

	private void channel(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		long start = s.getL("loading", 0);
		if (start == 0) return;
		double[] from = (double[]) s.get("load_from");
		Object hpObj = s.get("load_hp");
		float hp0 = hpObj instanceof Float f ? f : p.getHealth();
		double dx = p.getX() - from[0], dy = p.getY() - from[1], dz = p.getZ() - from[2];
		boolean moved = dx * dx + dy * dy + dz * dz > 0.5;
		boolean hurt = p.getHealth() < hp0 - 0.01f;
		if (moved || hurt) {
			s.setL("loading", 0);
			say(p, "§cChargement interrompu !");
			return;
		}
		if (now - start >= loadTicks) {
			double[] pos = (double[]) s.get("save_pos");
			s.setL("loading", 0);
			if (pos == null) return;
			Compat.particles(lvl, ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.3);
			Compat.teleport(p, pos[0], pos[1], pos[2]);
			p.giveExperienceLevels(-loadXpCost);
			s.setCd("load", now, loadCd);
			Compat.particles(lvl, ParticleTypes.PORTAL, pos[0], pos[1] + 1, pos[2], 40, 0.5, 0.3);
			Compat.sound(lvl, pos[0], pos[1], pos[2], SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.2f);
			say(p, "§aPartie chargée !");
		} else if (now % 10 == 0) {
			say(p, "§eChargement... " + (100 * (now - start) / loadTicks) + " %");
		}
	}

	/** Malus : mise à jour forcée, mais seulement quand tout est calme (pas de combat, pas de monstre proche). */
	private void forcedUpdate(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		long next = s.getL("update_next", 0);
		if (next == 0) {
			s.setL("update_next", now + updateInterval);
			return;
		}
		long end = s.getL("update_end", 0);
		if (now < end) {
			if (now % 10 == 0) {
				Compat.effect(p, MobEffects.SLOWNESS, 20, 6);
				Compat.effect(p, MobEffects.MINING_FATIGUE, 20, 5);
				long total = updateTicks;
				say(p, "§7Mise à jour en cours... " + (100 - 100 * (end - now) / total) + " %");
			}
			return;
		}
		if (now >= next && now - s.getL("hurt_at", -99999) > 200 && s.getL("loading", 0) == 0
				&& lvl.getEntitiesOfClass(Monster.class, p.getBoundingBox().inflate(16)).isEmpty()) {
			s.setL("update_end", now + updateTicks);
			s.setL("update_next", now + updateInterval);
		}
	}

	@Override
	public void onBlockBroken(ServerPlayer p, PlayerState s, BlockState st) {
		if (st.is(BlockTags.COAL_ORES) || st.is(BlockTags.IRON_ORES) || st.is(BlockTags.COPPER_ORES)
				|| st.is(BlockTags.GOLD_ORES) || st.is(BlockTags.REDSTONE_ORES) || st.is(BlockTags.LAPIS_ORES)
				|| st.is(BlockTags.DIAMOND_ORES) || st.is(BlockTags.EMERALD_ORES)) {
			p.giveExperiencePoints(oreXp);
		}
	}
}
