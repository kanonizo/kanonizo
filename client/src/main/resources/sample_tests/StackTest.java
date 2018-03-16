package sample_tests;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import sample_classes.Stack;

public class StackTest {
  @Test(expected = IllegalArgumentException.class)
  public void test0StackSize() {
    new Stack(0);
    fail("Expected exception");
  }

  @Test
  public void testEmptyOnCreation() {
    Stack s = new Stack(2);
    assertTrue(s.isEmpty());
  }

  @Test
  public void testNotEmptyAfterInsertion() {
    Stack s = new Stack(2);
    s.push(10);
    assertFalse(s.isEmpty());
  }

  @Test
  public void testNotFullOnCreation() {
    Stack s = new Stack(2);
    assertFalse(s.isFull());
  }

  @Test
  public void testFullAfterPushes() {
    Stack s = new Stack(2);
    s.push(10);
    s.push(5);
    assertTrue(s.isFull());
  }

  @Test
  public void testPushPop() {
    Stack s = new Stack(2);
    s.push(10);
    assertEquals(10, s.pop());
  }

  @Test
  public void testEmptyAfterPop() {
    Stack s = new Stack(2);
    s.push(10);
    s.pop();
    assertTrue(s.isEmpty());
  }

  @Test(expected = IllegalStateException.class)
  public void testPushOnFullStack() {
    Stack s = new Stack(1);
    s.push(10);
    s.push(5);
    fail("Expected exception");
  }

  @Test(expected = IllegalStateException.class)
  public void testPopFromEmptyStack() {
    Stack s = new Stack(1);
    s.pop();
    fail("Expected exception");
  }
}
