package ru.hh.search.core.morphology;

public final class ReverseSequence implements CharSequence {
  public static CharSequence reverse(CharSequence sequence) {
    return new ReverseSequence(sequence);
  }

  private final CharSequence sequence;

  public ReverseSequence(CharSequence sequence) {
    this.sequence = sequence;
  }

  public int length() {
    return sequence.length();
  }

  public char charAt(int index) {
    return sequence.charAt(position(index));
  }

  public CharSequence subSequence(int start, int end) {
    return sequence.subSequence(position(start), position(end));
  }

  private int position(int pos) {
    return sequence.length() - pos - 1;
  }
}
