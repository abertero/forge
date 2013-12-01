package forge.ai.ability;

import java.util.ArrayList;
import java.util.List;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

public class DamagePreventAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Card hostCard = sa.getSourceCard();
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        boolean chance = false;

        final Cost cost = sa.getPayCosts();

        // temporarily disabled until better AI
        if (!ComputerUtilCost.checkLifeCost(ai, cost, hostCard, 4, null)) {
            return false;
        }

        if (!ComputerUtilCost.checkDiscardCost(ai, cost, hostCard)) {
            return false;
        }

        if (!ComputerUtilCost.checkSacrificeCost(ai, cost, hostCard)) {
            return false;
        }

        if (!ComputerUtilCost.checkRemoveCounterCost(cost, hostCard)) {
            return false;
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt == null) {
            // As far as I can tell these Defined Cards will only have one of
            // them
            final List<GameObject> objects = AbilityUtils.getDefinedObjects(sa.getSourceCard(), sa.getParam("Defined"), sa);

            // react to threats on the stack
            if (!game.getStack().isEmpty()) {
                final List<GameObject> threatenedObjects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa);
                for (final Object o : objects) {
                    if (threatenedObjects.contains(o)) {
                        chance = true;
                    }
                }
            } else {
                PhaseHandler handler = game.getPhaseHandler();
                if (handler.is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                    boolean flag = false;
                    for (final Object o : objects) {
                        if (o instanceof Card) {
                            flag |= ComputerUtilCombat.combatantWouldBeDestroyed(ai, (Card) o, combat);
                        } else if (o instanceof Player) {
                            // Don't need to worry about Combat Damage during AI's turn
                            final Player p = (Player) o;
                            if (!handler.isPlayerTurn(p)) {
                                flag |= (p == ai && ((ComputerUtilCombat.wouldLoseLife(ai, combat) && sa
                                        .isAbility()) || ComputerUtilCombat.lifeInDanger(ai, combat)));
                            }
                        }
                    }

                    chance = flag;
                } else { // if nothing on the stack, and it's not declare
                         // blockers. no need to prevent
                    return false;
                }
            }
        } // non-targeted

        // react to threats on the stack
        else if (!game.getStack().isEmpty()) {
            sa.resetTargets();
            // check stack for something on the stack will kill anything i control
            final List<GameObject> objects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa);

            if (objects.contains(ai)) {
                sa.getTargets().add(ai);
                chance = true;
            }
            final List<Card> threatenedTargets = new ArrayList<Card>();
            // filter AIs battlefield by what I can target
            List<Card> targetables = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), ai, hostCard);
            targetables = CardLists.getTargetableCards(targetables, sa);

            for (final Card c : targetables) {
                if (objects.contains(c)) {
                    threatenedTargets.add(c);
                }
            }

            if (!threatenedTargets.isEmpty()) {
                // Choose "best" of the remaining to save
                sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(threatenedTargets));
                chance = true;
            }

        } // Protect combatants
        else if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            sa.resetTargets();
            if (sa.canTarget(ai) && ComputerUtilCombat.wouldLoseLife(ai, combat)
                    && (ComputerUtilCombat.lifeInDanger(ai, combat) || sa.isAbility() || sa.isTrigger())
                    && game.getPhaseHandler().getPlayerTurn().isOpponentOf(ai)) {
                sa.getTargets().add(ai);
                chance = true;
            } else {
                // filter AIs battlefield by what I can target
                List<Card> targetables = ai.getCardsIn(ZoneType.Battlefield);
                targetables = CardLists.getValidCards(targetables, tgt.getValidTgts(), ai, hostCard);
                targetables = CardLists.getTargetableCards(targetables, sa);

                if (targetables.isEmpty()) {
                    return false;
                }
                final List<Card> combatants = CardLists.filter(targetables, CardPredicates.Presets.CREATURES);
                CardLists.sortByEvaluateCreature(combatants);

                for (final Card c : combatants) {
                    if (ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat)) {
                        sa.getTargets().add(c);
                        chance = true;
                    }
                }
            }
        }
        if (sa.hasParam("DividedAsYouChoose") && sa.getTargets() != null && !sa.getTargets().getTargets().isEmpty()) {
            tgt.addDividedAllocation(sa.getTargets().getTargets().get(0), AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("Amount"), sa));
        }

        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        boolean chance = false;
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt == null) {
            // If there's no target on the trigger, just say yes.
            chance = true;
        } else {
            chance = preventDamageMandatoryTarget(ai, sa, mandatory);
        }

        return chance;
    }

    /**
     * <p>
     * preventDamageMandatoryTarget.
     * </p>
     * 
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean preventDamageMandatoryTarget(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        sa.resetTargets();
        // filter AIs battlefield by what I can target
        final Game game = ai.getGame();
        List<Card> targetables = game.getCardsIn(ZoneType.Battlefield);
        targetables = CardLists.getValidCards(targetables, tgt.getValidTgts(), ai, sa.getSourceCard());
        final List<Card> compTargetables = CardLists.filterControlledBy(targetables, ai);
        Card target = null;

        if (targetables.isEmpty()) {
            return false;
        }

        if (!mandatory && compTargetables.isEmpty()) {
            return false;
        }

        if (!compTargetables.isEmpty()) {
            final List<Card> combatants = CardLists.filter(compTargetables, CardPredicates.Presets.CREATURES);
            CardLists.sortByEvaluateCreature(combatants);
            if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                Combat combat = game.getCombat();
                for (final Card c : combatants) {
                    if (ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat)) {
                        target = c;
                        break;
                    }
                }
            }
            if (target == null) {
                target = combatants.get(0);
            }
        } else {
            target = ComputerUtilCard.getCheapestPermanentAI(targetables, sa, true);
        }
        sa.getTargets().add(target);
        if (sa.hasParam("DividedAsYouChoose")) {
            tgt.addDividedAllocation(target, AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("Amount"), sa));
        }
        return true;
    }

}