package test.com.dpaterson.testing;

import org.junit.Before;
import org.mockito.MockitoAnnotations;

public class MockitoTest {
  @Before
  public void initMockito() {
    MockitoAnnotations.initMocks(this);
  }
}
