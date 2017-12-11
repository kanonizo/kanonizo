package org.kanonizo.test;

import org.junit.Before;
import org.mockito.MockitoAnnotations;

public class MockitoTest {
  @Before
  public void initMockito() {
    MockitoAnnotations.initMocks(this);
  }
}
