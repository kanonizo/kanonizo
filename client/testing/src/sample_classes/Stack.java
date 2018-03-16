package sample_classes;
public class Stack {
  private int[] elems;
  private int index = -1;

  public Stack(int size) {
    if (size < 1) {
      throw new IllegalArgumentException("Stack cannot have 0 size");
    }
    elems = new int[size];
  }

  public void push(int elem) {
    if (index == elems.length - 1) {
      throw new IllegalStateException("Stack is full!");
    }
    elems[++index] = elem;
  }

  public int pop() {
    if (isEmpty()) {
      throw new IllegalStateException("Stack is empty!");
    }
    return elems[index--];
  }

  public boolean isEmpty() {
    return index == -1;
  }

  public boolean isFull() {
    return index == elems.length - 1;
  }
}
