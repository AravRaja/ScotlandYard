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

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;


/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {
	private final class MyGameState implements GameState {
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;

		private Integer moveNumber;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		
		private Piece CurrentPiece;


		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives,
				final ImmutableSet<Piece> winner,
				final Piece CurrentPiece,
				final Integer moveNumber
		)

		{
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.winner = ImmutableSet.of();
			this.CurrentPiece = CurrentPiece;
			this.moves = getAvailableMoves();

			this.moveNumber = moveNumber;

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
		}

		@Nonnull
		@Override public GameSetup getSetup(){ return setup; }

		@Nonnull
		@Override
		public ImmutableSet<Piece> getPlayers() {
			List<Piece> dp = new ArrayList<Piece>();
			for (Player d : detectives) {
				dp.add(d.piece());
			}
			dp.add(mrX.piece());
            return ImmutableSet.copyOf(dp);
		}

		@Nonnull
		@Override public ImmutableList<LogEntry> getMrXTravelLog(){ return log; }

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			Set<Piece> detectiveWinnersTemp = new HashSet<>();
			List<Integer> detectiveLocations = new ArrayList<>();
			for(Player d: detectives){
				detectiveWinnersTemp.add(d.piece());
				detectiveLocations.add(d.location());
			}
			ImmutableSet<Piece> mrXWinner = ImmutableSet.of(mrX.piece());
			ImmutableSet<Piece> detectiveWinners = ImmutableSet.copyOf(detectiveWinnersTemp);
			System.out.println(detectiveLocations);
			System.out.println(mrX.location());
			if (detectiveLocations.contains(mrX.location())){this.winner= detectiveWinners;}

			if(isMrXTurn()){
				if (log.size() == setup.moves.size()){this.winner= mrXWinner;}
				if (moves.isEmpty()){ this.winner = detectiveWinners;}
			}
			else{

				if (moves.isEmpty()){ this.winner= mrXWinner;}
			}
			System.out.println(winner);
			return winner;
		}

		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

			// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			List<Integer> detectivePositions = new ArrayList<Integer>();
			for (Player d : detectives) {
				//System.out.println(d.location());
				//System.out.println(d.piece());
				detectivePositions.add(d.location());
			}
			Set<SingleMove> moves = new HashSet<SingleMove>();
			boolean validDestination = true;
			for(int destination : setup.graph.adjacentNodes(source)) {
				validDestination = true;

				if (detectivePositions.contains(destination)) {

					validDestination = false;
				}
				// TODO find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the collection of moves to return
				if (validDestination){
					for(Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()) ) {
						if (player.has(t.requiredTicket())) {
							moves.add(new SingleMove(player.piece(),source,t.requiredTicket(),destination));
						}
					//TODO find out if the player has the required tickets
					//  if it does, construct a SingleMove and add it the collection of moves to return
					}
					if(player.has(Ticket.SECRET)) {
						moves.add(new SingleMove(player.piece(),source,Ticket.SECRET,destination));
					}
				// TODO consider the rules of secret moves here
				//  add moves to the destination via a secret ticket if there are any left with the player
				}
			}
			return moves;

			// TODO return the collection of moves
		}
		private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source, Set<SingleMove> mrXSingleMoves){
			Set<DoubleMove> moves = new HashSet<DoubleMove>();
			for(SingleMove firstMove : mrXSingleMoves){
				for(SingleMove secondMove :makeSingleMoves(setup, detectives, player, firstMove.destination)){
					if (!((firstMove.ticket.equals(secondMove.ticket)) && (!player.hasAtLeast(firstMove.ticket, 2)))){
						moves.add(new DoubleMove(player.piece(), firstMove.source(), firstMove.ticket, firstMove.destination, secondMove.ticket, secondMove.destination));
					}
				}
			}

			return moves;
		}
		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {

			Set<Move> moves = new HashSet<Move>();
			if (!winner.isEmpty()) {return ImmutableSet.copyOf(moves);}
			if (!isMrXTurn()){
				for(Player d : detectives){
					if (!remaining.contains(d.piece())) {
						moves.addAll(makeSingleMoves(getSetup(), detectives, d, d.location()));
					}
				}
			}
			else{
				Set<SingleMove> mrXSingleMoves = makeSingleMoves(getSetup(), detectives, mrX, mrX.location());
            	moves.addAll(mrXSingleMoves);
				if (mrX.has(Ticket.DOUBLE) && (getSetup().moves.size() > 1)){
					moves.addAll(makeDoubleMoves(getSetup(), detectives, mrX, mrX.location(), mrXSingleMoves));
					}
				}
			return ImmutableSet.copyOf(moves);
		}



		@Nonnull
		@Override public Optional<Integer> getDetectiveLocation(Detective detective){
			// For all detectives, if Detective#piece == detective, then return the location in an Optional.of();
			// otherwise, return Optional.empty();
			boolean valid = false;
			int loc = 0;
			for (Player d : detectives) {
                if (detective == d.piece()) {
					loc = d.location();
                    valid = true;
                    break;
                }
			}
			if (valid) {
				return Optional.of(loc);
			}
			else {return Optional.empty();}
        }

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			boolean valid = false;
            List<Player> players = new ArrayList<Player>(detectives);
			players.add(mrX);
			Player plr = players.get(0);
			for (Player p : players) {
				if (piece == p.piece()) {
					plr = p;
					valid = true;
					break;
				}
			}
			if (valid) {
				ImmutableMap<Ticket, Integer> finalTickets = plr.tickets();
				TicketBoard tb = new TicketBoard() {

					@Override
					public int getCount(@Nonnull Ticket ticket) {
						return finalTickets.get(ticket);
					}
				};
				return Optional.of(tb);
			}
			else {
				return Optional.empty();
			}
		}

		public boolean isMrXTurn(){
            return remaining.size() == detectives.size();

        }

		public List<ImmutableMap<Ticket, Integer>> setTickets(ImmutableMap<Ticket, Integer> oldTickets, List<Ticket> changedTickets){
			Map<Ticket, Integer> newtP = new HashMap<>(oldTickets);
			Map<Ticket, Integer> newtX = new HashMap<>(mrX.tickets());
			for (Ticket t : changedTickets) {

				newtP.replace(t, newtP.get(t), newtP.get(t) - 1);

				if (CurrentPiece.isDetective()) {
					newtX.replace(t, newtX.get(t), newtX.get(t) + 1);
				}
			}
			return List.of(ImmutableMap.copyOf(newtP),ImmutableMap.copyOf(newtX));
		}

		public Player setCurrentPlayer(){
			Player CurrentPlayer = null;
			if (CurrentPiece.isMrX()){ CurrentPlayer = mrX;}
			else {
				for (Player d : detectives) {
					if (d.piece().equals(CurrentPiece)) {
						CurrentPlayer = d;
					}
				}
			}
			return CurrentPlayer;
		}



		public static class LocationUpdate implements Visitor<Integer>{

			@Override
			public Integer visit(SingleMove move) {

				return move.destination;
			}

			@Override
			public Integer visit(DoubleMove move) {
				return move.destination2;
			}
		}
		public static class TicketUpdate implements Visitor<List<Ticket>>{
			ArrayList<Ticket> tickets = new ArrayList<Ticket>();
			@Override
			public List<Ticket> visit(SingleMove move) {
				tickets.add(move.ticket);
				return tickets;
			}

			@Override
			public List<Ticket> visit(DoubleMove move) {
				tickets.add(move.ticket1);
				tickets.add(move.ticket2);
				tickets.add(Ticket.DOUBLE);
				return tickets;
			}
		}
		public class LogUpdate implements Visitor<ImmutableList<LogEntry>>{
			ArrayList<LogEntry> LogEntries = new ArrayList<LogEntry>();

			@Override
			public ImmutableList<LogEntry> visit(SingleMove move) {
				if (move.commencedBy().isDetective()){return ImmutableList.of();}

				if (setup.moves.get(moveNumber)){LogEntries.add(LogEntry.reveal(move.ticket, move.destination));}
				else{LogEntries.add(LogEntry.hidden(move.ticket));}
				moveNumber += 1;

				return ImmutableList.copyOf(LogEntries);
			}

			@Override
			public ImmutableList<LogEntry> visit(DoubleMove move) {
				if (move.commencedBy().isDetective()){return ImmutableList.of();}
				if (setup.moves.get(moveNumber)){LogEntries.add(LogEntry.reveal(move.ticket1, move.destination1));}
				else{LogEntries.add(LogEntry.hidden(move.ticket1));}
				if (setup.moves.get(moveNumber+1)){LogEntries.add(LogEntry.reveal(move.ticket2, move.destination2));}
				else{LogEntries.add(LogEntry.hidden(move.ticket2));}
				moveNumber += 2;

				return ImmutableList.copyOf(LogEntries);
			}
		}


		@Override public GameState advance(Move move) {
			if (remaining.contains(move.commencedBy())){throw new IllegalArgumentException("Detective has already moved this round");}
			if (remaining.size() != detectives.size() && move.commencedBy().isMrX()){throw new IllegalArgumentException("Not all detectives have moved yet");}
			this.CurrentPiece = move.commencedBy();
			this.moves = getAvailableMoves();
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			Set<Piece> newRemainingTemp = new HashSet<>(Set.of());
			if (remaining.size() != detectives.size()){
				newRemainingTemp.addAll(remaining);
				newRemainingTemp.add(CurrentPiece);
			}
			ImmutableSet<Piece> newRemaining = ImmutableSet.copyOf(newRemainingTemp);





			//check validity of move ^

			Player CurrentPlayer = setCurrentPlayer();

			// gives us current player from piece ^



			TicketUpdate tc = new TicketUpdate();
			List<ImmutableMap<Ticket, Integer>> newTickets = setTickets(CurrentPlayer.tickets(), move.accept(tc));
            ImmutableMap<Ticket, Integer> PlayerTickets = newTickets.get(0);
			ImmutableMap<Ticket, Integer> XTickets = newTickets.get(1);
			LocationUpdate lc = new LocationUpdate();
			Integer location = move.accept(lc);
			LogUpdate lu = new LogUpdate();

			ImmutableList<LogEntry> newLog = ImmutableList.<LogEntry>builder()
					.addAll(log)
					.addAll(move.accept(lu))
					.build();

			Player newPlayer = new Player(CurrentPiece, PlayerTickets, location);
			Player newMrX;
			List<Player> newDetectives = new ArrayList<>(detectives);
			if (newPlayer.isMrX()){
				newMrX = newPlayer;

			}
			else{
				newMrX = new Player(mrX.piece(), XTickets, mrX.location());
				for(Player d:detectives){
					if (d.piece().equals(newPlayer.piece())){
						newDetectives.set(newDetectives.indexOf(d), newPlayer);
					}


					}
				}


            return new MyGameState(setup,newRemaining, newLog, newMrX, newDetectives, winner, CurrentPiece, moveNumber);


			//number of rounds og the game has been left
			//how many moves mrX has made


			//single move
			/* update the position of piece !
			 * give tickets to mrX !
			 * swap turn !
			 * check all,prev detective isn't in available moves !
			 * if no possible detective moves switch to mrX
			 * IF MR X
			 * update travel log
			 * discard tickets used !
			 * if move is reveal
			 *
			 * double move
			 * update travel log  twice
			 * discard three tickets
			 * update positions
			 *
			 * */

			 }
	}
	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		Set<Piece> remainingFull = new  HashSet<Piece>();
		for(Player d: detectives){
			remainingFull.add(d.piece());
		}
		return new MyGameState(setup, ImmutableSet.copyOf(remainingFull), ImmutableList.of(), mrX, detectives, ImmutableSet.of(), mrX.piece(), 0);

	}

}
