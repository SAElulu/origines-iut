package fr.iutsd.origines;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

/**
 * /origine                      : liste des personnages
 * /origine info <nom>           : fiche complète
 * /origine choisir <nom>        : choisir son personnage (une fois, sauf si allow_player_change = 1 dans la config)
 * /origine talisman             : récupérer son objet signature
 * /origine admin set <joueur> <nom> | reset <joueur> : réservé aux opérateurs
 */
public final class OrigineCommand {
	private static final SuggestionProvider<CommandSourceStack> NAMES = (ctx, b) ->
		SharedSuggestionProvider.suggest(Arrays.stream(Origin.values()).map(o -> o.id), b);

	private OrigineCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> d) {
		d.register(Commands.literal("origine")
			.executes(ctx -> list(ctx.getSource()))
			.then(Commands.literal("info")
				.then(Commands.argument("nom", StringArgumentType.word()).suggests(NAMES)
					.executes(ctx -> info(ctx.getSource(), StringArgumentType.getString(ctx, "nom")))))
			.then(Commands.literal("choisir")
				.then(Commands.argument("nom", StringArgumentType.word()).suggests(NAMES)
					.executes(ctx -> choose(ctx.getSource(), StringArgumentType.getString(ctx, "nom")))))
			.then(Commands.literal("talisman").executes(ctx -> talisman(ctx.getSource())))
			.then(Commands.literal("admin").requires(Compat::isAdmin)
				.then(Commands.literal("set")
					.then(Commands.argument("joueur", EntityArgument.player())
						.then(Commands.argument("nom", StringArgumentType.word()).suggests(NAMES)
							.executes(ctx -> adminSet(ctx, StringArgumentType.getString(ctx, "nom"))))))
				.then(Commands.literal("reset")
					.then(Commands.argument("joueur", EntityArgument.player())
						.executes(OrigineCommand::adminReset)))));
	}

	private static int list(CommandSourceStack src) {
		src.sendSuccess(() -> Component.literal("=== Origines IUT SD ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
		for (Origin o : Origin.values()) {
			src.sendSuccess(() -> Component.literal(" " + o.id + " : ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(o.label + ", " + o.title).withStyle(o.color)), false);
		}
		src.sendSuccess(() -> Component.literal("/origine info <nom>  |  /origine choisir <nom>").withStyle(ChatFormatting.YELLOW), false);
		return 1;
	}

	private static int info(CommandSourceStack src, String name) {
		Origin o = Origin.byId(name);
		if (o == null) {
			src.sendFailure(Component.literal("Personnage inconnu. Tape /origine pour la liste."));
			return 0;
		}
		o.describe().forEach(c -> src.sendSuccess(() -> c, false));
		return 1;
	}

	private static int choose(CommandSourceStack src, String name) throws CommandSyntaxException {
		ServerPlayer p = src.getPlayerOrException();
		Origin o = Origin.byId(name);
		if (o == null) {
			src.sendFailure(Component.literal("Personnage inconnu. Tape /origine pour la liste."));
			return 0;
		}
		PlayerState s = Store.get(p.getUUID());
		if (s.origin != null && !Config.flag("allow_player_change", false) && !Compat.isAdmin(src)) {
			src.sendFailure(Component.literal("Tu as déjà un personnage. Demande à un admin de le changer."));
			return 0;
		}
		apply(p, s, o);
		return 1;
	}

	private static int talisman(CommandSourceStack src) throws CommandSyntaxException {
		ServerPlayer p = src.getPlayerOrException();
		Origin o = Origin.byId(Store.get(p.getUUID()).origin);
		if (o == null) {
			src.sendFailure(Component.literal("Choisis d'abord un personnage avec /origine choisir <nom>."));
			return 0;
		}
		Talisman.giveIfMissing(p, o);
		src.sendSuccess(() -> Component.literal("Voilà ton objet : " + o.itemName).withStyle(o.color), false);
		return 1;
	}

	private static int adminSet(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
		Origin o = Origin.byId(name);
		if (o == null) {
			ctx.getSource().sendFailure(Component.literal("Personnage inconnu."));
			return 0;
		}
		ServerPlayer target = EntityArgument.getPlayer(ctx, "joueur");
		apply(target, Store.get(target.getUUID()), o);
		ctx.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " est maintenant " + o.label), true);
		return 1;
	}

	private static int adminReset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(ctx, "joueur");
		PlayerState s = Store.get(target.getUUID());
		s.origin = null;
		s.resetTransient();
		Talisman.clear(target);
		ctx.getSource().sendSuccess(() -> Component.literal("Origine de " + target.getName().getString() + " réinitialisée."), true);
		Compat.chat(target, Component.literal("Ton personnage a été réinitialisé : tape /origine pour en choisir un."));
		return 1;
	}

	private static void apply(ServerPlayer p, PlayerState s, Origin o) {
		Talisman.clear(p);
		s.origin = o.id;
		s.resetTransient();
		Talisman.giveIfMissing(p, o);
		o.describe().forEach(c -> Compat.chat(p, c));
		Compat.chat(p, Component.literal("Utilise ton objet \"" + o.itemName + "\" en clic droit (et accroupi + clic droit).").withStyle(ChatFormatting.YELLOW));
	}
}
