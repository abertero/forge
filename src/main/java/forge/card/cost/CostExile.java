package forge.card.cost;

import java.util.Iterator;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.GameActionUtil;
import forge.Player;
import forge.PlayerZone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.gui.GuiUtils;
import forge.gui.input.Input;

/**
 * The Class CostExile.
 */
public class CostExile extends CostPartWithList {
    // Exile<Num/Type{/TypeDescription}>
    // ExileFromHand<Num/Type{/TypeDescription}>
    // ExileFromGraveyard<Num/Type{/TypeDescription}>
    // ExileFromTop<Num/Type{/TypeDescription}> (of library)

    private Constant.Zone from = Constant.Zone.Battlefield;

    /**
     * Gets the from.
     * 
     * @return the from
     */
    public final Constant.Zone getFrom() {
        return from;
    }

    /**
     * Instantiates a new cost exile.
     * 
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     * @param from
     *            the from
     */
    public CostExile(final String amount, final String type, final String description, final Constant.Zone from) {
        super(amount, type, description);
        if (from != null) {
            this.from = from;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        Integer i = convertAmount();
        sb.append("Exile ");

        if (getThis()) {
            sb.append(type);
            if (!from.equals(Zone.Battlefield)) {
                sb.append(" from your ").append(from);
            }
            return sb.toString();
        }

        if (from.equals(Zone.Battlefield)) {
            String desc = typeDescription == null ? type : typeDescription;

            sb.append(Cost.convertAmountTypeToWords(i, amount, desc));
            if (!getThis()) {
                sb.append(" you control");
            }
            return sb.toString();
        }

        if (i != null) {
            sb.append(i);
        } else {
            sb.append(amount);
        }
        if (!type.equals("Card")) {
            sb.append(" " + type);
        }
        sb.append(" card");
        if (i == null || i > 1) {
            sb.append("s");
        }
        sb.append(" from your ").append(from);

        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#refund(forge.Card)
     */
    @Override
    public void refund(final Card source) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability, final Card source, final Player activator, final Cost cost) {
        CardList typeList = activator.getCardsIn(getFrom());
        if (!getThis()) {
            typeList = typeList.getValidCards(getType().split(";"), activator, source);

            Integer amount = convertAmount();
            if (amount != null && typeList.size() < amount) {
                return false;
            }
        } else if (!typeList.contains(source)) {
            return false;
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#payAI(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final void payAI(final SpellAbility ability, final Card source, final Cost_Payment payment) {
        for (Card c : list)
            AllZone.getGameAction().exile(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#payHuman(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payHuman(final SpellAbility ability, final Card source, final Cost_Payment payment) {
        String amount = getAmount();
        Integer c = convertAmount();
        Player activator = ability.getActivatingPlayer();
        CardList list = activator.getCardsIn(getFrom());
        list = list.getValidCards(type.split(";"), activator, source);
        if (c == null) {
            String sVar = source.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = CostUtil.chooseXValue(source, list.size());
            } else {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }
        if (getThis()) {
            CostUtil.setInput(CostExile.exileThis(ability, payment, this));
        } else if (from.equals(Constant.Zone.Battlefield) || from.equals(Constant.Zone.Hand)) {
            CostUtil.setInput(CostExile.exileType(ability, this, getType(), payment, c));
        } else if (from.equals(Constant.Zone.Library)) {
            CostExile.exileFromTop(ability, this, payment, c);
        } else {
            CostUtil.setInput(CostExile.exileFrom(ability, this, getType(), payment, c));
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#decideAIPayment(forge.card.spellability.SpellAbility
     * , forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean decideAIPayment(final SpellAbility ability, final Card source, final Cost_Payment payment) {
        resetList();
        if (getThis()) {
            list.add(source);
        } else {
            Integer c = convertAmount();
            if (c == null) {
                String sVar = source.getSVar(amount);
                // Generalize this
                if (sVar.equals("XChoice")) {
                    return false;
                }

                c = AbilityFactory.calculateAmount(source, amount, ability);
            }

            if (from.equals(Constant.Zone.Library)) {
                list = AllZone.getComputerPlayer().getCardsIn(Zone.Library, c);
            } else {
                list = ComputerUtil.chooseExileFrom(getFrom(), getType(), source, ability.getTargetCard(), c);
            }
            if (list == null || list.size() < c) {
                return false;
            }
        }
        return true;
    }

    // Inputs

    /**
     * Exile from top.
     * 
     * @param sa
     *            the sa
     * @param part
     *            the part
     * @param payment
     *            the payment
     * @param nNeeded
     *            the n needed
     */
    public static void exileFromTop(final SpellAbility sa, final CostExile part, final Cost_Payment payment,
            final int nNeeded) {
        StringBuilder sb = new StringBuilder();
        sb.append("Exile ").append(nNeeded).append(" cards from the top of your library?");
        CardList list = sa.getActivatingPlayer().getCardsIn(Zone.Library, nNeeded);

        if (list.size() > nNeeded) {
            // I don't believe this is possible
            payment.cancelCost();
            return;
        }

        boolean doExile = GameActionUtil.showYesNoDialog(sa.getSourceCard(), sb.toString());
        if (doExile) {
            Iterator<Card> itr = list.iterator();
            while (itr.hasNext()) {
                Card c = (Card) itr.next();
                part.addToList(c);
                AllZone.getGameAction().exile(c);
            }
            part.addListToHash(sa, "Exiled");
            payment.paidCost(part);
        } else {
            payment.cancelCost();
        }
    }

    /**
     * Exile from.
     * 
     * @param sa
     *            the sa
     * @param part
     *            the part
     * @param type
     *            the type
     * @param payment
     *            the payment
     * @param nNeeded
     *            the n needed
     * @return the input
     */
    public static Input exileFrom(final SpellAbility sa, final CostExile part, final String type,
            final Cost_Payment payment, final int nNeeded) {
        Input target = new Input() {
            private static final long serialVersionUID = 734256837615635021L;
            CardList typeList;

            @Override
            public void showMessage() {
                if (nNeeded == 0) {
                    done();
                }

                typeList = sa.getActivatingPlayer().getCardsIn(part.getFrom());
                typeList = typeList.getValidCards(type.split(";"), sa.getActivatingPlayer(), sa.getSourceCard());

                for (int i = 0; i < nNeeded; i++) {
                    if (typeList.size() == 0) {
                        cancel();
                    }

                    Object o = GuiUtils.getChoiceOptional("Exile from " + part.getFrom(), typeList.toArray());

                    if (o != null) {
                        Card c = (Card) o;
                        typeList.remove(c);
                        part.addToList(c);
                        AllZone.getGameAction().exile(c);
                        if (i == nNeeded - 1) {
                            done();
                        }
                    } else {
                        cancel();
                        break;
                    }
                }
            }

            @Override
            public void selectButtonCancel() {
                cancel();
            }

            public void done() {
                stop();
                part.addListToHash(sa, "Exiled");
                payment.paidCost(part);
            }

            public void cancel() {
                stop();
                payment.cancelCost();
            }
        };
        return target;
    }// exileFrom()

    /**
     * <p>
     * exileType.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param part
     *            the part
     * @param type
     *            a {@link java.lang.String} object.
     * @param payment
     *            a {@link forge.card.cost.Cost_Payment} object.
     * @param nNeeded
     *            the n needed
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input exileType(final SpellAbility sa, final CostExile part, final String type,
            final Cost_Payment payment, final int nNeeded) {
        Input target = new Input() {
            private static final long serialVersionUID = 1403915758082824694L;

            private CardList typeList;
            private int nExiles = 0;

            @Override
            public void showMessage() {
                if (nNeeded == 0) {
                    done();
                }

                StringBuilder msg = new StringBuilder("Exile ");
                int nLeft = nNeeded - nExiles;
                msg.append(nLeft).append(" ");
                msg.append(type);
                if (nLeft > 1) {
                    msg.append("s");
                }

                if (part.getFrom().equals(Constant.Zone.Hand)) {
                    msg.append(" from your Hand");
                }
                typeList = sa.getActivatingPlayer().getCardsIn(part.getFrom());
                typeList = typeList.getValidCards(type.split(";"), sa.getActivatingPlayer(), sa.getSourceCard());
                AllZone.getDisplay().showMessage(msg.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                cancel();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (typeList.contains(card)) {
                    nExiles++;
                    part.addToList(card);
                    AllZone.getGameAction().exile(card);
                    typeList.remove(card);
                    // in case nothing else to exile
                    if (nExiles == nNeeded) {
                        done();
                    } else if (typeList.size() == 0) {
                        // happen
                        cancel();
                    } else {
                        showMessage();
                    }
                }
            }

            public void done() {
                stop();
                part.addListToHash(sa, "Exiled");
                payment.paidCost(part);
            }

            public void cancel() {
                stop();
                payment.cancelCost();
            }
        };

        return target;
    }// exileType()

    /**
     * <p>
     * exileThis.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param payment
     *            a {@link forge.card.cost.Cost_Payment} object.
     * @param part
     *            the part
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input exileThis(final SpellAbility sa, final Cost_Payment payment, final CostExile part) {
        Input target = new Input() {
            private static final long serialVersionUID = 678668673002725001L;

            @Override
            public void showMessage() {
                Card card = sa.getSourceCard();
                if (sa.getActivatingPlayer().isHuman()
                        && sa.getActivatingPlayer().getZone(part.getFrom()).contains(card)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName());
                    sb.append(" - Exile?");
                    Object[] possibleValues = { "Yes", "No" };
                    Object choice = JOptionPane.showOptionDialog(null, sb.toString(), card.getName() + " - Cost",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, possibleValues,
                            possibleValues[0]);
                    if (choice.equals(0)) {
                        payment.getAbility().addCostToHashList(card, "Exiled");
                        AllZone.getGameAction().exile(card);
                        part.addToList(card);
                        stop();
                        part.addListToHash(sa, "Exiled");
                        payment.paidCost(part);
                    } else {
                        stop();
                        payment.cancelCost();
                    }
                }
            }
        };

        return target;
    }// input_exile()
}
