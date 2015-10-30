import java.util.*;

class Template
{
   enum NodeType
   {
      In,
      Out,
      Internal,
      Target
   }

   Template(TemplateBuilder builder)
   {
      m_name = builder.GetName();
      m_codes = builder.GetCodes();

      m_nodes = builder.GetUnmodifiableNodes();
      m_connections = builder.GetUnmodifiableConnections();

      m_num_in_nodes = builder.GetNumInNodes();
      m_num_out_nodes = builder.GetNumOutNodes();
      m_num_internal_nodes = builder.GetNumInternalNodes();

      // cannot use this again
      builder.Clear();
   }

   public static String MakeConnectionName(String from, String to)
   {
      return from + "->" + to;
   }

   private NodeRecord FindNodeRecord(String name)
   {
      return m_nodes.get(name);
   }

   int NodesAdded()
   {
      // we add a node for each internal node but we
      // remove the one we are replacing
      return m_num_internal_nodes - 1;
   }

   boolean Expand(Graph graph, INode target, Random random)
   {
      Collection<DirectedEdge> target_in_connections = target.GetInConnections();
      Collection<DirectedEdge> target_out_connections = target.GetOutConnections();

      if (m_num_in_nodes != target_in_connections.size())
      {
         return false;
      }

      if (m_num_out_nodes != target_out_connections.size())
      {
         return false;
      }

      // here we might check codes, if we haven't already

      HashMap<NodeRecord, INode> template_to_graph = new HashMap<>();

      template_to_graph.put(FindNodeRecord("<target>"), target);

      // create nodes for each we are adding and map to their NodeRecords
      for (NodeRecord nr : m_nodes.values())
      {
         if (nr.Type == NodeType.Internal)
         {
            template_to_graph.put(nr, graph.AddNode(nr.Name, nr.Codes, m_name, nr.Radius));
         }
      }

      // find nodes for in connections and map to their NodeRecords
      {
         Iterator<DirectedEdge> g_it = target_in_connections.iterator();

         for (NodeRecord nr : m_nodes.values())
         {
            if (nr.Type == NodeType.In)
            {
               assert g_it.hasNext();

               INode g_conn = g_it.next().Start;

               template_to_graph.put(nr, g_conn);
            }
         }
      }

      // find nodes for out connections and map to their NodeRecords
      {
         Iterator<DirectedEdge> g_it = target_out_connections.iterator();

         for (NodeRecord nr : m_nodes.values())
         {
            if (nr.Type == NodeType.Out)
            {
               assert g_it.hasNext();

               INode g_conn = g_it.next().End;

               template_to_graph.put(nr, g_conn);
            }
         }
      }

      ApplyConnections(target, template_to_graph, graph);

      // make three attempts to position the nodes
      // no point if no random components, but pretty cheap to do...
      for (int i = 0; i < 3; i++)
      {
         if (TryPositions(graph, template_to_graph, random))
         {
            // we needed target for use in position calculations
            // but now we're done with it
            graph.RemoveNode(target);

            return true;
         }
      }

      return false;
   }

   private boolean TryPositions(Graph graph,
                        HashMap<NodeRecord, INode> template_to_graph,
                        Random rand)
   {
      // position new nodes relative to known nodes
      for (NodeRecord nr : m_nodes.values())
      {
         if (nr.Type == NodeType.Internal)
         {
            INode positionOn = template_to_graph.get(nr.PositionOn);

            XY pos = positionOn.GetPos();
            XY towards_step = new XY();
            XY away_step = new XY();

            if (nr.PositionTowards != null)
            {
               INode positionTowards = template_to_graph.get(nr.PositionTowards);

               XY d = positionTowards.GetPos().Minus(pos);

               towards_step = d.Multiply(0.1);
            }

            if (nr.PositionAwayFrom != null)
            {
               INode positionAwayFrom = template_to_graph.get(nr.PositionAwayFrom);

               XY d = positionAwayFrom.GetPos().Minus(pos);

               away_step = d.Multiply(0.1);
            }

            pos = pos.Plus(towards_step).Minus(away_step);

            if (nr.Nudge)
            {
               // we make the typical scale of edges and node radii on the order of
               // 100, so a displacement of 5 should be enough to separate things enough to avoid
               // stupid forces, while being nothing like as far as the nearest existing neighbours
               double angle = (rand.nextFloat() * (2 * Math.PI));
               pos = pos.Plus(new XY(Math.sin(angle) * 5, Math.cos(angle) * 5));
            }

            INode n = template_to_graph.get(nr);

            n.SetPos(pos);
         }
      }

      if (Util.FindCrossingEdges(graph.AllGraphEdges()).size() > 0)
      {
         return false;
      }

      return true;
   }

   private void ApplyConnections(INode node_replacing, HashMap<NodeRecord, INode> template_to_graph,
                         Graph graph)
   {
      for(DirectedEdge e : node_replacing.GetConnections())
      {
         graph.Disconnect(e.Start, e.End);
      }

      // apply new connections
      for (ConnectionRecord cr : m_connections.values())
      {
         INode nf = template_to_graph.get(cr.From);
         INode nt = template_to_graph.get(cr.To);

         graph.Connect(nf, nt, cr.MinLength, cr.MaxLength, cr.Width);
      }
   }

   final static class NodeRecord
   {
      public NodeType Type;
      public String Name;
      public boolean Nudge;
      public NodeRecord PositionOn;       // required
      public NodeRecord PositionTowards;  // null for none
      public NodeRecord PositionAwayFrom; // null for none
      public String Codes;                // copied onto node
      public double Radius;

      NodeRecord(NodeType type, String name,
                 boolean nudge, NodeRecord positionOn, NodeRecord positionTowards, NodeRecord positionAwayFrom,
                 String codes, double radius)
      {
         Type = type;
         Name = name;
         Nudge = nudge;
         PositionOn = positionOn;
         PositionTowards = positionTowards;
         PositionAwayFrom = positionAwayFrom;
         Codes = codes;
         Radius = radius;
      }
   }

   public static final class ConnectionRecord
   {
      public NodeRecord From;
      public NodeRecord To;
      public double MinLength;
      public double MaxLength;
      public double Width;

      ConnectionRecord(NodeRecord from, NodeRecord to,
                       double min_length, double max_length,
                       double width)
      {
         From = from;
         To = to;
         MinLength = min_length;
         MaxLength = max_length;
         Width = width;
      }
   }

   String GetCodes()
   {
      return m_codes;
   }

   String GetName()
   {
      return m_name;
   }

   private final String m_name;

   final private Map<String, NodeRecord> m_nodes;
   final private Map<String, ConnectionRecord> m_connections;

   // just to avoid keeping counting
   final private int m_num_in_nodes;
   final private int m_num_out_nodes;
   final private int m_num_internal_nodes;

   final private String m_codes;
}