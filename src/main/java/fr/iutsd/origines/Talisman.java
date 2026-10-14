package fr.iutsd.origines;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** L'objet signature de chaque origine : un objet vanilla inoffensif renommé, utilisé en clic droit. */
public final class Talisman {
	private Talisman() {}

	public static ItemStack create(Origin o) {
		ItemStack st = new ItemStack(o.item);
		st.set(DataComponents.CUSTOM_NAME, Component.literal(o.itemName).withStyle(o.color));
		return st;
	}

	public static boolean is(ItemStack st, Origin o) {
		return !st.isEmpty() && st.is(o.item) && st.getHoverName().getString().equals(o.itemName);
	}

	/** Quelle origine correspond à cet objet ? (null si ce n'est pas un talisman) */
	public static Origin ofStack(ItemStack st) {
		if (st.isEmpty()) return null;
		for (Origin o : Origin.values()) {
			if (is(st, o)) return o;
		}
		return null;
	}

	public static boolean holding(ServerPlayer p, Origin o) {
		return is(p.getMainHandItem(), o) || is(p.getOffhandItem(), o);
	}

	public static boolean has(ServerPlayer p, Origin o) {
		var inv = p.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (is(inv.getItem(i), o)) return true;
		}
		return false;
	}

	/** Retire tous les talismans (changement d'origine). */
	public static void clear(ServerPlayer p) {
		var inv = p.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (ofStack(inv.getItem(i)) != null) inv.setItem(i, ItemStack.EMPTY);
		}
	}

	public static void giveIfMissing(ServerPlayer p, Origin o) {
		if (!has(p, o)) Compat.give(p, create(o));
	}
}
