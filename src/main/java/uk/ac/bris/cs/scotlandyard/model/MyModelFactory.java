package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.ArrayList;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {
	private final class MyModel implements Model {
		private Board.GameState state;
		private ArrayList<Observer> observers;

        private MyModel(final Board.GameState state) {
			this.state = state;
            this.observers = new ArrayList<>();
        }

        @Nonnull
		@Override
		public Board getCurrentBoard() {
			return this.state;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {

			if(observer == null){throw new NullPointerException("An observer cannot be null");}
			if (observers.contains(observer)) { throw new IllegalArgumentException("cannot register same observer more than once"); }
			this.observers.add(observer);
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			if(observer == null){throw new NullPointerException("An observer cannot be null");};
			if (!observers.contains(observer)) { throw new IllegalArgumentException("cannot unregister observer that is not registered"); }
			this.observers.remove(observer);
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(this.observers);
		}

		@Override
		public void chooseMove(@Nonnull Move move) {
			this.state = state.advance(move);
			if (state.getWinner().isEmpty()) {
				for (Observer obs:observers){
					obs.onModelChanged(getCurrentBoard(), Observer.Event.MOVE_MADE);
				}
				//inform observers of new state and EVENT.MOVE_MADE
			}
			else {
				for (Observer obs:observers){
					obs.onModelChanged(getCurrentBoard(), Observer.Event.GAME_OVER);
				}
				//inform observers of new state and EVENT.GAME_OVER
			}

		}
	}




	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		Board.GameState gs = new MyGameStateFactory().build(setup,mrX,detectives);
        return new MyModel(gs);
    }
}
