package fr.iutsd.origines;

import fr.iutsd.origines.power.Anouchka;
import fr.iutsd.origines.power.Baptou;
import fr.iutsd.origines.power.Elie;
import fr.iutsd.origines.power.Lucas;
import fr.iutsd.origines.power.Remi;
import fr.iutsd.origines.power.ThomasD;
import fr.iutsd.origines.power.ThomasH;
import fr.iutsd.origines.power.Timothe;
import fr.iutsd.origines.power.Yago;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Les 9 personnages. Les valeurs chiffrées exactes sont dans config/origines_iut.json. */
public enum Origin {
	THOMAS_H("thomas_h", "Thomas_H", "Le Pongiste Quantique", ChatFormatting.AQUA, Items.STICK, "Raquette", new ThomasH(),
		new String[][]{{"smash", "Smash"}, {"return", "Renvoi"}},
		"SMASH : frappe tous les monstres devant toi (dégâts + recul). Coûte de la faim.",
		"RENVOI : pendant 1,5 s, les flèches et projectiles qui t'approchent repartent vers leur tireur.",
		"Jeu de jambes : Vitesse I tant que tu tiens la raquette.",
		"Un plastron en fer, diamant ou netherite t'enlève tout : la raquette demande de la souplesse."),

	THOMAS_D("thomas_d", "Thomas_D", "L'Archiviste", ChatFormatting.GREEN, Items.PAPER, "Disquette", new ThomasD(),
		new String[][]{{"save", "Sauvegarde"}, {"load", "Chargement"}},
		"SAUVEGARDE : enregistre ta position (valable 10 min, même dimension).",
		"CHARGEMENT : après 5 s immobile (annulé si touché), retour à la sauvegarde. Coûte 3 niveaux d'XP.",
		"Chercheur : miner un minerai donne de l'XP en plus.",
		"Mise à jour forcée : environ toutes les 20 min, quand tout est calme, tu restes figé 5 s."),

	TIMOTHE("timothe", "Timothe", "Le Collectionneur All-Star", ChatFormatting.GOLD, Items.MAGMA_CREAM, "Ballon", new Timothe(),
		new String[][]{{"dunk", "Dunk"}},
		"DUNK : bond de basketteur puis écrasement au sol qui blesse et repousse les monstres autour. Pas de dégâts de chute.",
		"(aucune)",
		"Collection : chaque NOUVEL objet ramassé donne de l'XP et un coup de Hype ; des paliers donnent des récompenses. Tes animaux apprivoisés proches régénèrent.",
		"Mauvais perdant : un dunk dans le vide te ralentit ; mourir te laisse affaibli 3 min."),

	ANOUCHKA("anouchka", "Anouchka", "La Vagabonde Nocturne", ChatFormatting.DARK_PURPLE, Items.BOOK, "Carnet de voyage", new Anouchka(),
		new String[][]{{"smoke", "Fumée"}, {"eye", "Ombres"}},
		"FUMÉE NOIRE : nuage qui fait perdre leur cible aux monstres, tu deviens invisible. Consomme 1 charbon.",
		"OMBRES : fait briller les monstres dans 32 blocs pendant 6 s.",
		"Enfant de la nuit : dans le noir, vision nocturne et Vitesse I. Chaque NOUVEAU biome visité donne de l'XP et de la vitesse.",
		"Héliophobie : en plein soleil sans casque, Faiblesse. Besoin de liberté : rester 5 min dans la même zone te donne la Faim."),

	YAGO("yago", "Yago", "L'Ambassadeur du Carnaval", ChatFormatting.YELLOW, Items.GOLD_NUGGET, "Maracas", new Yago(),
		new String[][]{{"batucada", "Batucada"}, {"confetti", "Confettis"}},
		"BATUCADA : 10 s de fête. Toi et les joueurs proches gagnez Vitesse et Hâte, tous les animaux adultes proches entrent en amour.",
		"CONFETTIS : les monstres autour de toi se figent quelques secondes.",
		"Ambassadeur : près d'un villageois, tu as les prix du Héros du village.",
		"Impossible de se taire : après la Batucada, les monstres proches te prennent pour cible. Puis gueule de bois (nausée et lenteur)."),

	REMI("remi", "Rémi", "Le Sergent Ludique", ChatFormatting.DARK_GREEN, Items.COPPER_INGOT, "Sifflet de sergent", new Remi(),
		new String[][]{{"order", "Ordre"}, {"dice", "Dé"}},
		"ORDRE : tes loups apprivoisés et les golems de fer proches attaquent le monstre le plus proche, avec Force.",
		"DÉ : lance un d6. 1 = raté, 2-3 = régénération, 4-5 = absorption, 6 = coup critique (Force + Résistance).",
		"Tenue réglementaire : armure complète du même matériau = Résistance I.",
		"Endurance limitée : après 8 s de sprint continu, tu dois reprendre ton souffle (ralentissement bref)."),

	ELIE("elie", "Elie", "Le Ressort Humain", ChatFormatting.LIGHT_PURPLE, Items.SLIME_BALL, "Ressort", new Elie(),
		new String[][]{{"bound", "Air Jordan"}, {"grimace", "Grimace"}},
		"AIR JORDAN : saut géant et chute au ralenti (appuie sur saut après le clic).",
		"GRIMACE : les monstres proches sont figés de peur quelques secondes.",
		"Ressort : tu ne prends aucun dégât de chute en dessous de 8 blocs environ.",
		"Aucune discrétion : les monstres te repèrent de loin quand tu bouges. Inarrêtable : 3 bonds en 1 min = coup de fatigue."),

	LUCAS("lucas", "Lucas", "Le Stratège Suprême", ChatFormatting.RED, Items.CLOCK, "Plan parfait", new Lucas(),
		new String[][]{{"counter", "Contre"}, {"joke", "Blague"}},
		"CONTRE : répond au dernier danger subi (feu, explosion, chute, noyade, flèche, mêlée, magie) par la protection adaptée.",
		"BLAGUE : faux bruit de creeper à distance, les monstres vont voir ce qui se passe.",
		"Adaptation : après 3 coups du même type en 90 s, tu encaisses beaucoup mieux ce type pendant 90 s.",
		"Ego : un Contre inutile t'affaiblit. Surprise : un type de dégâts jamais vu depuis 5 min te sonne."),

	BAPTOU("baptou", "Baptou", "Le Commentateur du Rift", ChatFormatting.BLUE, Items.PRISMARINE_SHARD, "Cristal d'invocateur", new Baptou(),
		new String[][]{{"flash", "Flash"}, {"read", "Lecture"}},
		"FLASH : téléportation de 7 blocs dans la direction du regard (bloquée par les murs). Coûte de la faim.",
		"LECTURE : analyse la créature visée (PV, affichage) : elle brille, ralentie et affaiblie. Coûte de la faim.",
		"Annonceur : tes kills s'enchaînent en Double Kill, Triple Kill... avec punchlines dans le chat. Hotfix : ton outil en main se répare tout seul.",
		"Flame dans le chat : chaque sort te fait briller 3 s (tout le monde te voit). Tilt : après une mort, Flash est bloqué 2 min."),
	;

	public final String id;
	public final String label;
	public final String title;
	public final ChatFormatting color;
	public final Item item;
	public final String itemName;
	public final OriginPower power;
	/** Paires {clé de recharge, nom affiché} pour la barre de recharges. */
	public final String[][] cooldownKeys;
	public final String primaryText;
	public final String secondaryText;
	public final String passiveText;
	public final String malusText;

	Origin(String id, String label, String title, ChatFormatting color, Item item, String itemName, OriginPower power,
	       String[][] cooldownKeys, String primaryText, String secondaryText, String passiveText, String malusText) {
		this.id = id;
		this.label = label;
		this.title = title;
		this.color = color;
		this.item = item;
		this.itemName = itemName;
		this.power = power;
		this.cooldownKeys = cooldownKeys;
		this.primaryText = primaryText;
		this.secondaryText = secondaryText;
		this.passiveText = passiveText;
		this.malusText = malusText;
	}

	public static Origin byId(String id) {
		if (id == null) return null;
		for (Origin o : values()) {
			if (o.id.equalsIgnoreCase(id)) return o;
		}
		return null;
	}

	/** Fiche complète affichée dans le chat. */
	public List<Component> describe() {
		List<Component> out = new ArrayList<>();
		out.add(Component.literal("=== " + label + " : " + title + " ===").withStyle(color, ChatFormatting.BOLD));
		out.add(Component.literal("Objet : " + itemName).withStyle(ChatFormatting.GRAY));
		out.add(Component.literal("Clic droit : ").withStyle(ChatFormatting.WHITE).append(Component.literal(primaryText).withStyle(ChatFormatting.GREEN)));
		out.add(Component.literal("Accroupi + clic droit : ").withStyle(ChatFormatting.WHITE).append(Component.literal(secondaryText).withStyle(ChatFormatting.GREEN)));
		out.add(Component.literal("Passifs : ").withStyle(ChatFormatting.WHITE).append(Component.literal(passiveText).withStyle(ChatFormatting.AQUA)));
		out.add(Component.literal("Malus : ").withStyle(ChatFormatting.WHITE).append(Component.literal(malusText).withStyle(ChatFormatting.RED)));
		return out;
	}
}
