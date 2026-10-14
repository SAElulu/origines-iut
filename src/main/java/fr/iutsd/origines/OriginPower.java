package fr.iutsd.origines;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.block.state.BlockState;

/** Comportement d'une origine. Toutes les méthodes sont optionnelles. 'now' = tick du serveur. */
public abstract class OriginPower {
	/** Clic droit avec l'objet signature. */
	public void primary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {}

	/** Accroupi + clic droit avec l'objet signature. */
	public void secondary(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {}

	/** Appelé à chaque tick serveur pour un joueur de cette origine. */
	public void tick(ServerPlayer p, PlayerState s, ServerLevel lvl, long now) {}

	public void onRespawn(ServerPlayer p, PlayerState s, long now) {}

	/** Renvoyer false annule les dégâts. */
	public boolean allowDamage(ServerPlayer p, PlayerState s, DamageSource src, float amount, long now) {
		return true;
	}

	public void onBlockBroken(ServerPlayer p, PlayerState s, BlockState state) {}

	public void onKill(ServerPlayer p, PlayerState s, LivingEntity victim, long now) {}

	// ---------- utilitaires partagés ----------

	protected static void say(ServerPlayer p, String text) {
		Compat.actionbar(p, Component.literal(text));
	}

	/** Vérifie la recharge ; sinon affiche le temps restant. */
	protected static boolean checkCd(ServerPlayer p, PlayerState s, String key, String label, long now) {
		long r = s.remaining(key, now);
		if (r > 0) {
			say(p, "§c" + label + " : encore " + ((r + 19) / 20) + " s");
			return false;
		}
		return true;
	}

	/** Coût en faim (points de nourriture). Refuse si le joueur est trop affamé. */
	protected static boolean spendFood(ServerPlayer p, int cost) {
		FoodData f = p.getFoodData();
		if (f.getFoodLevel() <= cost) {
			say(p, "§cTrop affamé pour ça !");
			return false;
		}
		f.setFoodLevel(f.getFoodLevel() - cost);
		return true;
	}
}
