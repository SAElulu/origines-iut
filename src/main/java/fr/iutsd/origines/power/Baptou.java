package fr.iutsd.origines.power;

import fr.iutsd.origines.Compat;
import fr.iutsd.origines.Config;
import fr.iutsd.origines.OriginPower;
import fr.iutsd.origines.PlayerState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Baptou, le Commentateur du Rift : Flash, Lecture de la méta, annonceur de kills, hotfix des outils. */
public final class Baptou extends OriginPower {
	private static final List<String> DEFAULT_PUNCHLINES = List.of(
		"Ça c'est du outplay !",
		"Quelqu'un a désactivé le cerveau de la créature.",
		"Gap de niveau, pas de gap de skill... ah si, il y a les deux.",
		"Pas de panique, c'est juste un skill issue (de l'autre).",
		"GG, easy. Prochaine partie.",
		"Le monstre a ragequit.",
		"Mid ou feed, il a choisi.",
		"On appelle ça un diff.");

	private final int flashCd = Config.ticks("baptou.flash_cooldown_s", 40);
	private final double flashRange = Config.d("baptou.flash_range", 7);
	private final int flashFood = (int) Config.d("baptou.flash_food_cost", 3);
	private final int readCd = Config.ticks("baptou.read_cooldown_s", 20);
	private final double readRange = Config.d("baptou.read_range", 24);
	private final int readFood = (int) Config.d("baptou.read_food_cost", 2);
	private final int readTicks = Config.ticks("baptou.read_duration_s", 15);
	private final int pingTicks = Config.ticks("baptou.ping_glow_s", 3);
	private final int streakWindow = Config.ticks("baptou.streak_window_s", 10);
	private final int tiltTicks = Config.ticks("baptou.tilt_flash_lock_s", 120);
	private final int hotfixEvery = Config.ticks("baptou.hotfix_every_s", 30);
	private final List<String> punchlines = loadPunchlines();

	/** Les punchlines sont modifiables : config/origines_iut_punchlines.txt (une par ligne). */
	private static List<String> loadPunchlines() {
		Path f = FabricLoader.getInstance().getConfigDir().resolve("origines_iut_punchlines.txt");
		try {
			if (!Files.exists(f)) {
				Files.createDirectories(f.getParent());
				Files.write(f, DEFAULT_PUNCHLINES);
				return DEFAULT_PUNCHLINES;
			}
			List<String> lines = Files.readAllLines(f).stream().map(String::trim).filter(x -> !x.isEmpty()).toList();
			return lines.isEmpty() ? DEFAULT_PUNCHLINES : lines;
		} catch (Exception e) {
			return DEFAULT_PUNCHLINES;
		}
	}

	private void ping(ServerPlayer p) {
		Compat.effect(p, MobEffects.GLOWING, pingTicks, 0);
	}

	@Override
	public void primary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		if (!checkCd(p, s, "flash", "Flash", now)) return;
		Vec3 from = p.getEyePosition();
		Vec3 look = p.getLookAngle();
		Vec3 to = from.add(look.scale(flashRange));
		var hit = lvl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		Vec3 eyeDest = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation().subtract(look.scale(0.6));
		Vec3 feet = eyeDest.subtract(0, p.getEyeHeight(), 0);

		var shift = feet.subtract(p.position());
		if (!lvl.noCollision(p, p.getBoundingBox().move(shift))) {
			feet = feet.add(0, 1, 0);
			if (!lvl.noCollision(p, p.getBoundingBox().move(feet.subtract(p.position())))) {
				say(p, "§cPas la place pour flash !");
				return;
			}
		}
		if (!spendFood(p, flashFood)) return;
		s.setCd("flash", now, flashCd);
		Compat.particles(lvl, ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.3);
		Compat.teleport(p, feet.x, feet.y, feet.z);
		Compat.particles(lvl, ParticleTypes.PORTAL, feet.x, feet.y + 1, feet.z, 30, 0.4, 0.3);
		Compat.sound(lvl, feet.x, feet.y, feet.z, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.5f);
		ping(p);
	}

	@Override
	public void secondary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		if (!checkCd(p, s, "read", "Lecture", now)) return;
		Vec3 eye = p.getEyePosition();
		Vec3 look = p.getLookAngle();
		LivingEntity target = null;
		double best = Double.MAX_VALUE;
		for (LivingEntity e : lvl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(readRange),
				x -> x != p && x.isAlive() && !(x instanceof Player))) {
			Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
			double dist = to.length();
			if (dist > readRange || to.normalize().dot(look) < 0.96 || !p.hasLineOfSight(e)) continue;
			if (dist < best) {
				best = dist;
				target = e;
			}
		}
		if (target == null) {
			say(p, "§7Personne dans le viseur.");
			return;
		}
		if (!spendFood(p, readFood)) return;
		s.setCd("read", now, readCd);
		Compat.effect(target, MobEffects.GLOWING, readTicks, 0);
		Compat.effect(target, MobEffects.SLOWNESS, readTicks, 0);
		Compat.effect(target, MobEffects.WEAKNESS, readTicks, 0);
		say(p, "§9Lecture : " + target.getName().getString() + " - "
			+ Math.round(target.getHealth()) + "/" + Math.round(target.getMaxHealth()) + " PV");
		Compat.sound(lvl, p.getX(), p.getY(), p.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 0.7f);
		ping(p);
	}

	@Override
	public void tick(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {
		// Hotfix : l'objet en main se répare tout seul
		if (now % hotfixEvery == 0) {
			ItemStack st = p.getMainHandItem();
			if (!st.isEmpty() && st.isDamaged()) st.setDamageValue(Math.max(0, st.getDamageValue() - 1));
		}
	}

	@Override
	public void onKill(ServerPlayer p, PlayerState s, LivingEntity victim, long now) {
		long last = s.getL("streak_at", -99999);
		long streak = (now - last <= streakWindow) ? s.getL("streak", 0) + 1 : 1;
		s.setL("streak", streak);
		s.setL("streak_at", now);

		String title = null;
		if (streak == 1 && s.getL("first_blood_done", 0) == 0) {
			s.setL("first_blood_done", 1);
			title = "FIRST BLOOD !";
		} else if (streak == 2) title = "DOUBLE KILL !";
		else if (streak == 3) title = "TRIPLE KILL !";
		else if (streak == 4) title = "QUADRA KILL !";
		else if (streak >= 5) title = "PENTAKILL !";
		if (title == null) return;

		if (streak >= 2) Compat.effect(p, MobEffects.REGENERATION, 100, 0);
		String line = punchlines.get(p.level().getRandom().nextInt(punchlines.size()));
		lvlServer(p).getPlayerList().broadcastSystemMessage(
			Component.literal("§9[Rift] §b" + p.getName().getString() + " : §l" + title + "§r§7 " + line), false);
	}

	private static net.minecraft.server.MinecraftServer lvlServer(ServerPlayer p) {
		return Compat.level(p).getServer();
	}

	@Override
	public void onRespawn(ServerPlayer p, PlayerState s, long now) {
		// Tilt : après une mort, Flash est bloqué
		s.setCd("flash", now, tiltTicks);
		say(p, "§cTilt... Flash est en recharge.");
	}
}
