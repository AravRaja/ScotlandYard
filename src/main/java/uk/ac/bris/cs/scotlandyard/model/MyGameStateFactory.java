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
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		private Piece CurrentPiece;

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
			this.winner = ImmutableSet.of();
			this.CurrentPiece = mrX.piece();
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
			return winner;
		}

		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

			// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			List<Integer> detectivePositions = new ArrayList<Integer>();
			for (Player d : detectives) {
				//System.out.println(d.piece());
				detectivePositions.add(d.location());
			}
			Set<SingleMove> moves = new HashSet<SingleMove>();
			boolean validDestination = true;
			for(int destination : setup.graph.adjacentNodes(source)) {
				validDestination = true;
				//System.out.println(destination+ "hey");
				//System.out.println(detectivePositions);
				if (detectivePositions.contains(destination)) {
					//System.out.println(destination+ "hi");
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

			if (!this.CurrentPiece.equals(mrX.piece())){
				for(Player d : detectives){
					moves.addAll(makeSingleMoves(getSetup(), detectives, d, d.location()));
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

		public List<ImmutableMap<ScotlandYard.Ticket, Integer>> setTickets(ImmutableMap<ScotlandYard.Ticket, Integer> oldTickets, List<Ticket> changedTickets){
			Map<ScotlandYard.Ticket, Integer> newtP = oldTickets;
			Map<ScotlandYard.Ticket, Integer> newtX = mrX.tickets();
			for (Ticket t : changedTickets) {
				newtP.replace(t, newtP.get(t), newtP.get(t) - 1);
				if (CurrentPiece.isDetective()) {
					newtX.replace(t, newtX.get(t), newtX.get(t) + 1);
				}
			}
			return List.of(ImmutableMap.copyOf(newtP),ImmutableMap.copyOf(newtX));
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


		@Override public GameState advance(Move move) {
			this.CurrentPiece = move.commencedBy();
			this.moves = getAvailableMoves();
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			//check validity of move ^

			Player CurrentPlayer = null;

			for(Player d: detectives){
				if (d.piece().equals(CurrentPiece)){CurrentPlayer = d;}
			}
			if (mrX.piece().equals(CurrentPiece)){ CurrentPlayer = mrX;}
			// gives us current player from piece ^


            assert CurrentPlayer != null;
            Map<Ticket, Integer> tickets = CurrentPlayer.tickets();
			Integer location = CurrentPlayer.location();

			LocationUpdate lc = new LocationUpdate();
			location = move.accept(lc);


			if (CurrentPiece.equals(mrX.piece())) {

			}

			//number of rounds og the game has been left
			//how many moves mrX has made


			//single move
			/* update the position of piece
			 * give tickets to mrX
			 * swap turn
			 * check prev detective isnt in available moves
			 * if no possible detective moves switch to mrX
			 * IF MR X
			 * update travel log
			 * discard tickets used
			 * if move is reveal
			 *
			 * double move
			 * update travel log  twice
			 * discard three tickets
			 * update positions
			 *
			 * */

			return null;  }
	}
	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(MRX), ImmutableList.of(), mrX, detectives, ImmutableSet.of());
	}

}
