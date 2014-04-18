package forge.card;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import forge.GuiBase;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.item.InventoryItemFromSet;
import forge.item.PreconDeck;
import forge.item.SealedProduct;
import forge.util.Lang;

public class CardDetailUtil {
    private CardDetailUtil() {
    }

    public enum CardBorderColor {
        WHITE(254, 253, 244),
        BLUE(90, 146, 202),
        BLACK(32, 34, 31),
        RED(253, 66, 40),
        GREEN(22, 115, 69),
        MULTICOLOR(248, 219, 85),
        COLORLESS(160, 166, 164),
        LAND(190, 153, 112),
        FACE_DOWN(83, 61, 40),
        UNKNOWN(200, 0, 230);

        public final int r, g, b;

        private CardBorderColor(int r0, int g0, int b0) {
            r = r0;
            g = g0;
            b = b0;
        }
    }

    public static CardBorderColor getBorderColor(final Card card, boolean canShow) {
        return getBorderColors(card, canShow, false).get(0);
    }

    public static List<CardBorderColor> getBorderColors(final Card card, boolean canShow, boolean supportMultiple) {
        List<CardBorderColor> borderColors = new ArrayList<CardBorderColor>();
        ColorSet cardColors = card.determineColor();

        if (!canShow) {
            borderColors.add(CardBorderColor.FACE_DOWN);
        }
        else if (cardColors.isColorless()) {
            if (card.isLand()) { //use different color for lands vs. other colorless cards
                borderColors.add(CardBorderColor.LAND);
            }
            else {
                borderColors.add(CardBorderColor.COLORLESS);
            }
        }
        else {
            int colorCount = cardColors.countColors();
            if (colorCount > 2 || (colorCount > 1 && !supportMultiple)) {
                borderColors.add(CardBorderColor.MULTICOLOR);
            }
            else if (cardColors.hasWhite()) {
                if (colorCount == 1) {
                    borderColors.add(CardBorderColor.WHITE);
                }
                else if (cardColors.hasBlue()) {
                    borderColors.add(CardBorderColor.WHITE);
                    borderColors.add(CardBorderColor.BLUE);
                }
                else if (cardColors.hasBlack()) {
                    borderColors.add(CardBorderColor.WHITE);
                    borderColors.add(CardBorderColor.BLACK);
                }
                else if (cardColors.hasRed()) {
                    borderColors.add(CardBorderColor.RED);
                    borderColors.add(CardBorderColor.WHITE);
                }
                else if (cardColors.hasGreen()) {
                    borderColors.add(CardBorderColor.GREEN);
                    borderColors.add(CardBorderColor.WHITE);
                }
            }
            else if (cardColors.hasBlue()) {
                if (colorCount == 1) {
                    borderColors.add(CardBorderColor.BLUE);
                }
                else if (cardColors.hasBlack()) {
                    borderColors.add(CardBorderColor.BLUE);
                    borderColors.add(CardBorderColor.BLACK);
                }
                else if (cardColors.hasRed()) {
                    borderColors.add(CardBorderColor.BLUE);
                    borderColors.add(CardBorderColor.RED);
                }
                else if (cardColors.hasGreen()) {
                    borderColors.add(CardBorderColor.GREEN);
                    borderColors.add(CardBorderColor.BLUE);
                }
            }
            else if (cardColors.hasBlack()) {
                if (colorCount == 1) {
                    borderColors.add(CardBorderColor.BLACK);
                }
                else if (cardColors.hasRed()) {
                    borderColors.add(CardBorderColor.BLACK);
                    borderColors.add(CardBorderColor.RED);
                }
                else if (cardColors.hasGreen()) {
                    borderColors.add(CardBorderColor.BLACK);
                    borderColors.add(CardBorderColor.GREEN);
                }
            }
            else if (cardColors.hasRed()) { //if we got this far, must be mono-red or red-green
                borderColors.add(CardBorderColor.RED);
                if (cardColors.hasGreen()) {
                    borderColors.add(CardBorderColor.GREEN);
                }
            }
            else if (cardColors.hasGreen()) { //if we got this far, must be mono-green
                borderColors.add(CardBorderColor.GREEN);
            }
        }

        if (borderColors.isEmpty()) { // If your card has a violet border, something is wrong
            borderColors.add(CardBorderColor.UNKNOWN);
        }
        return borderColors;
    }

    public static String getItemDescription(final InventoryItemFromSet item) {
        if (item instanceof SealedProduct) {
            return ((SealedProduct)item).getDescription();
        }
        if (item instanceof PreconDeck) {
            return ((PreconDeck) item).getDescription();
        }
        return item.getName(); 
    }

    public static String formatCardType(final Card card) {
        final ArrayList<String> list = card.getType();
        final StringBuilder sb = new StringBuilder();
    
        final ArrayList<String> superTypes = new ArrayList<String>();
        final ArrayList<String> cardTypes = new ArrayList<String>();
        final ArrayList<String> subTypes = new ArrayList<String>();
        final boolean allCreatureTypes = list.contains("AllCreatureTypes");
    
        for (final String t : list) {
            if (allCreatureTypes && t.equals("AllCreatureTypes")) {
                continue;
            }
            if (CardType.isASuperType(t) && !superTypes.contains(t)) {
                superTypes.add(t);
            }
            if (CardType.isACardType(t) && !cardTypes.contains(t)) {
                cardTypes.add(t);
            }
            if (CardType.isASubType(t) && !subTypes.contains(t) && (!allCreatureTypes || !CardType.isACreatureType(t))) {
                subTypes.add(t);
            }
        }
    
        for (final String type : superTypes) {
            sb.append(type).append(" ");
        }
        for (final String type : cardTypes) {
            sb.append(type).append(" ");
        }
        if (!subTypes.isEmpty() || allCreatureTypes) {
            sb.append("- ");
        }
        if (allCreatureTypes) {
            sb.append("All creature types ");
        }
        for (final String type : subTypes) {
            sb.append(type).append(" ");
        }
    
        return sb.toString();
    }

    public static String formatPowerToughness(final Card card) {
        StringBuilder ptText = new StringBuilder();
        if (card.isCreature()) {
            ptText.append(card.getNetAttack()).append(" / ").append(card.getNetDefense());
        }

        if (card.isPlaneswalker()) {
            if (ptText.length() > 0) {
                ptText.insert(0, "P/T: ");
                ptText.append(" - ").append("Loy: ");
            } else {
                ptText.append("Loyalty: ");
            }

            int loyalty = card.getCounters(CounterType.LOYALTY);
            if (loyalty == 0) {
                loyalty = card.getBaseLoyalty();
            }
            ptText.append(loyalty);
        }
        return ptText.toString();
    }
    
    public static String formatCardId(final Card card) {
        return card.getUniqueNumber() > 0 ? "[" + card.getUniqueNumber() + "]" : "";
    }

    public static String composeCardText(final Card card, final boolean canShow) {
        final StringBuilder area = new StringBuilder();

        // Token
        if (card.isToken()) {
            area.append("Token");
        }

        if (canShow) {
            // card text
            if (area.length() != 0) {
                area.append("\n");
            }
            String text = card.getText();
            // LEVEL [0-9]+-[0-9]+
            // LEVEL [0-9]+\+

            String regex = "LEVEL [0-9]+-[0-9]+ ";
            text = text.replaceAll(regex, "$0\r\n");

            regex = "LEVEL [0-9]+\\+ ";
            text = text.replaceAll(regex, "\r\n$0\r\n");

            // displays keywords that have dots in them a little better:
            regex = "\\., ";
            text = text.replaceAll(regex, ".\r\n");

            area.append(text);
        }

        if (card.isPhasedOut()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Phased Out");
        }

        // counter text
        final CounterType[] counters = CounterType.values();
        for (final CounterType counter : counters) {
            if (card.getCounters(counter) != 0) {
                if (area.length() != 0) {
                    area.append("\n");
                }
                area.append(counter.getName() + " counters: ");
                area.append(card.getCounters(counter));
            }
        }

        if (card.isCreature()) {
            int damage = card.getDamage();
            if (damage > 0) {
                if (area.length() != 0) {
                    area.append("\n");
                }
                area.append("Damage: " + damage);
            }
            int assigned = card.getTotalAssignedDamage();
            if (assigned > 0) {
                if (area.length() != 0) {
                    area.append("\n");
                }
                area.append("Assigned Damage: " + assigned);
            }
        }
        if (card.isPlaneswalker()) {
            int assigned = card.getTotalAssignedDamage();
            if (assigned > 0) {
                if (area.length() != 0) {
                    area.append("\n");
                }
                area.append("Assigned Damage: " + assigned);
            }
        }

        // Regeneration Shields
        final int regenShields = card.getShield().size();
        if (regenShields > 0) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Regeneration Shield(s): ").append(regenShields);
        }

        // Damage Prevention
        final int preventNextDamage = card.getPreventNextDamageTotalShields();
        if (preventNextDamage > 0) {
            area.append("\n");
            area.append("Prevent the next ").append(preventNextDamage).append(" damage that would be dealt to ");
            area.append(card.getName()).append(" this turn.");
        }

        // top revealed
        if ((card.hasKeyword("Play with the top card of your library revealed.") || card
                .hasKeyword("Players play with the top card of their libraries revealed."))
                && card.getController() != null
                && (card.isInZone(ZoneType.Battlefield) || (card.isInZone(ZoneType.Command) && !card.isCommander()))
                && !card.getController().getZone(ZoneType.Library).isEmpty()) {
            area.append("\r\nTop card of your library: ");
            area.append(card.getController().getCardsIn(ZoneType.Library, 1));
            if (card.hasKeyword("Players play with the top card of their libraries revealed.")) {
                for (final Player p : card.getController().getAllOtherPlayers()) {
                    if (p.getZone(ZoneType.Library).isEmpty()) {
                        area.append(p.getName());
                        area.append("'s library is empty.");
                    } else {
                        area.append("\r\nTop card of ");
                        area.append(p.getName());
                        area.append("'s library: ");
                        area.append(p.getCardsIn(ZoneType.Library, 1));
                    }
                }
            }
        }

        // chosen type
        if (!card.getChosenType().equals("")) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(chosen type: ");
            area.append(card.getChosenType());
            area.append(")");
        }

        // chosen color
        if (!card.getChosenColor().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(chosen colors: ");
            area.append(card.getChosenColor());
            area.append(")");
        }

        // chosen player
        if (card.getChosenPlayer() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(chosen player: " + card.getChosenPlayer() + ")");
        }

        // named card
        if (!card.getNamedCard().equals("")) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(named card: ");
            area.append(card.getNamedCard());
            area.append(")");
        }

        // equipping
        if (!card.getEquipping().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("=Equipping ");
            area.append(card.getEquipping().get(0));
            area.append("=");
        }

        // equipped by
        if (!card.getEquippedBy().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("=Equipped by ");
            for (final Iterator<Card> it = card.getEquippedBy().iterator(); it.hasNext();) {
                area.append(it.next());
                if (it.hasNext()) {
                    area.append(", ");
                }
            }
            area.append("=");
        }

        // enchanting
        final GameEntity entity = card.getEnchanting();
        if (entity != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("*Enchanting ");

            if (entity instanceof Card) {
                final Card c = (Card) entity;
                if (!GuiBase.getInterface().mayShowCard(c)) {
                    area.append("Morph (");
                    area.append(card.getUniqueNumber());
                    area.append(")");
                } else {
                    area.append(entity);
                }
            } else {
                area.append(entity);
            }
            area.append("*");
        }

        // enchanted by
        if (!card.getEnchantedBy().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("*Enchanted by ");
            for (final Iterator<Card> it = card.getEnchantedBy().iterator(); it.hasNext();) {
                area.append(it.next());
                if (it.hasNext()) {
                    area.append(", ");
                }
            }
            area.append("*");
        }

        // controlling
        if (card.getGainControlTargets().size() > 0) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("+Controlling: ");
            for (final Iterator<Card> it = card.getGainControlTargets().iterator(); it.hasNext();) {
                area.append(it.next());
                if (it.hasNext()) {
                    area.append(", ");
                }
            }
            area.append("+");
        }

        // cloned via
        if (card.getCloneOrigin() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("^Cloned via: ");
            area.append(card.getCloneOrigin().getName());
            area.append("^");
        }

        // Imprint
        if (!card.getImprinted().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Imprinting: ");
            for (final Iterator<Card> it = card.getImprinted().iterator(); it.hasNext();) {
                area.append(it.next());
                if (it.hasNext()) {
                    area.append(", ");
                }
            }
        }

        // Haunt
        if (!card.getHauntedBy().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Haunted by: ");
            for (final Iterator<Card> it = card.getHauntedBy().iterator(); it.hasNext();) {
                area.append(it.next());
                if (it.hasNext()) {
                    area.append(", ");
                }
            }
        }
        if (card.getHaunting() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Haunting " + card.getHaunting());
        }

        // must block
        if (card.getMustBlockCards() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            String mustBlockThese = Lang.joinHomogenous(card.getMustBlockCards());
            area.append("Must block " + mustBlockThese);
        }
        return area.toString();
    }
}
