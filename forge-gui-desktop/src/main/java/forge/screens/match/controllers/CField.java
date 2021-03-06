/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.screens.match.controllers;

import com.google.common.base.Function;

import forge.LobbyPlayer;
import forge.UiCommand;
import forge.Singletons;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.framework.ICDoc;
import forge.match.MatchConstants;
import forge.match.input.Input;
import forge.match.input.InputPayMana;
import forge.properties.ForgePreferences;
import forge.screens.match.ZoneAction;
import forge.screens.match.views.VField;
import forge.toolbox.MouseTriggerEvent;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

/**
 * Controls Swing components of a player's field instance.
 */
public class CField implements ICDoc {
    // The one who owns cards on this side of table
    private final Player player;
    // Tho one who looks at screen and 'performs actions'
    private final LobbyPlayer viewer;
    private final VField view;
    private boolean initializedAlready = false;

    private final MouseListener madAvatar = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            CPrompt.SINGLETON_INSTANCE.getInputControl().selectPlayer(player, new MouseTriggerEvent(e));
        }
    };

    /**
     * Controls Swing components of a player's field instance.
     * 
     * @param p0 &emsp; {@link forge.game.player.Player}
     * @param v0 &emsp; {@link forge.screens.match.views.VField}
     * @param playerViewer 
     */
    @SuppressWarnings("serial")
    public CField(final Player p0, final VField v0, LobbyPlayer playerViewer) {
        this.player = p0;
        this.viewer = playerViewer;
        this.view = v0;

        ZoneAction handAction = new ZoneAction(player.getZone(ZoneType.Hand), MatchConstants.HUMANHAND) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ( player.getLobbyPlayer() == viewer || ForgePreferences.DEV_MODE || player.hasKeyword("Play with your hand revealed."))
                    super.actionPerformed(e);
            }
        };
        
        ZoneAction libraryAction = new ZoneAction(player.getZone(ZoneType.Library), MatchConstants.HUMANLIBRARY) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ForgePreferences.DEV_MODE)
                    super.actionPerformed(e);
            }
        };
        
        ZoneAction exileAction = new ZoneAction(player.getZone(ZoneType.Exile), MatchConstants.HUMANEXILED);
        ZoneAction graveAction = new ZoneAction(player.getZone(ZoneType.Graveyard), MatchConstants.HUMANGRAVEYARD);
        ZoneAction flashBackAction = new ZoneAction(player.getZone(ZoneType.Graveyard), MatchConstants.HUMANFLASHBACK) {
            @Override
            protected List<Card> getCardsAsIterable() {
                return player.getCardsActivableInExternalZones();
            }

            @Override
            protected void doAction(final Card c) {
                // activate cards only via your own flashback button
                if (player.getLobbyPlayer() != CField.this.viewer) {
                    return;
                }

                CPrompt.SINGLETON_INSTANCE.getInputControl().selectCard(c, null);
                // Temporarily commenting out the below to route, Flashback cards through the InputProxy
                /*
                final Game game = player.getGame();
                // TODO: "can play" check needed!

                // should I check for who owns these cards? Are there any abilities to be played from opponent's graveyard? 
                final SpellAbility ab = player.getController().getAbilityToPlay(c.getAllPossibleAbilities(player, true));
                if ( null != ab) {
                    game.getAction().invoke(new Runnable(){ 
                    	@Override public void run() {
                    		HumanPlay.playSpellAbility(player, ab);
                    		game.getStack().addAllTirggeredAbilitiesToStack();
                    }});
                }
                */
            }
        };

        Function<Byte, Void> manaAction = new Function<Byte, Void>() {
            public Void apply(Byte colorCode) {
                if (CField.this.player.getLobbyPlayer() == CField.this.viewer) {
                    final Input in = Singletons.getControl().getInputQueue().getInput();
                    if (in instanceof InputPayMana) {
                        // Do something
                        ((InputPayMana) in).useManaFromPool(colorCode);
                    }
                }
                return null;
            }
        };
        
        view.getDetailsPanel().setupMouseActions(handAction, libraryAction, exileAction, graveAction, flashBackAction, manaAction);
        
    }

    @Override
    public void initialize() {
        if (initializedAlready) { return; }
        initializedAlready = true;
    
        // Observers
//        CField.this.player.getZone(ZoneType.Hand).addObserver(observerZones);
//        CField.this.player.getZone(ZoneType.Battlefield).addObserver(observerPlay);
//        CField.this.player.addObserver(observerDetails);
    
        // Listeners
        // Player select
        this.view.getAvatarArea().addMouseListener(madAvatar);
    }

    @Override
    public void update() {
    }

    /** @return {@link forge.game.player.Player} */
    public Player getPlayer() {
        return this.player;
    }

    /** @return {@link forge.screens.match.views.VField} */
    public VField getView() {
        return this.view;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }
} // End class CField
