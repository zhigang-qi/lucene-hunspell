package ru.hh.search.core.morphology.fsm;

import static com.google.common.collect.Lists.newArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import static java.util.Arrays.copyOfRange;
import java.util.List;
import ru.hh.search.core.util.collections.Pair;

public class FSM {
  public static FSMBuilder builder() {
    return new FSMBuilder();
  }

  public static FSM load(InputStream stream) throws IOException {
    try {
      ObjectInputStream ois = new ObjectInputStream(stream);
      int[] table = (int[]) ois.readObject();
      byte[] charMap = (byte[]) ois.readObject();
      FSM fsm = new FSM(table, charMap);
      ois.close();
      return fsm;
    } catch (ClassNotFoundException e) {
      throw new IOException(e);
    }
  }

  public void save(OutputStream stream) throws IOException {
    ObjectOutputStream oos = new ObjectOutputStream(stream);
    oos.writeObject(table);
    oos.writeObject(charMap);
    oos.close();
  }

  private final int[] table;
  private final byte[] charMap;

  FSM(int[] table, byte[] charMap) {
    this.table = table;
    this.charMap = charMap;
  }

  public int sizeInKb() {
    return table.length * 4 / 1024;
  }

  public Pair<Integer, int[]> lookup(CharSequence word) {
    int matchLength = 0;
    int matchState = 0;

    int state = 0;

    for (int i = 0; i < word.length(); i++) {
      int transition = findTransition(state, word.charAt(i));

      if (transition < 0)
        break;

      state = transitionState(transition);
      if (annotationCount(state) > 0) {
        matchState = state;
        matchLength = i + 1;
      }
    }

    int from = matchState + transitionCount(matchState) + 1;
    int to = from + annotationCount(matchState);

    return Pair.of(matchLength, copyOfRange(table, from, to));
  }

  public List<Integer> lookup2(CharSequence word) {
    List<Integer> lengths = newArrayList();
    int state = 0;

    for (int i = 0; i < word.length(); i++) {
      int transition = findTransition(state, word.charAt(i));

      if (transition < 0)
        break;

      state = transitionState(transition);
      if (annotationCount(state) > 0)
        lengths.add(i + 1);
    }

    return lengths;
  }

  private int findTransition(int offset, char c) {
    int key = charMap[c];
    int low = offset + 1;
    int high = low + transitionCount(offset) - 1;

    while (low <= high) {
      int mid = (low + high) >>> 1;
      int midVal = transitionKey(mid);

      if (midVal < key)
        low = mid + 1;
      else if (midVal > key)
        high = mid - 1;
      else
        return mid; // key found
    }
    return -(low + 1);  // key not found.
  }

  private int annotationCount(int offset) {
    return (table[offset] & 0xFFFF0000) >>> 16;
  }

  private int transitionCount(int offset) {
    return table[offset] & 0x0000FFFF;
  }

  private int transitionState(int offset) {
    return (table[offset] & 0xFFFFFF00) >>> 8;
  }

  private int transitionKey(int offset) {
    return table[offset] & 0x000000FF;
  }

  static int stateValue(int annotationCount, int transitionCount) {
    return annotationCount << 16 | transitionCount;
  }

  static int transitionValue(int state, int key) {
    return state << 8 | key;
  }
  /*
   binary form:
     state*

   state:
     annotationCount 16bit, transitionCount 16bit
     transition*
     annotation*

   transition:
     state offset 24bit, coded char 8bit

   annotation:
     32bit
  */
}
