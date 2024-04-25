package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.*;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;

import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;


/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {
	private  final class MyGameState implements GameState {
		private  GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;


		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;


		private Mode turn;
		//added a turn variable to track which turn modes the game is in for getWinner and advance purposes





		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives,
				final ImmutableSet<Piece> winner
		)

		{
			this.setup = setup;
			this.remaining = remaining;

			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;



			this.turn = Mode.STANDARD;
			this.winner = getWinner();

			this.moves = getAvailableMoves();





			if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			if(!mrX.isMrX()) throw new IllegalArgumentException("mrX is not the MRX piece");
			for (Player p : detectives) {
				if(!p.isDetective()) throw new IllegalArgumentException("Player in detectives is not a detective");
				if (p.has(Ticket.SECRET) || p.has(Ticket.DOUBLE)) throw new IllegalArgumentException("detective has secret ticket");
				for (Player d : detectives) {
					if (d!=p & d.location() == p.location()) throw new IllegalArgumentException("two detectives on same square");
				}
			}
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("graph is empty");
			//checks the game state is all valid to continue otherwise throws an error ^

		}

		@Nonnull
		@Override public GameSetup getSetup(){ return setup; }

		@Nonnull
		@Override
		public ImmutableSet<Piece> getPlayers() {
			//adds all detectives playing and mrX to a new List and returns it

			List<Piece> dp = new ArrayList<>();
			for (Player d : detectives) {
				dp.add(d.piece());
			}
			dp.add(mrX.piece());

			return ImmutableSet.copyOf(dp);
		}

		@Nonnull
		@Override public ImmutableList<LogEntry> getMrXTravelLog(){ return log; }

		public boolean isMrXTurn(){
			return (remaining.size() == detectives.size() || turn == Mode.MRX);
			//if all the detectives are in remaining then none of them can move so has to be mrX turn
			//or if the turn mode is on mrX's turn

		}

		public Mode getTurn(){

			if(getAvailableMoves().isEmpty() && winner.isEmpty()){
				return Mode.MRX;
			}
			else{
				return Mode.STANDARD;
			}
			//if it's the detectives turn, but they have no moves and the games isn't over swap to mrX's turn
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			this.winner = ImmutableSet.of();
			Set<Piece> detectiveWinnersTemp = new HashSet<>();
			List<Integer> detectiveLocations = new ArrayList<>();
			for(Player d: detectives){
				detectiveWinnersTemp.add(d.piece());
				detectiveLocations.add(d.location());
			}


			ImmutableSet<Piece> mrXWinner = ImmutableSet.of(mrX.piece());
			ImmutableSet<Piece> detectiveWinners = ImmutableSet.copyOf(detectiveWinnersTemp);
			//makes the two winning sets: either all the detectives playing in a set or mrX in a set

			turn = Mode.ALLDETECTIVES;
			ImmutableSet<Move> detectiveAvailableMoves  = getAvailableMoves();
			turn = Mode.MRX;
			ImmutableSet<Move> mrXAvailableMoves  = getAvailableMoves();
			turn = Mode.STANDARD;

			//gives us different sets of available moves for different players for use to test for a winner


			if (detectiveLocations.contains(mrX.location())){
				this.winner= detectiveWinners;
				return winner;
			}
			//if mrX and detective are on same square detective wins ^

			if(isMrXTurn()){
				if (log.size() == setup.moves.size() ){
					this.winner= mrXWinner;
					return winner;
				}
				//if its mrX's turn, and he's reached the set amount of moves he wins!

				if (mrXAvailableMoves.isEmpty()){
					this.winner = detectiveWinners;
					return winner;
				}
				//if its mrX's move, and he doesn't have any moves detectives win

				if(detectiveAvailableMoves.isEmpty()){
					this.winner = mrXWinner;
					return winner;
				}
				//otherwise if the detectives don't have any moves mrX wins

			}
			else{
				if (mrXAvailableMoves.isEmpty() && getAvailableMoves().isEmpty()){
					this.winner = detectiveWinners;
					return winner;
				}
				//if it's the detectives turn, and they have no moves and mrX has no moves then they win
			}
			this.turn = getTurn();
			if(isMrXTurn()){
				if (log.size() == setup.moves.size() ){
					this.winner= mrXWinner;
					return winner;
				}}
			return winner;
		}

		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){


			List<Integer> detectivePositions = new ArrayList<>();
			for (Player d : detectives) {
				detectivePositions.add(d.location());
			}

			Set<SingleMove> moves = new HashSet<>();

			for(int destination : setup.graph.adjacentNodes(source)) {
				//loops through adjacent nodes to the chosen detective source
				if (!detectivePositions.contains(destination)){
					//checks that none of the other detectives are on any of the adjacent nodes

					for(Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()) ) {
						if (player.has(t.requiredTicket())) {
							moves.add(new SingleMove(player.piece(),source,t.requiredTicket(),destination));
							//looks at the transport needed to get to these adjacent nodes and checks player tickets
							//if the has the required ticket then add a possible single move to move
						}
					}
					if(player.has(Ticket.SECRET)) {
						moves.add(new SingleMove(player.piece(),source,Ticket.SECRET,destination));
						//if a player has a secret ticket add this single move as well as secret tickets can move via all transport
					}

				}
			}
			return moves;
		}
		private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, Set<SingleMove> mrXSingleMoves){
			Set<DoubleMove> moves = new HashSet<>();

			for(SingleMove firstMove : mrXSingleMoves){

				for(SingleMove secondMove :makeSingleMoves(setup, detectives, player, firstMove.destination)){

					if (!((firstMove.ticket.equals(secondMove.ticket)) && (!player.hasAtLeast(firstMove.ticket, 2)))){

						moves.add(new DoubleMove(player.piece(), firstMove.source(), firstMove.ticket, firstMove.destination, secondMove.ticket, secondMove.destination));
						//loops through all the single moves and makes an associated set of single moves for each possible location
						//if mrX has the required tickets for both the moves then it adds the Double Move

					}
				}
			}

			return moves;
		}
		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {

			Set<Move> moves = new HashSet<>();
			//makes an empty hashSet of moves that will be filled ^


			if (!winner.isEmpty()) {
				return ImmutableSet.copyOf(moves);}
			//if a winner has been chosen there can't be any moves available thus returning an empty set ^

			if (!isMrXTurn() && turn == Mode.STANDARD){
				//checks if it's the detectives turn ^
				for(Player d : detectives){
					if (!remaining.contains(d.piece())) {
						moves.addAll(makeSingleMoves(getSetup(), detectives, d, d.location()));
						//loops through the detectives
						//and if the detective has not moved already (if its in remaining)
						// it adds all that specific detectives moves

					}
				}
			}
			else if ( turn == Mode.ALLDETECTIVES){

				for(Player d : detectives){
					moves.addAll(makeSingleMoves(getSetup(), detectives, d, d.location()));
					//as the mode is ALLDETECTIVES shows all the available detectives moves
					// regardless if they've moved already or if its mrX's turn
				}

			}
			else{
				Set<SingleMove> mrXSingleMoves = makeSingleMoves(getSetup(), detectives, mrX, mrX.location());
				moves.addAll(mrXSingleMoves);
				//else it mrX's turn so adds all his moves to moves instead of any detectives moves


				if (mrX.has(Ticket.DOUBLE) && (getSetup().moves.size() > 1)){
					moves.addAll(makeDoubleMoves(getSetup(), detectives, mrX, mrXSingleMoves));
				}
				// adds mrX's double moves if he has a double move ticket
			}
			return ImmutableSet.copyOf(moves);
		}



		@Nonnull
		@Override public Optional<Integer> getDetectiveLocation(Detective detective){
			//functions loops through and find associated detective and returns its location
			for (Player d : detectives) {
				if (detective == d.piece()) {
					return Optional.of(d.location());

				}
			}
			return Optional.empty();
		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			//function returns a ticketBoard of the piece with the method getCount
			List<Player> players = new ArrayList<>(detectives);
			players.add(mrX);

			for (Player p : players) {
				if (piece == p.piece()) {
					return Optional.of(ticket -> p.tickets().get(ticket));
				}
			}
			return Optional.empty();
		}



		public List<ImmutableMap<Ticket, Integer>> getTickets(ImmutableMap<Ticket, Integer> oldTickets, List<Ticket> usedTickets){
			//function takes a players old tickets and the tickets that were used ands returns a new map of the players changed tickets
			// as well as mrX's changed tickets if it's a detectives turn

			Map<Ticket, Integer> newtP = new HashMap<>(oldTickets);
			Map<Ticket, Integer> newtX = new HashMap<>(mrX.tickets());
			for (Ticket t : usedTickets) {

				newtP.replace(t, newtP.get(t), newtP.get(t) - 1);

				if (!isMrXTurn()) {
					newtX.replace(t, newtX.get(t), newtX.get(t) + 1);
				}
			}
			return List.of(ImmutableMap.copyOf(newtP),ImmutableMap.copyOf(newtX));
		}

		public Player setCurrentPlayer(Piece currentPiece){
			//function takes a piece and returns the associated player

			if (currentPiece.isMrX()){ return mrX;}

			else {
				for (Player d : detectives) {
					if (d.piece().equals(currentPiece)) {
						return d;
					}
				}
			}

			throw new RuntimeException("no player seems attached to this current piece");
		}



		public static class LocationUpdate implements Visitor<Integer>{

			@Override
			public Integer visit(SingleMove move) {
				//returns final location after single move
				return move.destination;
			}
			@Override
			public Integer visit(DoubleMove move) {
				//returns final location after double move
				return move.destination2;
			}
		}


		public static class TicketUpdate implements Visitor<List<Ticket>>{
			ArrayList<Ticket> tickets = new ArrayList<>();
			@Override
			public List<Ticket> visit(SingleMove move) {
				tickets.add(move.ticket);
				//returns all the tickets used in a single move
				return tickets;
			}

			@Override
			public List<Ticket> visit(DoubleMove move) {
				tickets.add(move.ticket1);
				tickets.add(move.ticket2);
				tickets.add(Ticket.DOUBLE);
				//returns all the tickets used in a double move
				return tickets;
			}
		}
		public class LogUpdate implements Visitor<ImmutableList<LogEntry>>{
			ArrayList<LogEntry> LogEntries = new ArrayList<>();
			Integer moveNumber = log.size();

			@Override
			public ImmutableList<LogEntry> visit(SingleMove move) {
				if (move.commencedBy().isDetective()){return ImmutableList.of();}
				// no update to log if it's a detectives move ^


				if (setup.moves.get(moveNumber)){LogEntries.add(LogEntry.reveal(move.ticket, move.destination));}
				else{LogEntries.add(LogEntry.hidden(move.ticket));}
				//adds the single move to the log entries checking if mrX is on a reveal move.




				return ImmutableList.copyOf(LogEntries);
			}

			@Override
			public ImmutableList<LogEntry> visit(DoubleMove move) {

				if (move.commencedBy().isDetective()){return ImmutableList.of();}
				// no update to log if it's a detectives move ^
				if (setup.moves.get(moveNumber)){LogEntries.add(LogEntry.reveal(move.ticket1, move.destination1));}
				else{LogEntries.add(LogEntry.hidden(move.ticket1));}
				//adds the first move of the double move to the log

				if (setup.moves.get(moveNumber+1)){LogEntries.add(LogEntry.reveal(move.ticket2, move.destination2));}
				else{LogEntries.add(LogEntry.hidden(move.ticket2));}
				//adds the second move of a double move to the log


				return ImmutableList.copyOf(LogEntries);
			}
		}


		@Nonnull
		@Override public GameState advance(Move move) {

			if (remaining.contains(move.commencedBy())){throw new IllegalArgumentException("Detective has already moved this round");}
			if (!isMrXTurn() && move.commencedBy().isMrX()){throw new IllegalArgumentException("Not all detectives have moved yet");}
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
			//check validity of move ^

			Set<Piece> newRemainingTemp = new HashSet<>(Set.of());
			if (!isMrXTurn()){
				newRemainingTemp.addAll(remaining);
				newRemainingTemp.add(move.commencedBy());
			}
			ImmutableSet<Piece> newRemaining = ImmutableSet.copyOf(newRemainingTemp);

			// updates the new remaining adding the detective or resetting if MrX is moving^

			Piece currentPiece = move.commencedBy();
			Player currentPlayer = setCurrentPlayer(currentPiece);
			//makes a current player and piece for readability and simplicity

			TicketUpdate tc = new TicketUpdate();
			List<ImmutableMap<Ticket, Integer>> newTickets = getTickets(currentPlayer.tickets(), move.accept(tc));
			ImmutableMap<Ticket, Integer> playerTickets = newTickets.get(0);
			ImmutableMap<Ticket, Integer> xTickets = newTickets.get(1);
			//updates the advance players tickets and new mrX tickets for the new game state ^

			LocationUpdate lc = new LocationUpdate();
			Integer location = move.accept(lc);

			//gets the new location of the player after its move for the new game state



			LogUpdate lu = new LogUpdate();
			List<LogEntry> logUpdate= move.accept(lu);
			ImmutableList<LogEntry> newLog = ImmutableList.<LogEntry>builder()
					.addAll(log)
					.addAll(logUpdate)
					.build();
			//updates mrX's travel log ^




			Player newPlayer = new Player(currentPiece, playerTickets, location);
			Player newMrX;
			//initializes the newPlayer's attributes that jus moved as well as new mrX


			List<Player> newDetectives = new ArrayList<>(detectives);

			if (newPlayer.isMrX()){
				newMrX = newPlayer;
			}
			else{
				newMrX = new Player(mrX.piece(), xTickets, mrX.location());

				for(Player d:detectives){

					if (d.piece().equals(newPlayer.piece())){
						newDetectives.set(newDetectives.indexOf(d), newPlayer);
					}
				}
			}
			//updates the detectives list if a detective has moved with the new player
			//otherwise only updates newMrX and keep detectives the same


			return new MyGameState(setup, newRemaining, newLog, newMrX, newDetectives, winner);
		}
	}
	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {

		Set<Piece> remainingFull = new HashSet<>();
		for(Player d: detectives){
			remainingFull.add(d.piece());
		}

		return new MyGameState(setup, ImmutableSet.copyOf(remainingFull), ImmutableList.of(), mrX, detectives, ImmutableSet.of());

	}

}
