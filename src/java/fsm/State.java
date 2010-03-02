package ru.hh.search.core.morphology.fsm;

import it.unimi.dsi.fastutil.chars.CharAVLTreeSet;
import it.unimi.dsi.fastutil.chars.CharSortedSet;
import java.util.Arrays;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.copyOf;
import static ru.hh.search.core.morphology.fsm.FSM.stateValue;
import static ru.hh.search.core.morphology.fsm.FSM.transitionValue;
import static ru.hh.search.core.util.collections.ArrayUtils.insert;

class State {
  private static final int[] NO_ANNOTATIONS = new int[0];
  private static final char[] NO_KEYS = new char[0];
  private static final State[] NO_CHILDREN = new State[0];

  private int[] annotations = NO_ANNOTATIONS;
  private char[] keys = NO_KEYS;
  private State[] children = NO_CHILDREN;
  private int incomingLinks = 0;

  private boolean color = false;
  private int offset = 0;
  private int hash = 0;

  interface Visitor {
    void visit(State state);
  }

  static class BinaryWriter implements Visitor {
    int[] buffer;
    byte[] charmap;

    BinaryWriter(int[] buffer, byte[] charmap) {
      this.buffer = buffer;
      this.charmap = charmap;
    }

    public void visit(State state) {
      int offset = state.offset;

      buffer[offset++] = stateValue(state.annotations.length, state.size());
      for (int i = 0; i < state.size(); i++)
        buffer[offset++] = transitionValue(state.children[i].offset, charmap[state.keys[i]]);
      for (int i = 0; i < state.annotations.length; i++)
        buffer[offset++] = state.annotations[i];
    }
  }

  static class OffsetCalculator implements Visitor {
    int offset = 0;

    public void visit(State state) {
      state.offset = offset;
      offset += 1 + state.keys.length + state.annotations.length;
    }
  }

  static class AlphabetBuilder implements Visitor {
    CharSortedSet alphabet = new CharAVLTreeSet();

    public void visit(State state) {
      for (char key : state.keys)
        alphabet.add(key);
    }
  }

  public <T extends Visitor> T apply(T visitor) {
    apply(visitor, color);
    return visitor;
  }

  private void apply(Visitor visitor, boolean color) {
    if (this.color != color)
      return;

    this.color = !this.color;

    visitor.visit(this);

    for (State child : children)
      child.apply(visitor, color);
  }

  public void annotate(int annotation) {
    int n = binarySearch(annotations, annotation);

    if (n >= 0)
      return;

    annotations = insert(annotations, -n - 1, annotation);

    hash = 0;
  }

  public int addKey(char key) {
    int insertionPoint = -binarySearch(keys, key) - 1;

    keys = insert(keys, insertionPoint, key);
    children = insert(children, insertionPoint, null);

    hash = 0;

    return insertionPoint;
  }

  public void link(int slot, State child) {
    State previousChild = children[slot];

    if (previousChild != null)
      previousChild.incomingLinks--;

    children[slot] = child;
    child.incomingLinks++;

    hash = 0;
  }

  public int size() {
    return keys.length;
  }

  public int transition(CharSequence word, int position) {
    return position == word.length() ? -1 : binarySearch(keys, word.charAt(position));
  }

  public State child(int slot) {
    return children[slot];
  }

  public boolean isConfluence() {
    return incomingLinks > 1;
  }

  public State clone() {
    State clone = new State();

    clone.annotations = annotations;
    clone.keys = keys;

    clone.children = copyOf(children, children.length);
    for (State child : clone.children)
      child.incomingLinks++;

    return clone;
  }

  public boolean equals(Object o) {
    if (this == o)
      return true;

    State state = (State) o;

    return Arrays.equals(keys, state.keys) && Arrays.equals(annotations, state.annotations) && Arrays.equals(children, state.children);
  }

  public int hashCode() {
    int hash = this.hash;

    if (hash == 0) {
      hash = 1;
      for (int annotation : annotations)
        hash = hash * 31 + annotation;
      for (State child : children)
        hash = hash * 31 + child.hashCode();
      for (char key : keys)
        hash = hash * 31 + key;
      this.hash = hash;
    }

    return hash;
  }
}
