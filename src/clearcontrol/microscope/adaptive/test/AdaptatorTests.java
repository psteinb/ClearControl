package clearcontrol.microscope.adaptive.test;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import clearcontrol.core.concurrent.thread.ThreadUtils;
import clearcontrol.microscope.adaptive.AdaptiveEngine;

/**
 * AutoPilot tests
 *
 * @author royer
 */
public class AdaptatorTests
{

  /**
   * tests
   */
  @Test
  public void test()
  {
    TestState lTestState = new TestState("initial state");
    AdaptiveEngine<TestState> lAdaptator =
                                         new AdaptiveEngine<TestState>(null,
                                                                       lTestState);


    AdaptationTestModule lAdaptationTests =
                                          new AdaptationTestModule("A",
                                                                   10);

    lAdaptator.add(lAdaptationTests);

    assertEquals(0, lAdaptator.estimateNextStepInSeconds(), 0.001);

    while (lAdaptator.step())
    {
      double lEstimatedTimeInSeconds =
                                     lAdaptator.estimateNextStepInSeconds();
      System.out.format("step: estimated-time=%gs \n",
                        lEstimatedTimeInSeconds);
      ThreadUtils.sleep(1, TimeUnit.MILLISECONDS);
    }

  }

}
