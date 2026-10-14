package fr.iutsd.origines;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OriginsMod implements ModInitializer {
	public static final String MOD_ID = "origines_iut";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Charge les 9 origines (et enregistre leurs valeurs dans la config)
		Origin.values();
		Config.flush();

		ServerLifecycleEvents.SERVER_STARTED.register(Store::load);
		ServerLifecycleEvents.SERVER_STOPPING.register(Store::save);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> OrigineCommand.register(dispatcher));
		ServerTickEvents.END_SERVER_TICK.register(OriginsMod::tick);

		// Réapparition : talisman rendu + effets de malus de mort
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			PlayerState s = Store.get(newPlayer.getUUID());
			Origin o = Origin.byId(s.origin);
			if (o == null) return;
			long now = Compat.level(newPlayer).getServer().getTickCount();
			Talisman.giveIfMissing(newPlayer, o);
			o.power.onRespawn(newPlayer, s, now);
		});

		// Dégâts : certaines origines en annulent une partie (chutes...)
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer p) {
				PlayerState s = Store.get(p.getUUID());
				Origin o = Origin.byId(s.origin);
				if (o != null) return o.power.allowDamage(p, s, source, amount, Compat.level(p).getServer().getTickCount());
			}
			return true;
		});

		// Kills (annonceur de Baptou...)
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof ServerPlayer p && entity != p) {
				PlayerState s = Store.get(p.getUUID());
				Origin o = Origin.byId(s.origin);
				if (o != null) o.power.onKill(p, s, entity, Compat.level(p).getServer().getTickCount());
			}
		});

		// Minage
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (player instanceof ServerPlayer p) {
				PlayerState s = Store.get(p.getUUID());
				Origin o = Origin.byId(s.origin);
				if (o != null) o.power.onBlockBroken(p, s, state);
			}
		});

		// Clic droit avec le talisman : primaire ; accroupi : secondaire
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (!(player instanceof ServerPlayer p) || !(level instanceof ServerLevel lvl)) return InteractionResult.PASS;
			ItemStack st = p.getItemInHand(hand);
			Origin held = Talisman.ofStack(st);
			if (held == null) return InteractionResult.PASS;
			PlayerState s = Store.get(p.getUUID());
			if (!held.id.equals(s.origin)) {
				Compat.actionbar(p, Component.literal("§cCet objet n'est pas fait pour toi."));
				return InteractionResult.PASS;
			}
			long now = lvl.getServer().getTickCount();
			if (p.isShiftKeyDown()) held.power.secondary(p, s, lvl, now);
			else held.power.primary(p, s, lvl, now);
			return InteractionResult.SUCCESS;
		});

		LOGGER.info("Origines IUT SD chargé : {} personnages.", Origin.values().length);
	}

	private static void tick(MinecraftServer server) {
		long now = server.getTickCount();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			PlayerState s = Store.get(p.getUUID());
			Origin o = Origin.byId(s.origin);
			if (o == null) {
				if (now % 600 == 40) Compat.actionbar(p, Component.literal("§eTape /origine pour choisir ton personnage !"));
				continue;
			}
			ServerLevel lvl = Compat.level(p);
			o.power.tick(p, s, lvl, now);
			if (now % 10 == 0 && Talisman.holding(p, o)) showCooldowns(p, s, o, now);
		}
		if (now % 6000 == 0) Store.save(server);
	}

	/** Recharges visibles dans la barre d'action quand le talisman est en main. */
	private static void showCooldowns(ServerPlayer p, PlayerState s, Origin o, long now) {
		StringBuilder sb = new StringBuilder();
		for (String[] cd : o.cooldownKeys) {
			long r = s.remaining(cd[0], now);
			if (sb.length() > 0) sb.append("  §8|  ");
			sb.append("§f").append(cd[1]).append(' ');
			sb.append(r <= 0 ? "§aprêt" : "§c" + ((r + 19) / 20) + " s");
		}
		Compat.actionbar(p, Component.literal(sb.toString()));
	}
}
