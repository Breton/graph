package engine.graph;

import engine.XY;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

@SuppressWarnings("ConstantConditions")
public class TemplateTest
{
   @Test
   public void testExpand_Positioning() throws Exception
   {
      // no ins, outs or connections, just swapping one disconnected node for another...
      // place directly on replaced node
      {
         TemplateBuilder tb = new TemplateBuilder("", "");

         tb.AddNode(Template.NodeType.Internal, "a");

         Template t = tb.Build();

         Graph g = new Graph();

         INode a = g.addNode("x", "", "", 0);
         a.setPos(new XY(-4, 3));

         assertTrue(t.Expand(g, a, new Random(1)));
         assertEquals(1, g.numNodes());
         INode new_n = g.allGraphNodes().get(0);
         assertEquals("a", new_n.getName());
         assertEquals(new XY(-4, 3), new_n.getPos());
      }

      // no ins, outs or connections, just swapping one disconnected node for another...
      // place offset from replaced node
      {
         TemplateBuilder tb = new TemplateBuilder("", "");

         tb.AddNode(Template.NodeType.Internal, "a",
               true, "<target>", null, null,
               "", 0.0);

         Template t = tb.Build();

         Graph g = new Graph();

         INode n = g.addNode("x", "", "", 0);
         n.setPos(new XY(-4, 3));

         assertTrue(t.Expand(g, n, new Random(1)));
         assertEquals(1, g.numNodes());
         INode new_n = g.allGraphNodes().get(0);
         assertEquals("a", new_n.getName());

         // we offset by 5 in a random direction
         double dist = new XY(-4, 3).minus(new_n.getPos()).length();
         assertEquals(5, dist, 1e-6);
      }

      // an "in" and a replaced node,
      // place new node on the "in"
      // (poss not a desirable scenario but should work...)
      {
         TemplateBuilder tb = new TemplateBuilder("", "");

         tb.AddNode(Template.NodeType.In, "in");
         tb.AddNode(Template.NodeType.Internal, "a",
               false, "in", null, null,
               "", 0.0);
         tb.Connect("in", "a", 0, 0, 0);

         Template t = tb.Build();

         Graph g = new Graph();

         INode a = g.addNode("x", "", "", 0);
         a.setPos(new XY(-4, 3));
         INode in = g.addNode("in", "", "", 0);
         in.setPos(new XY(10, 9));

         g.connect(in, a, 0, 0, 0);

         assertTrue(t.Expand(g, a, new Random(1)));
         assertEquals(2, g.numNodes());
         assertNotNull(FindNode(g, "in"));
         INode new_n = FindNode(g, "a");
         assertNotNull(new_n != null);
         assertEquals(new XY(10, 9), new_n.getPos());
      }

      // an "in" and a replaced node,
      // place new node offset from the "in"
      {
         TemplateBuilder tb = new TemplateBuilder("", "");

         tb.AddNode(Template.NodeType.In, "in");
         tb.AddNode(Template.NodeType.Internal, "a",
               true, "in", null, null,
               "", 0.0);
         tb.Connect("in", "a", 0, 0, 0);

         Template t = tb.Build();

         Graph g = new Graph();

         INode a = g.addNode("x", "", "", 0);
         a.setPos(new XY(-4, 3));
         INode in = g.addNode("in", "", "", 0);
         in.setPos(new XY(10, 9));

         g.connect(in, a, 0, 0, 0);

         assertTrue(t.Expand(g, a, new Random(1)));
         assertEquals(2, g.numNodes());
         assertNotNull(FindNode(g, "in"));
         INode new_n = FindNode(g, "a");
         assertNotNull(new_n != null);

         // we offset by 5 in a random direction
         double dist = new XY(10, 9).minus(new_n.getPos()).length();
         assertEquals(5, dist, 1e-6);
      }

      // an "in" and a replaced node,
      // place new node on replaced node but moved towards "in"
      {
         TemplateBuilder tb = new TemplateBuilder("", "");

         tb.AddNode(Template.NodeType.In, "in");
         tb.AddNode(Template.NodeType.Internal, "a",
               false, "<target>", "in", null,
               "", 0.0);
         tb.Connect("in", "a", 0, 0, 0);

         Template t = tb.Build();

         Graph g = new Graph();

         INode a = g.addNode("x", "", "", 0);
         a.setPos(new XY(-4, 3));
         INode in = g.addNode("in", "", "", 0);
         in.setPos(new XY(-14, -7));

         g.connect(in, a, 0, 0, 0);

         assertTrue(t.Expand(g, a, new Random(1)));
         assertEquals(2, g.numNodes());
         assertNotNull(FindNode(g, "in"));
         INode new_n = FindNode(g, "a");
         assertNotNull(new_n != null);

         // position on replaced node but 10% of the way towards "in"
         assertEquals(new XY(-5, 2), new_n.getPos());
      }

      // an "in" and a replaced node,
      // place new node on replaced node but moved away from  "in"
      {
         TemplateBuilder tb = new TemplateBuilder("", "");

         tb.AddNode(Template.NodeType.In, "in");
         tb.AddNode(Template.NodeType.Internal, "a",
               false, "<target>", null, "in",
               "", 0.0);
         tb.Connect("in", "a", 0, 0, 0);

         Template t = tb.Build();

         Graph g = new Graph();

         INode a = g.addNode("x", "", "", 0);
         a.setPos(new XY(-4, 3));
         INode in = g.addNode("in", "", "", 0);
         in.setPos(new XY(-14, -7));

         g.connect(in, a, 0, 0, 0);

         assertTrue(t.Expand(g, a, new Random(1)));
         assertEquals(2, g.numNodes());
         assertNotNull(FindNode(g, "in"));
         INode new_n = FindNode(g, "a");
         assertNotNull(new_n != null);

         // position on replaced node but 10% of the way towards "in"
         assertEquals(new XY(-3, 4), new_n.getPos());
      }

      // an "in", an "out", and a replaced node,
      // place new node on replaced node but moved away from "in"
      // and towards "out"
      {
         TemplateBuilder tb = new TemplateBuilder("", "");

         tb.AddNode(Template.NodeType.In, "in");
         tb.AddNode(Template.NodeType.Out, "out");
         tb.AddNode(Template.NodeType.Internal, "a",
               false, "<target>", "out", "in",
               "", 0.0);
         tb.Connect("in", "a", 0, 0, 0);

         Template t = tb.Build();

         Graph g = new Graph();

         INode a = g.addNode("x", "", "", 0);
         a.setPos(new XY(10, 10));
         INode in = g.addNode("in", "", "", 0);
         in.setPos(new XY(20, 10));
         INode out = g.addNode("out", "", "", 0);
         out.setPos(new XY(10, 20));

         g.connect(in, a, 0, 0, 0);
         g.connect(a, out, 0, 0, 0);

         assertTrue(t.Expand(g, a, new Random(1)));
         assertEquals(3, g.numNodes());
         assertNotNull(FindNode(g, "in"));
         INode new_n = FindNode(g, "a");
         assertNotNull(new_n != null);

         // position on replaced node but 10% of the way away from "in"
         // and 10% (rel to original position) towards out
         assertEquals(new XY(9, 11), new_n.getPos());
      }
   }

   class PostExpandReporter implements Template.IPostExpand
   {
      @Override
      public void AfterExpand(INode n)
      {
         AfterExpandCount++;
         AfterExpandAfterDone = DoneCount > 0;
      }

      @Override
      public void Done()
      {
         DoneCount++;
         DoneAfterAfterExpand = AfterExpandCount > 0;
      }

      int AfterExpandCount = 0;
      int DoneCount = 0;
      boolean DoneAfterAfterExpand = false;
      boolean AfterExpandAfterDone = false;
   }

   @Test
   public void testPostExpand()
   {
      {
         PostExpandReporter ipo = new PostExpandReporter();

         TemplateBuilder tb = new TemplateBuilder("", "", ipo);

         tb.AddNode(Template.NodeType.Internal, "a");

         Template t = tb.Build();

         Graph g = new Graph();

         INode a = g.addNode("x", "", "", 0);

         assertTrue(t.Expand(g, a, new Random(1)));

         assertEquals(1, ipo.AfterExpandCount);
         assertEquals(1, ipo.DoneCount);
         assertTrue(ipo.DoneAfterAfterExpand);
         assertFalse(ipo.AfterExpandAfterDone);
      }
   }

   @Test
   public void testExpand_Fails() throws Exception
   {
      // cannot expand with unavoidable crossing edges
      {
         TemplateBuilder tb = new TemplateBuilder("", "");

         tb.AddNode(Template.NodeType.In, "in");
         tb.AddNode(Template.NodeType.Internal, "x",
               false, "<target>", null, null,
               "", 0);

         tb.Connect("in", "x", 0, 0, 0);

         Template t = tb.Build();

         Graph g = new Graph();

         INode a = g.addNode("a", "", "", 0);
         INode b = g.addNode("b", "", "", 0);
         INode c = g.addNode("c", "", "", 0);
         INode d = g.addNode("d", "", "", 0);

         a.setPos(new XY(10, 0));
         b.setPos(new XY(-10, 0));
         c.setPos(new XY(0, 10));
         d.setPos(new XY(0, -10));

         g.connect(a, b, 0, 0, 0);
         g.connect(c, d, 0, 0, 0);

         IGraphRestore igr = g.createRestorePoint();

         // cannot succeed as want to re-connect c and d
         // but that line has to hit the a -> b edge
         assertFalse(t.Expand(g, d, new Random(1)));

         // failed template expansion is destructive, so restore our graph
         igr.Restore();

         // just to prove this is why we are failing
         g.disconnect(a, b);

         assertTrue(t.Expand(g, d, new Random(1)));
      }

      // fail with various wrong numbers of ins/outs
      {
         TemplateBuilder tb = new TemplateBuilder("", "");

         tb.AddNode(Template.NodeType.In, "in");
         tb.AddNode(Template.NodeType.Out, "out");
         tb.AddNode(Template.NodeType.Internal, "x",
               false, "<target>", null, null,
               "", 0);

         Template t = tb.Build();

         Graph g = new Graph();

         INode a = g.addNode("a", "", "", 0);
         INode b = g.addNode("b", "", "", 0);
         INode c = g.addNode("c", "", "", 0);
         INode d = g.addNode("d", "", "", 0);
         INode x = g.addNode("x", "", "", 0);

         {
            IGraphRestore igr = g.createRestorePoint();

            // no ins or outs
            assertFalse(t.Expand(g, x, new Random(1)));

            // failed template expansion is destructive, so restore our graph
            igr.Restore();
         }

         g.connect(a, x, 0, 0, 0);

         {
            IGraphRestore igr = g.createRestorePoint();

            // still no outs
            assertFalse(t.Expand(g, x, new Random(1)));

            // failed template expansion is destructive, so restore our graph
            igr.Restore();
         }

         g.connect(x, b, 0, 0, 0);

         {
            IGraphRestore igr = g.createRestorePoint();

            // 1 in 1 out
            assertTrue(t.Expand(g, x, new Random(1)));

            // failed template expansion is destructive, so restore our graph
            igr.Restore();
         }

         g.connect(c, x, 0, 0, 0);

         {
            IGraphRestore igr = g.createRestorePoint();

            // two ins 1 out
            assertFalse(t.Expand(g, x, new Random(1)));

            // failed template expansion is destructive, so restore our graph
            igr.Restore();
         }

         g.connect(x, d, 0, 0, 0);

         {
            IGraphRestore igr = g.createRestorePoint();

            // 2 ins 2 outs
            assertFalse(t.Expand(g, x, new Random(1)));

            // failed template expansion is destructive, so restore our graph
            igr.Restore();
         }

         g.disconnect(b, x);

         {
            IGraphRestore igr = g.createRestorePoint();

            // 0 ins 2 outs
            assertFalse(t.Expand(g, x, new Random(1)));

            // failed template expansion is destructive, so restore our graph
            igr.Restore();
         }

         g.disconnect(x, c);

         {
            IGraphRestore igr = g.createRestorePoint();

            // 0 ins 1 out
            assertFalse(t.Expand(g, x, new Random(1)));

            // failed template expansion is destructive, so restore our graph
            igr.Restore();
         }
      }
   }

   @Test
   public void testGetCode()
   {
      TemplateBuilder tb = new TemplateBuilder("a", "xyz");
      Template t = tb.Build();

      assertEquals("xyz", t.GetCodes());
   }

   @Test
   public void testNodesAdded()
   {
      {
         TemplateBuilder tb = new TemplateBuilder("", "");
         Template t = tb.Build();

         assertEquals(-1, t.NodesAdded());
      }

      {
         TemplateBuilder tb = new TemplateBuilder("", "");
         tb.AddNode(Template.NodeType.In, "in1");
         tb.AddNode(Template.NodeType.Out, "out1");
         Template t = tb.Build();

         // in or out nodes have no effect on change in total
         assertEquals(-1, t.NodesAdded());
      }

      {
         TemplateBuilder tb = new TemplateBuilder("", "");
         tb.AddNode(Template.NodeType.Internal, "internal",
               false, "<target>", null, null,
               "", 0);

         Template t = tb.Build();

         // one replaced node is removed and an internal node is added
         assertEquals(0, t.NodesAdded());
      }

      {
         TemplateBuilder tb = new TemplateBuilder("", "");
         tb.AddNode(Template.NodeType.Internal, "internal",
               false, "<target>", null, null,
               "", 0);
         tb.AddNode(Template.NodeType.Internal, "internal2",
               false, "<target>", null, null,
               "", 0);

         Template t = tb.Build();

         // one replaced node is removed and two internal nodes are added
         assertEquals(1, t.NodesAdded());
      }
   }

   // finds first node of required name, unit tests keep this unique
   // but that isn't a requirement of graphs generally
   private static INode FindNode(Graph g, String name)
   {
      for (INode n : g.allGraphNodes())
      {
         if (n.getName().equals(name))
            return n;
      }

      return null;
   }
}
