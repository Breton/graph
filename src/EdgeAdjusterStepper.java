class EdgeAdjusterStepper implements IExpandStepper
{
   public interface IChildFactory
   {
      IExpandStepper MakeChild(Graph g, double max_move, double force_target, double move_target);
   }

   EdgeAdjusterStepper(Graph graph, DirectedEdge edge)
   {
      m_graph = graph;
      m_edge = edge;
   }

   @Override
   public Expander.ExpandRetInner Step(Expander.ExpandStatus status)
   {
      switch (status)
      {
         case StepIn:
            SplitEdge();

            IExpandStepper child = m_child_factory.MakeChild(m_graph, 1.0, 0.001, 0.01);

            return new Expander.ExpandRetInner(Expander.ExpandStatus.StepIn,
                  child, "Relaxing split edge.");

         case StepOutSuccess:
            return new Expander.ExpandRetInner(Expander.ExpandStatus.StepOutSuccess,
                  null, "Successfully relaxed split edge.");

         case StepOutFailure:
            return new Expander.ExpandRetInner(Expander.ExpandStatus.StepOutFailure,
                  null, "Failed to relax split edge.");
      }

      // shouldn't get here, crash horribly

      throw new UnsupportedOperationException();
   }

   private void SplitEdge()
   {
      INode c = m_graph.AddNode("c", "", "EdgeExtend", m_edge.Width);

      XY mid = m_edge.Start.GetPos().Plus(m_edge.End.GetPos()).Divide(2);

      c.SetPos(mid);

      m_graph.Disconnect(m_edge.Start, m_edge.End);
      // idea of lengths is to force no more length but allow
      // a longer corridor if required
      m_graph.Connect(m_edge.Start, c, m_edge.MinLength / 2, m_edge.MaxLength, m_edge.Width);
      m_graph.Connect(c, m_edge.End, m_edge.MinLength / 2, m_edge.MaxLength, m_edge.Width);
   }

   static void SetChildFactory(IChildFactory factory)
   {
      m_child_factory = factory;
   }

   private final Graph m_graph;
   private final DirectedEdge m_edge;

   private static IChildFactory m_child_factory;
}
