package ru.hh.search.core.morphology.fsm;

import static com.google.common.collect.Maps.newHashMap;
import java.util.Map;

class Registry {
  private final Map<State, State> states = newHashMap();

  public State intern(State state) {
    State interned = states.get(state);

    if (interned == null) {
      states.put(state, state);
      return state;
    }

    return interned;
  }

  public void remove(State state) {
    states.remove(state);
  }
}
