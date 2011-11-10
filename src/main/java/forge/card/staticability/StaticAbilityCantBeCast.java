package forge.card.staticability;

import java.util.HashMap;

import forge.Card;
import forge.Phase;
import forge.Player;
import forge.card.spellability.AbilityMana;
import forge.card.spellability.SpellAbility;

/**
 * The Class StaticAbility_CantBeCast.
 */
public class StaticAbilityCantBeCast {

    /**
     * TODO Write javadoc for this method.
     * 
     * @param stAb
     *            a StaticAbility
     * @param card
     *            the card
     * @param activator
     *            the activator
     * @return true, if successful
     */
    public static boolean applyCantBeCastAbility(final StaticAbility stAb, final Card card, final Player activator) {
        final HashMap<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();

        if (params.containsKey("ValidCard")
                && !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
            return false;
        }

        if (params.containsKey("Caster") && (activator != null)
                && !activator.isValid(params.get("Caster"), hostCard.getController(), hostCard)) {
            return false;
        }

        if (params.containsKey("OnlySorcerySpeed") && (activator != null) && Phase.canCastSorcery(activator)) {
            return false;
        }

        return true;
    }

    /**
     * TODO Write javadoc for this method.
     * 
     * @param stAb
     *            a StaticAbility
     * @param card
     *            the card
     * @param activator
     *            the activator
     * @return true, if successful
     */
    public static boolean applyCantBeActivatedAbility(final StaticAbility stAb, final Card card, SpellAbility sa) {
        final HashMap<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();
        final Player activator = sa.getActivatingPlayer();

        if (params.containsKey("ValidCard")
                && !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
            return false;
        }

        if (params.containsKey("Activator") && (activator != null)
                && !activator.isValid(params.get("Activator"), hostCard.getController(), hostCard)) {
            return false;
        }
        
        if (params.containsKey("NonMana") && sa instanceof AbilityMana) {
            return false;
        }

        return true;
    }

}
