package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.*;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import javax.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;


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

		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives)
		{
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
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
			return null;
		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			return null;
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
			ImmutableMap<Ticket, Integer> tickets;
			for (Player d : detectives) {
				if (piece == d.piece()) {
					tickets = d.tickets();
					valid = true;
					break;
				}
			}
			if (valid) {
				return Optional.empty();
			}
			else {return Optional.empty();}
		}


		@Override public GameState advance(Move move) {  return null;  }
	}
	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

}
