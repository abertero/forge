package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import forge.gui.GuiUtils;

public class AbilityFactory_ZoneAffecting {
	
	//**********************************************************************
	//******************************* DRAW *********************************
	//**********************************************************************
	public static SpellAbility createAbilityDraw(final AbilityFactory AF){
		final SpellAbility abDraw = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 5445572699000471299L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				return drawStackDescription(af, this);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();		
			}
			
			public boolean canPlayAI()
			{
				return drawCanPlayAI(af,this);
			}
			
			@Override
			public void resolve() {
				drawResolve(af, this);
			}
			
		};
		return abDraw;
	}
	
	public static SpellAbility createSpellDraw(final AbilityFactory AF){
		final SpellAbility spDraw = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
				return drawStackDescription(af, this);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				return drawCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				drawResolve(af, this);
			}
			
		};
		return spDraw;
	}
	
	public static SpellAbility createDrawbackDraw(final AbilityFactory AF){
		final SpellAbility dbDraw = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
				return drawStackDescription(af, this);
			}

			@Override
			public void resolve() {
				drawResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return drawTargetAI(af, this);
			}
			
		};
		return dbDraw;
	}
	
	public static String drawStackDescription(AbilityFactory af, SpellAbility sa){
		StringBuilder sb = new StringBuilder();
		
		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");
		
		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		
		for(Player p : tgtPlayers)
			sb.append(p.toString()).append(" ");

        int numCards = 1;
        if (af.getMapParams().containsKey("NumCards"))
        	numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), af.getMapParams().get("NumCards"), sa);
		
		sb.append("draws (").append(numCards).append(")");
		
		if (af.getMapParams().containsKey("NextUpkeep"))
			sb.append(" at the beginning of the next upkeep");
		
		sb.append(".");
		
		Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null){
        	sb.append(abSub.getStackDescription());
        }
		
		return sb.toString();
	}
	
	public static boolean drawCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		Ability_Cost abCost = af.getAbCost();
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				return false;
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
			if (abCost.getDiscardCost()) 	return false;
			
			if (abCost.getSubCounter()) {
				if (abCost.getCounterType().equals(Counters.P1P1)) return false; // Other counters should be used 
			}
			
		}
			
		boolean bFlag = drawTargetAI(af, sa);
		
		if (!bFlag)
			return false;
		
		if (tgt != null){
			ArrayList<Player> players = tgt.getTargetPlayers();
			if (players.size() > 0 && players.get(0).equals(AllZone.HumanPlayer))
				return true;
		}
		
		Random r = new Random();
		boolean randomReturn = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		
		// some other variables here, like handsize vs. maxHandSize

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	randomReturn &= subAb.chkAI_Drawback();
		return randomReturn;
	}
	
    public static boolean drawTargetAI(AbilityFactory af, SpellAbility sa) {
        Target tgt = af.getAbTgt();
        HashMap<String,String> params = af.getMapParams();
        
        int computerHandSize = AllZoneUtil.getCardsInZone(Constant.Zone.Hand, AllZone.ComputerPlayer).size();
        int humanLibrarySize = AllZoneUtil.getCardsInZone(Constant.Zone.Library, AllZone.HumanPlayer).size();
        int computerLibrarySize = AllZoneUtil.getCardsInZone(Constant.Zone.Library, AllZone.ComputerPlayer).size();
        int computerMaxHandSize = AllZone.ComputerPlayer.getMaxHandSize();
        
        // todo: handle deciding what X would be around here for Braingeyser type cards
        int numCards = 1;
        if (params.containsKey("NumCards"))
        	numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
        	
        
        if (tgt != null) {
            // ability is targeted
            tgt.resetTargets();
            
            if (!AllZone.HumanPlayer.cantLose() && numCards >= humanLibrarySize) {
                // Deck the Human? DO IT!
                tgt.addTarget(AllZone.HumanPlayer);
                return true;
            }
            
            if (numCards >= computerLibrarySize) {
                // Don't deck your self
                return false;
            }
            
            if (computerHandSize + numCards > computerMaxHandSize) {
                // Don't draw too many cards and then risk discarding cards at EOT
                return false;
            }
            
            tgt.addTarget(AllZone.ComputerPlayer);
        }
        else {
        	// todo: when non-targeted abilities can make the Human draw, take that into consideration
        	
            // ability is not targeted
            if (numCards >= computerLibrarySize) {
                // Don't deck yourself
                return false;
            }
            
            if (computerHandSize + numCards > computerMaxHandSize) {
                // Don't draw too many cards and then risk discarding cards at EOT
            	if (!(af.getMapParams().containsKey("NextUpkeep") || sa instanceof Ability_Sub))
            		return false;
            }
        }
        return true;
    }// drawTargetAI()
	
	public static void drawResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		
		Card source = sa.getSourceCard();
        int numCards = 1;
        if (params.containsKey("NumCards"))
        	numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
		
		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
		
		boolean optional = params.containsKey("Optional");
		boolean slowDraw = params.containsKey("NextUpkeep");
		
		for(Player p : tgtPlayers)
			if (tgt == null || p.canTarget(af.getHostCard())){
				if (optional){
					if (p.isComputer()){
			            if (numCards >= AllZoneUtil.getPlayerCardsInLibrary(p).size()) {
			                // AI shouldn't itself
			                continue;
			            }
					}
					else{
						StringBuilder sb = new StringBuilder();
						sb.append("Do you want to draw ").append(numCards).append(" cards(s)");
						
						if(slowDraw)
							sb.append(" next upkeep");
							
						sb.append("?");
						
						if(!GameActionUtil.showYesNoDialog(sa.getSourceCard(), sb.toString())) 
							continue;
					}
				}

				if (slowDraw)
					for(int i = 0; i < numCards; i++)
						p.addSlowtripList(source);
				else
					p.drawCards(numCards);		
				
			}

		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
	     	   abSub.resolve();
	        }
	        else{
				String DrawBack = params.get("SubAbility");
				if (af.hasSubAbility())
					 CardFactoryUtil.doDrawBack(DrawBack, 0, source.getController(), source.getController().getOpponent(), tgtPlayers.get(0), source, null, sa);
	        }
		}
	}
	
	//**********************************************************************
	//******************************* MILL *********************************
	//**********************************************************************
	
	public static SpellAbility createAbilityMill(final AbilityFactory AF){
		final SpellAbility abMill = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 5445572699000471299L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return millStackDescription(this, af);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();		
			}
			
			public boolean canPlayAI()
			{
				return millCanPlayAI(af,this);
			}
			
			@Override
			public void resolve() {
				millResolve(af, this);
			}
			
		};
		return abMill;
	}
	
	public static SpellAbility createSpellMill(final AbilityFactory AF){
		final SpellAbility spMill = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return millStackDescription(this, af);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				return millCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				millResolve(af, this);
			}
			
		};
		return spMill;
	}
	
	public static SpellAbility createDrawbackMill(final AbilityFactory AF){
		final SpellAbility dbMill = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return millStackDescription(this, af);
			}
			
			public boolean canPlayAI()
			{
				return millCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				millResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return millTargetAI(af, this);
			}
			
		};
		return dbMill;
	}
	
	public static String millStackDescription(SpellAbility sa, AbilityFactory af){
		// when getStackDesc is called, just build exactly what is happening
		StringBuilder sb = new StringBuilder();
		int numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), af.getMapParams().get("NumCards"), sa);
		
		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		
		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");
		
		for(Player p : tgtPlayers)
			sb.append(p.toString()).append(" ");
		
		String destination = af.getMapParams().get("Destination");
		if (destination == null || destination.equals(Constant.Zone.Graveyard))
			sb.append("mills ");
		else if (destination.equals(Constant.Zone.Exile))
			sb.append("exiles ");
		sb.append(numCards);
		sb.append(" card");
		if(numCards != 1) sb.append("s");
		sb.append(" from his or her library.");
		
		Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null){
        	sb.append(abSub.getStackDescription());
        }
		
		return sb.toString();
	}
	
	public static boolean millCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		if (!ComputerUtil.canPayCost(sa))
			return false;
		

		Card source = sa.getSourceCard();
		Ability_Cost abCost = af.getAbCost();
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				return false;
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
			if (abCost.getDiscardCost()) 	return false;
			
			if (abCost.getSubCounter()) {
				if (abCost.getCounterType().equals(Counters.P1P1)) return false; // Other counters should be used 
			}
			
		}
		
		boolean bFlag = millTargetAI(af, sa);
		if (!bFlag)
			return false;
		
		Random r = new Random();
		boolean randomReturn = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		
		// some other variables here, like deck size, and phase and other fun stuff

		return randomReturn;
	}
	
	public static boolean millTargetAI(AbilityFactory af, SpellAbility sa){
		Target tgt = af.getAbTgt();
		HashMap<String,String> params = af.getMapParams();
		
		if (tgt != null){
			tgt.resetTargets();
			
			int numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
			
			CardList pLibrary = AllZoneUtil.getCardsInZone(Constant.Zone.Library, AllZone.HumanPlayer);
			
			if (pLibrary.size() == 0)	// deck already empty, no need to mill
				return false;
			
			if (numCards >= pLibrary.size()){
				// Can Mill out Human's deck? Do it!
				tgt.addTarget(AllZone.HumanPlayer);
				return true;
			}
			
			// Obscure case when you know what your top card is so you might? want to mill yourself here
			// if (AI wants to mill self)
			// tgt.addTarget(AllZone.ComputerPlayer);
			// else
			tgt.addTarget(AllZone.HumanPlayer);
		}
		return true;
	}
	
	public static void millResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		
		Card source = sa.getSourceCard();

		int numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
		
		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
		
		String destination = params.get("Destination");
		if (destination == null)
			destination = Constant.Zone.Graveyard;
		
		for(Player p : tgtPlayers)
			if (tgt == null || p.canTarget(af.getHostCard()))
				p.mill(numCards, destination);	

		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
	     	   abSub.resolve();
	        }
			else{
				String DrawBack = params.get("SubAbility");
				if (af.hasSubAbility())
					 CardFactoryUtil.doDrawBack(DrawBack, 0, source.getController(), source.getController().getOpponent(), tgtPlayers.get(0), source, null, sa);
			}
		}
	}
	
	//////////////////////
	//
	//Discard stuff
	//
	//////////////////////
	
	//NumCards - the number of cards to be discarded (may be integer or X)
	//Mode	- the mode of discard - should match spDiscard
	//				-Random
	//				-TgtChoose
	//				-RevealYouChoose
	//				-Hand
	//DiscardValid - a ValidCards syntax for acceptable cards to discard
	//UnlessType - a ValidCards expression for "discard x unless you discard a ..."
	//TODO - possibly add an option for EachPlayer$True - Each player discards a card (Rix Maadi, Slivers, 
	//			Delirium Skeins, Strong Arm Tactics, Wheel of Fortune, Wheel of Fate
	
	//Examples:
	//A:SP$Discard | Cost$B | Tgt$TgtP | NumCards$2 | Mode$Random | SpellDescription$<...>
	//A:AB$Discard | Cost$U | ValidTgts$ Opponent | Mode$RevealYouChoose | NumCards$X | SpellDescription$<...>
	
	public static SpellAbility createAbilityDiscard(final AbilityFactory AF) {
		final SpellAbility abDraw = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = 4348585353456736817L;
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription() {
				// when getStackDesc is called, just build exactly what is happening
				return discardStackDescription(af, this);
			}
			
			@Override
			public boolean canPlay() {
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			@Override
			public boolean canPlayAI() {
				discardTargetAI(af);
				return discardCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				discardResolve(af, this);
			}
			
		};
		return abDraw;
	}

	public static SpellAbility createSpellDiscard(final AbilityFactory AF) {
		final SpellAbility spDraw = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = 4348585353456736817L;
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription() {
				// when getStackDesc is called, just build exactly what is happening
				return discardStackDescription(af, this);
			}
			
			@Override
			public boolean canPlay() {
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			@Override
			public boolean canPlayAI() {
				discardTargetAI(af);
				return discardCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				discardResolve(af, this);
			}
			
		};
		return spDraw;
	}
	
	public static SpellAbility createDrawbackDiscard(final AbilityFactory AF) {
		final SpellAbility dbDraw = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
			private static final long serialVersionUID = 4348585353456736817L;
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription() {
				// when getStackDesc is called, just build exactly what is happening
				return discardStackDescription(af, this);
			}
			
			@Override
			public boolean canPlay() {
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			@Override
			public boolean canPlayAI() {
				discardTargetAI(af);
				return discardCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				discardResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				discardTargetAI(af);
				return discardCheckDrawbackAI(af, this);
			}
			
		};
		return dbDraw;
	}
	
	
	private static void discardResolve(final AbilityFactory af, final SpellAbility sa){
		Card source = sa.getSourceCard();
		HashMap<String,String> params = af.getMapParams();

		String mode = params.get("Mode");

		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);

		for(Player p : tgtPlayers)
			if (tgt == null || p.canTarget(af.getHostCard())) {	
				if(mode.equals("Hand")) {
					p.discardHand(sa);
					continue;
				}
				
				int numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
				
				if(mode.equals("Random")) {
					p.discardRandom(numCards, sa);
				}
				
				else if(mode.equals("TgtChoose")) {
					if(params.containsKey("UnlessType")) {
						p.discardUnless(numCards, params.get("UnlessType"), sa);
					}
					else p.discard(numCards, sa, true);
				}

				else if(mode.equals("RevealYouChoose")) {
					// Is Reveal you choose right? I think the wrong player is being used?
					PlayerZone pzH = AllZone.getZone(Constant.Zone.Hand, p);
					if(pzH.size() != 0) {
						CardList dPHand = new CardList(pzH.getCards());
						CardList dPChHand = new CardList(dPHand.toArray());

						if (params.containsKey("DiscardValid")) {	// Restrict card choices
							String[] dValid = params.get("DiscardValid").split(",");
							dPChHand = dPHand.getValidCards(dValid,source.getController(),source);
						}

						if(source.getController().isComputer()){
							//AI
							for(int i = 0; i < numCards; i++) {
								if (dPChHand.size() > 0){
									CardList dChoices = new CardList();
									if(params.containsKey("DiscardValid")) {
										String dValid = params.get("DiscardValid");
										if (dValid.contains("Creature") && !dValid.contains("nonCreature")) {
											Card c = CardFactoryUtil.AI_getBestCreature(dPChHand);
											if (c!=null)
												dChoices.add(CardFactoryUtil.AI_getBestCreature(dPChHand));
										}
									}


									CardListUtil.sortByTextLen(dPChHand);
									dChoices.add(dPChHand.get(0));

									CardListUtil.sortCMC(dPChHand);
									dChoices.add(dPChHand.get(0));

									Card dC = dChoices.get(CardUtil.getRandomIndex(dChoices));
									dPChHand.remove(dC);

									CardList dCs = new CardList();
									dCs.add(dC);
									GuiUtils.getChoiceOptional("Computer has chosen", dCs.toArray());

									AllZone.ComputerPlayer.discard(dC, sa); // is this right?
								}
							}
						}
						else {
							//human
							GuiUtils.getChoiceOptional("Revealed computer hand", dPHand.toArray());

							for(int i = 0; i < numCards; i++) {
								if (dPChHand.size() > 0) {
									Card dC = GuiUtils.getChoice("Choose a card to be discarded", dPChHand.toArray());

									dPChHand.remove(dC);
									AllZone.HumanPlayer.discard(dC, sa); // is this right?
								}
							}
						}
					}
				}
			}

		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
				abSub.resolve();
			}
			else{
				String DrawBack = params.get("SubAbility");
				if (af.hasSubAbility())
					CardFactoryUtil.doDrawBack(DrawBack, 0, source.getController(), source.getController().getOpponent(), source.getController(), source, null, sa);
			}
		}
	}
	
	private static String discardStackDescription(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		String mode = params.get("Mode");
		StringBuilder sb = new StringBuilder();
		
		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
		
		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");
		
		for(Player p : tgtPlayers)
			sb.append(p.toString()).append(" ");
		
		if(mode.equals("RevealYouChoose")) 
			sb.append("reveals his or her hand.").append("  You choose (");
		else 
			sb.append("discards (");
		
		if(mode.equals("Hand"))
			sb.append("Hand");
		else 
			sb.append(AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa));
			
		sb.append(")");
		
		if(mode.equals("RevealYouChoose")) 
			sb.append(" to discard");
		
		if(mode.equals("Random"))
			sb.append(" at random.");
		else 
			sb.append(".");
		
		Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null)
        	sb.append(abSub.getStackDescription());
		
		return sb.toString();
	}
	
	private static boolean discardCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		Ability_Cost abCost = af.getAbCost();
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				return false;
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
			if (abCost.getDiscardCost()) 	return false;
			
			if (abCost.getSubCounter()) {
				if (abCost.getCounterType().equals(Counters.P1P1)) return false; // Other counters should be used 
			}
			
		}
			/*
		///////////////////////////////////////////////////
		//copied from spDiscard
		
		int nCards = Integer.parseInt(params.get("NumCards"));;
    	
    	CardList humanHand = AllZoneUtil.getPlayerHand(AllZone.HumanPlayer);
    	int numHHand = humanHand.size();
    	
    	if (numHHand >= nCards)
    	{
    		if (Tgt)
    			setTargetPlayer(AllZone.HumanPlayer);
    		
    		return true;
    	}
    		
    	return false;
    	
    	//end copied
		
		if (!bFlag)
			return false;
		*/
		ArrayList<Player> players;
		if (tgt != null){
			players = tgt.getTargetPlayers();
			if (players.size() > 0 && players.get(0).equals(AllZone.HumanPlayer))
				return true;
		}
		else{
			// todo: take Each or Self into account differently
			// players = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		}
		
		Random r = new Random();
		boolean randomReturn = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		
		// some other variables here, like handsize vs. maxHandSize

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	randomReturn &= subAb.chkAI_Drawback();
		return randomReturn;
	}
	
	private static boolean discardTargetAI(AbilityFactory af) {
		Target tgt = af.getAbTgt();
		if(tgt!= null) {
			if (AllZoneUtil.getCardsInZone(Constant.Zone.Hand, AllZone.HumanPlayer).size() > 0 &&
					AllZone.HumanPlayer.canTarget(af.getHostCard())){
				tgt.addTarget(AllZone.HumanPlayer);
				return true;
			}
			return false;
		}
		return false;
	}// discardTargetAI()
	
	
	private static boolean discardCheckDrawbackAI(AbilityFactory af, Ability_Sub subAb) {
		// Drawback AI improvements
		// if parent draws cards, make sure cards in hand + cards drawn > 0
		return true;
	}// discardCheckDrawbackAI()
	
	//**********************************************************************
	//******************************* SCRY *********************************
	//**********************************************************************
	
	public static SpellAbility createDrawbackScry(final AbilityFactory AF){
		final SpellAbility dbScry = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = 7763043327497404630L;
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
				return scryStackDescription(af, this);
			}

			@Override
			public void resolve() {
				scryResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return scryTargetAI(af, this);
			}
			
		};
		return dbScry;
	}
	
	private static void scryResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		
		Card source = sa.getSourceCard();
        int num = 1;
        if (params.containsKey("ScryNum"))
        	num = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("ScryNum"), sa);
		
		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		
		for(Player p : tgtPlayers) {
			if (tgt == null || p.canTarget(af.getHostCard())){
				p.scry(num);
			}
		}

		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
	     	   abSub.resolve();
	        }
	        else{
				String DrawBack = params.get("SubAbility");
				if (af.hasSubAbility())
					 CardFactoryUtil.doDrawBack(DrawBack, 0, source.getController(), source.getController().getOpponent(), tgtPlayers.get(0), source, null, sa);
	        }
		}
	}
	
	private static boolean scryTargetAI(AbilityFactory af, SpellAbility sa) {
        Target tgt = af.getAbTgt();
        
        if (tgt != null) {
            // ability is targeted
            tgt.resetTargets();
            
            tgt.addTarget(AllZone.ComputerPlayer);
        }
        
        return true;
    }// scryTargetAI()
	
	public static String scryStackDescription(AbilityFactory af, SpellAbility sa){
		StringBuilder sb = new StringBuilder();
		
		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");
		
		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		
		for(Player p : tgtPlayers)
			sb.append(p.toString()).append(" ");

        int num = 1;
        if (af.getMapParams().containsKey("ScryNum"))
        	num = AbilityFactory.calculateAmount(sa.getSourceCard(), af.getMapParams().get("ScryNum"), sa);
		
		sb.append("scrys (").append(num).append(").");
		
		Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null){
        	sb.append(abSub.getStackDescription());
        }
		
		return sb.toString();
	}
	
	private static boolean scryCanPlayAI(final AbilityFactory af, SpellAbility sa){
		return true;
	}
	
	public static SpellAbility createAbilityScry(final AbilityFactory AF){
		final SpellAbility abScry = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 2631175859655699419L;
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return scryStackDescription(af, this);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();		
			}
			
			public boolean canPlayAI()
			{
				return scryCanPlayAI(af,this);
			}
			
			@Override
			public void resolve() {
				scryResolve(af, this);
			}
			
		};
		return abScry;
	}
	
	public static SpellAbility createSpellScry(final AbilityFactory AF){
		final SpellAbility spScry = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 6273876397392154403L;
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return scryStackDescription(af, this);
			}
			
			public boolean canPlay(){
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				return scryCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				scryResolve(af, this);
			}
			
		};
		return spScry;
	}
}
