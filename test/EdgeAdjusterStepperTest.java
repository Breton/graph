import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class EdgeAdjusterStepperTest
{
   @Before
   public void setUp() throws Exception
   {
      m_config = new LevelGeneratorConfiguration(1);
   }

   @Test
   public void testStep_Fail() throws Exception
   {
      EdgeAdjusterStepper.SetChildFactory(
            (a, b) -> new TestStepper(false, null));

      Graph g = new Graph();
      INode n1 = g.AddNode("", "", "", 0);
      INode n2 = g.AddNode("", "", "", 0);
      DirectedEdge de = g.Connect(n1, n2, 0, 0, 0);

      StepperController e = new StepperController(g, new EdgeAdjusterStepper(g, de, m_config));

      StepperController.ExpandRet ret;

      do
      {
         ret = e.Step();
      }
      while(!ret.Complete);

      assertEquals(StepperController.ExpandStatus.StepOutFailure, ret.Status);
   }

   @Test
   public void testStep_Succeed() throws Exception
   {
      EdgeAdjusterStepper.SetChildFactory(
            (a, b) -> new TestStepper(true, null));

      Graph g = new Graph();
      INode n1 = g.AddNode("", "", "", 0);
      INode n2 = g.AddNode("", "", "", 0);
      DirectedEdge de = g.Connect(n1, n2, 0, 0, 0);

      StepperController e = new StepperController(g, new EdgeAdjusterStepper(g, de, m_config));

      StepperController.ExpandRet ret;

      do
      {
         ret = e.Step();
      }
      while(!ret.Complete);

      assertEquals(StepperController.ExpandStatus.StepOutSuccess, ret.Status);
      assertEquals(3, g.NumNodes());
      assertFalse(n1.connects(n2));
      assertEquals(0, n1.getInConnections().size());
      assertEquals(1, n1.getOutConnections().size());
      assertEquals(1, n2.getInConnections().size());
      assertEquals(0, n2.getOutConnections().size());

      DirectedEdge n1_out = n1.getOutConnections().stream().findFirst().get();
      DirectedEdge n2_in = n2.getInConnections().stream().findFirst().get();
      assertEquals(n1_out.End, n2_in.Start);
   }

   @Test
   public void testThrowsForIteration()
   {
      // steppers only get called with iterate if they return it, and this one doesn't
      boolean thrown = false;

      try
      {
         // none of these parameters used in this case
         EdgeAdjusterStepper etss = new EdgeAdjusterStepper(null, null, null);

         etss.Step(StepperController.ExpandStatus.Iterate);
      }
      catch(UnsupportedOperationException uoe)
      {
         thrown = true;
      }

      assertTrue(thrown);
   }

   private LevelGeneratorConfiguration m_config;
}