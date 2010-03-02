package ru.hh.search.core.morphology.fsm;

import it.unimi.dsi.fastutil.chars.CharIterator;
import it.unimi.dsi.fastutil.chars.CharSortedSet;

public class FSMBuilder {
  private final State root = new State();
  private final Registry registry = new Registry();

  public void put(CharSequence word, int annotation) {
    traversePrefix(root, word, 0, annotation, false);
  }

  public FSM build() {
    byte[] charMap = new byte[Character.MAX_VALUE + 1];

    CharSortedSet alphabet = root.apply(new State.AlphabetBuilder()).alphabet;

    byte symbol = 0;
    CharIterator iter = alphabet.iterator();
    while (iter.hasNext())
      charMap[iter.nextChar()] = symbol++;

    int[] buffer = new int[root.apply(new State.OffsetCalculator()).offset];
    root.apply(new State.BinaryWriter(buffer, charMap));

    return new FSM(buffer, charMap);
  }

  public void traversePrefix(State state, CharSequence word, int position, int annotation, boolean postConfluence) {
    int slot = state.transition(word, position);

    if (slot < 0)
      addSuffix(state, word, position, annotation);
    else {
      State child = state.child(slot);

      if (child.isConfluence())
        postConfluence = true;

      if (postConfluence)
        child = child.clone();
      else
        registry.remove(child);

      traversePrefix(child, word, position + 1, annotation, postConfluence);

      state.link(slot, registry.intern(child));
    }
  }

  public void addSuffix(State state, CharSequence word, int position, int annotation) {
    if (position == word.length()) {
      state.annotate(annotation);
      return;
    }

    State child = new State();
    addSuffix(child, word, position + 1, annotation);
    state.link(state.addKey(word.charAt(position)), registry.intern(child));
  }
}
