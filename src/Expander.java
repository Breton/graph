import java.util.Stack;

public class Expander
{
   enum ExpandStatus
   {
      Iterate,          // current stepper requires more steps
      StepIn,           // move down into a child stepper
      StepOutSuccess,   // current stepper successfully completed, move back to parent
      StepOutFailure    // current stepper failed, revert graph and move back to parent
   }

   static class ExpandRetInner
   {
      final ExpandStatus Status;
      final IExpandStepper ChildStepper;
      final String Log;

      ExpandRetInner(ExpandStatus status,
                IExpandStepper childStepper,
                String log)
      {
         Status = status;
         ChildStepper = childStepper;
         Log = log;
      }
   }

   static class ExpandRet
   {
      final ExpandStatus Status;
      final String Log;
      final boolean Complete;

      ExpandRet(ExpandRetInner eri,
                boolean complete)
      {
         Status = eri.Status;
         Log = eri.Log;

         Complete = complete;
      }
   }

   Expander(Graph graph, IExpandStepper initial_stepper)
   {
      m_graph = graph;
      PushStepper(initial_stepper);
      // we start with a (conceptual) step in from the invoking code
      m_last_step_status = ExpandStatus.StepIn;
   }

   ExpandRet Step()
   {
      IExpandStepper stepper = CurrentStepper();

      if (stepper == null)
         throw new NullPointerException("Attempt to step without an initial stepper.  Either you failed to supply one, or this Expander has completed.");

      ExpandRetInner eri = stepper.Step(m_last_step_status);

      m_last_step_status = eri.Status;

      switch (m_last_step_status)
      {
         case StepIn:
            PushStepper(eri.ChildStepper);
            break;

         case StepOutFailure:
            PopStepper(false);
            break;

         case StepOutSuccess:
            PopStepper(true);
            break;
      }

      return new ExpandRet(eri, CurrentStepper() == null);
   }

   private void PushStepper(IExpandStepper stepper)
   {
      m_stack.push(
            new OrderedPair<>(stepper, m_graph != null ? m_graph.CreateRestorePoint() : null));
   }

   private IExpandStepper CurrentStepper()
   {
      if (m_stack.empty())
         return null;

      return m_stack.peek().First;
   }

   private void PopStepper(boolean success)
   {
      IGraphRestore igr = m_stack.pop().Second;

      if (!success && igr != null)
      {
         igr.Restore();
      }
   }

   private Stack<OrderedPair<IExpandStepper, IGraphRestore>>
         m_stack = new Stack<>();
   private Graph m_graph;
   private ExpandStatus m_last_step_status;
}
