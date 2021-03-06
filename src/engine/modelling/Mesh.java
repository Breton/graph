package engine.modelling;

import engine.IDraw;
import engine.OrderedPair;
import engine.XYZ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Mesh
{
   // offset exact meaning depends on shape created, but is for example base of cylinder, centre of sphere
   // elevation is rotation about Y, followed by
   // orientation, which is rotation about Z
   private Mesh(XYZ[] points, XYZ[] normals, Triangle[] triangles)
   {
      for(XYZ xyz : points)
      {
         assert xyz != null;
      }

      for(XYZ xyz : normals)
      {
         assert xyz != null;
      }

      for(Triangle t : triangles)
      {
         assert t != null;

         assert t.PointIndex1 >= 0 && t.PointIndex1 < points.length;
         assert t.PointIndex2 >= 0 && t.PointIndex2 < points.length;
         assert t.PointIndex3 >= 0 && t.PointIndex3 < points.length;

         assert t.NormalIndex1 >= 0 && t.NormalIndex1 < normals.length;
         assert t.NormalIndex2 >= 0 && t.NormalIndex2 < normals.length;
         assert t.NormalIndex3 >= 0 && t.NormalIndex3 < normals.length;
      }

      Points = points;
      Normals = normals;
      Triangles = triangles;
   }

   public void draw(IDraw draw)
   {
      draw.beginTriangles();

      for(Triangle t : Triangles)
      {
         draw.triangle(
               Points[t.PointIndex1],
               Points[t.PointIndex2],
               Points[t.PointIndex3],
               Normals[t.NormalIndex1],
               Normals[t.NormalIndex2],
               Normals[t.NormalIndex3]);
      }

      draw.endTriangles();
   }

   // base of cylinder is at (0, 0, 0) facing up X
   static public Mesh createCylinder(double radius, double length, double facetingFactor,
                                     boolean capBase, boolean capTop, int maxSlicesRound, int maxSlicesUp,
                                     boolean smooth)
   {
      return createCone(radius, radius, length, facetingFactor,
            capBase, capTop, maxSlicesRound, maxSlicesUp,
            smooth);
   }

   public static Mesh createCone(double baseRadius, double topRadius, double length, double facetingFactor,
                                 boolean capBase, boolean capTop, int maxSlicesRound, int maxSlicesUp,
                                 boolean smooth)
   {
      double max_rad = Math.max(baseRadius, topRadius);

      // we need enough steps to represent curveature
      // do this before applying maxima, as maxima can deliberately make us a triangle etc
      while (facetingFactor > baseRadius / 2 || facetingFactor > topRadius / 2)
         facetingFactor /= 2;

      int radius_steps = (int)(Math.PI * 2 * max_rad / facetingFactor);
      int length_steps = (int)(length / facetingFactor);

      if (maxSlicesRound != -1)
         radius_steps = Math.min(radius_steps, maxSlicesRound);

      if (maxSlicesUp != -1)
         length_steps = Math.min(length_steps, maxSlicesUp);

      // must at least be a triangular prism
      radius_steps = Math.max(radius_steps, 3);
      //with at least two rings of points
      length_steps = Math.max(length_steps, 2);

      int points_size = radius_steps * length_steps;
      int normals_size = points_size;
      int triangle_size = radius_steps * (length_steps - 1) * 2;

      // need one normal, one centre point
      // and one fan of radius_steps triangles for each end
      if (capBase)
      {
         normals_size++;
         points_size++;
         triangle_size += radius_steps;
      }

      if (capTop)
      {
         normals_size++;
         points_size++;
         triangle_size += radius_steps;
      }

      XYZ[] points = new XYZ[points_size];
      XYZ[] normals = new XYZ[normals_size];
      Triangle[] triangles = new Triangle[triangle_size];

      int which = 0;
      double angle_step = 2 * Math.PI / radius_steps;
      double length_step = length / (length_steps - 1);

      double length_pos = 0;

      for(int i = 0; i < length_steps; i++)
      {
         double angle = 0;

         double frac = (double)i / (length_steps - 1);

         double radius = baseRadius + (topRadius - baseRadius) * frac;

         for(int j = 0; j < radius_steps; j++)
         {
            points[which] = new XYZ(length_pos, Math.sin(angle) * radius, Math.cos(angle) * radius);
            // wrong, need to factor slope in also...
            normals[which] = new XYZ(0, Math.sin(angle), Math.cos(angle));
            which++;

            angle += angle_step;
         }

         length_pos += length_step;
      }

      int trig_which = 0;

      if (capBase)
      {
         normals[which] = new XYZ(-1, 0, 0);
         points[which] = new XYZ(0, 0, 0);

         int prev_idx = radius_steps - 1;
         for(int i = 0; i < radius_steps; i++)
         {
            triangles[trig_which] = new Triangle(
                  prev_idx, i, which,
                  which, which, which);

            trig_which++;
            prev_idx = i;
         }

         which++;
      }

      if (capTop)
      {
         normals[which] = new XYZ(1, 0, 0);
         points[which] = new XYZ(length, 0, 0);

         int prev_idx = length_steps * radius_steps - 1;
         for(int i = (length_steps - 1) * radius_steps; i < length_steps * radius_steps; i++)
         {
            triangles[trig_which] = new Triangle(
                  i, prev_idx, which,
                  which, which, which);

            trig_which++;
            prev_idx = i;
         }
      }

      which = 0;
      int next_line_which = radius_steps;

      for(int i = 0; i < length_steps - 1; i++)
      {
         int prev = (i + 1) * radius_steps - 1;
         int next_line_prev = (i + 2) * radius_steps - 1;
         for(int j = 0; j < radius_steps; j++)
         {
            triangles[trig_which] = new Triangle(
                  which, prev, next_line_which,
                  which, prev, next_line_which);

            triangles[trig_which + 1] = new Triangle(
                  prev, next_line_prev, next_line_which,
                  prev, next_line_prev, next_line_which);

            trig_which += 2;

            next_line_prev = next_line_which;
            prev = which;

            which++;
            next_line_which++;
         }
      }

      Mesh ret = new Mesh(points, normals, triangles);

      if (!smooth)
         ret = ret.facetted();

      return ret.optimised();
   }

   public static Mesh createSphere(double radius, double baseHeight, double topHeight, double facetingFactor,
                                   boolean capBase, boolean capTop, int maxSlicesRound, int maxSlicesUp,
                                   boolean smooth)
   {
      assert topHeight >= -radius && topHeight <= radius;
      assert baseHeight >= -radius && baseHeight <= radius;
      assert topHeight > baseHeight;

      double length = topHeight - baseHeight;

      // we need enough steps to represent curveature
      // do this before applying maxima, as maxima can deliberately make us a triangle etc
      while (facetingFactor > radius / 2 || facetingFactor > length / 2)
         facetingFactor /= 2;

      int radius_steps = (int)(Math.PI * 2 * radius / facetingFactor);
      int length_steps = (int)(length / facetingFactor);

      if (maxSlicesRound != -1)
         radius_steps = Math.min(radius_steps, maxSlicesRound);

      if (maxSlicesUp != -1)
         length_steps = Math.min(length_steps, maxSlicesUp);

      // must at least be a triangular prism
      radius_steps = Math.max(radius_steps, 3);
      //with at least two rings of points
      length_steps = Math.max(length_steps, 2);


      int points_size = radius_steps * length_steps;
      int normals_size = points_size;
      int triangle_size = radius_steps * (length_steps - 1) * 2;

      // need one normal, one centre point
      // and one fan of radius_steps triangles for each end
      if (capBase)
      {
         normals_size++;
         points_size++;
         triangle_size += radius_steps;
      }

      if (capTop)
      {
         normals_size++;
         points_size++;
         triangle_size += radius_steps;
      }

      XYZ[] points = new XYZ[points_size];
      XYZ[] normals = new XYZ[normals_size];
      Triangle[] triangles = new Triangle[triangle_size];

      int which = 0;
      double angle_step = 2 * Math.PI / radius_steps;
      double length_step = length / (length_steps - 1);

      double length_pos = baseHeight;

      for(int i = 0; i < length_steps; i++)
      {
         double angle = 0;

         double slice_radius = Math.sqrt(radius * radius - length_pos * length_pos);

         for(int j = 0; j < radius_steps; j++)
         {
            points[which] = new XYZ(length_pos, Math.sin(angle) * slice_radius, Math.cos(angle) * slice_radius);
            normals[which] = points[which].asUnit();
            which++;

            angle += angle_step;
         }

         length_pos += length_step;
      }

      int trig_which = 0;

      if (capBase)
      {
         normals[which] = new XYZ(-1, 0, 0);
         //noinspection SuspiciousNameCombination
         points[which] = new XYZ(baseHeight, 0, 0);

         int prev_idx = radius_steps - 1;
         for(int i = 0; i < radius_steps; i++)
         {
            triangles[trig_which] = new Triangle(
                  prev_idx, i, which,
                  which, which, which);

            trig_which++;
            prev_idx = i;
         }

         which++;
      }

      if (capTop)
      {
         normals[which] = new XYZ(1, 0, 0);
         //noinspection SuspiciousNameCombination
         points[which] = new XYZ(topHeight, 0, 0);

         int prev_idx = length_steps * radius_steps - 1;
         for(int i = (length_steps - 1) * radius_steps; i < length_steps * radius_steps; i++)
         {
            triangles[trig_which] = new Triangle(
                  i, prev_idx, which,
                  which, which, which);

            trig_which++;
            prev_idx = i;
         }
      }

      which = 0;
      int next_line_which = radius_steps;

      for(int i = 0; i < length_steps - 1; i++)
      {
         int prev = (i + 1) * radius_steps - 1;
         int next_line_prev = (i + 2) * radius_steps - 1;
         for(int j = 0; j < radius_steps; j++)
         {
            triangles[trig_which] = new Triangle(
                  which, prev, next_line_which,
                  which, prev, next_line_which);

            triangles[trig_which + 1] = new Triangle(
                  prev, next_line_prev, next_line_which,
                  prev, next_line_prev, next_line_which);

            trig_which += 2;

            next_line_prev = next_line_which;
            prev = which;

            which++;
            next_line_which++;
         }
      }

      Mesh ret = new Mesh(points, normals, triangles);

      if (!smooth)
         ret = ret.facetted();

      return ret.optimised();
   }

   private Mesh optimised()
   {
      // we assume only normals and points need optimising
      // triangles should already be unique

      ArrayList<Triangle> triangles = new ArrayList<>();

      HashMap<XYZ, Integer> point_map = new HashMap<>();
      HashMap<XYZ, Integer> normal_map = new HashMap<>();

      for(Triangle t : Triangles)
      {
         int pi1 = storePoint(Points[t.PointIndex1], point_map);
         int pi2 = storePoint(Points[t.PointIndex2], point_map);
         int pi3 = storePoint(Points[t.PointIndex3], point_map);

         int ni1 = storePoint(Normals[t.NormalIndex1], normal_map);
         int ni2 = storePoint(Normals[t.NormalIndex2], normal_map);
         int ni3 = storePoint(Normals[t.NormalIndex3], normal_map);

         triangles.add(new Triangle(pi1, pi2, pi3, ni1, ni2, ni3));
      }

      assert triangles.size() == Triangles.length;
      assert point_map.size() <= Points.length;
      assert normal_map.size() <= Normals.length;

      XYZ[] points_out = point_map.entrySet()
            .stream()
            .sorted((x, y) -> x.getValue() - y.getValue())
            .map(Map.Entry::getKey).toArray(XYZ[]::new);

      XYZ[] normals_out = normal_map.entrySet()
            .stream()
            .sorted((x, y) -> x.getValue() - y.getValue())
            .map(Map.Entry::getKey).toArray(XYZ[]::new);

      Triangle[] triangles_out = triangles.stream().toArray(Triangle[]::new);

      return new Mesh(points_out, normals_out, triangles_out);
   }

   private int storePoint(XYZ point, HashMap<XYZ, Integer> point_map)
   {
      Integer i = point_map.get(point);

      if (i != null)
         return i;

      int next = point_map.size();

      point_map.put(point, next);

      return next;
   }

   private Mesh facetted()
   {
      // we assume only normals and points need optimising
      // triangles should already be unique

      ArrayList<Triangle> triangles = new ArrayList<>();

      HashMap<XYZ, Integer> normal_map = new HashMap<>();

      for(Triangle t : Triangles)
      {
         XYZ face_normal =
               Points[t.PointIndex3].minus(Points[t.PointIndex2])
                     .cross(Points[t.PointIndex1].minus(Points[t.PointIndex2]));

         if (!face_normal.isZero())
         {
            face_normal = face_normal.asUnit();
         }
         else
         {
            face_normal = Normals[t.NormalIndex1];
         }

         int ni = storePoint(face_normal, normal_map);

         triangles.add(new Triangle(t.PointIndex1, t.PointIndex2, t.PointIndex3, ni, ni, ni));
      }

      assert triangles.size() == Triangles.length;

      XYZ[] normals_out = normal_map.entrySet()
            .stream()
            .sorted((x, y) -> x.getValue() - y.getValue())
            .map(Map.Entry::getKey).toArray(XYZ[]::new);

      Triangle[] triangles_out = triangles.stream().toArray(Triangle[]::new);

      return new Mesh(Points, normals_out, triangles_out);
   }

   enum Axes
   {
      XAxis,
      YAxis,
      ZAxis
   }

   public static Mesh createCuboid(double xSize, double ySize, double zSize, double facetingFactor)
   {
      int y_steps = (int) Math.max(2, ySize / facetingFactor);
      int z_steps = (int) Math.max(2, zSize / facetingFactor);
      int x_steps = (int) Math.max(2, xSize / facetingFactor);

      int point_size = (y_steps * z_steps + z_steps * x_steps + x_steps * y_steps) * 2;
      int normal_size = 6;
      int triangle_size =
            ((y_steps - 1) * (z_steps - 1)
                  + (z_steps - 1) * (x_steps - 1)
                  + (x_steps - 1) * (y_steps - 1)) * 4;

      XYZ points[] = new XYZ[point_size];
      XYZ normals[] = new XYZ[normal_size];
      Triangle triangles[] = new Triangle[triangle_size];

      normals[0] = new XYZ(1, 0, 0);
      normals[1] = new XYZ(-1, 0, 0);
      normals[2] = new XYZ(0, 1, 0);
      normals[3] = new XYZ(0, -1, 0);
      normals[4] = new XYZ(0, 0, 1);
      normals[5] = new XYZ(0, 0, -1);

      int wherePoints = 0;
      int whereTriangles = 0;

      OrderedPair<Integer, Integer> ret = face(wherePoints, whereTriangles,
            zSize, ySize, xSize / 2,
            z_steps, y_steps,
            Axes.ZAxis, Axes.YAxis,
            points, triangles,
            0);
      wherePoints = ret.First;
      whereTriangles = ret.Second;

      ret = face(wherePoints, whereTriangles,
            ySize, zSize, -xSize / 2,
            y_steps, z_steps,
            Axes.YAxis, Axes.ZAxis,
            points, triangles,
            1);
      wherePoints = ret.First;
      whereTriangles = ret.Second;

      ret = face(wherePoints, whereTriangles,
            xSize, zSize, ySize / 2,
            x_steps, z_steps,
            Axes.XAxis, Axes.ZAxis,
            points, triangles,
            2);
      wherePoints = ret.First;
      whereTriangles = ret.Second;

      ret = face(wherePoints, whereTriangles,
            zSize, xSize, -ySize / 2,
            z_steps, x_steps,
            Axes.ZAxis, Axes.XAxis,
            points, triangles,
            3);
      wherePoints = ret.First;
      whereTriangles = ret.Second;

      ret = face(wherePoints, whereTriangles,
            ySize, xSize, zSize / 2,
            y_steps, x_steps,
            Axes.YAxis, Axes.XAxis,
            points, triangles,
            4);
      wherePoints = ret.First;
      whereTriangles = ret.Second;

      face(wherePoints, whereTriangles,
            xSize, ySize, -zSize / 2,
            x_steps, y_steps,
            Axes.XAxis, Axes.YAxis,
            points, triangles,
            5);

      return new Mesh(points, normals, triangles);
   }

   private static OrderedPair<Integer, Integer> face(int wherePoints, int whereTriangles,
                            double size1, double size2, double position,
                            int steps1, int steps2,
                            Axes axis1, Axes axis2,
                            XYZ[] points, Triangle[] triangles,
                            int normalIdx)
   {
      double pos1 = -size1 / 2;

      double step1 = size1 / (steps1 - 1);
      double step2 = size2 / (steps2 - 1);

      int this_point = wherePoints;

      for (int i = 0; i < steps1; i++)
      {
         double pos2 = -size2 / 2;

         for (int j = 0; j < steps2; j++)
         {
            points[this_point] = createXYZ(pos1, pos2, position, axis1, axis2);
            this_point++;

            pos2 += step2;
         }

         pos1 += step1;
      }

      for(int i = 0; i < steps1 - 1; i++)
      {
         for (int j = 0; j < steps2 - 1; j++)
         {
            this_point = wherePoints + i * steps2 + j;
            int next_point = this_point + 1;
            int this_point_next_row = this_point + steps2;
            int next_point_next_row = this_point + steps2 + 1;

            assert this_point >= 0 && this_point < points.length;
            assert next_point >= 0 && next_point < points.length;
            assert this_point_next_row >= 0 && this_point_next_row < points.length;
            assert next_point_next_row >= 0 && next_point_next_row < points.length;

            triangles[whereTriangles] = new Triangle(
                  this_point, next_point, this_point_next_row,
                  normalIdx ,normalIdx, normalIdx);
            whereTriangles++;

            triangles[whereTriangles] = new Triangle(
                  next_point, next_point_next_row, this_point_next_row,
                  normalIdx, normalIdx, normalIdx);
            whereTriangles++;
         }
      }

      return new OrderedPair<>(wherePoints + steps1 * steps2, whereTriangles);
   }

   private static XYZ createXYZ(double pos1, double pos2, double position, Axes axis1, Axes axis2)
   {
      double x = findAxisValue(pos1, pos2, position, Axes.XAxis, axis1, axis2);
      double y = findAxisValue(pos1, pos2, position, Axes.YAxis, axis1, axis2);
      double z = findAxisValue(pos1, pos2, position, Axes.ZAxis, axis1, axis2);

      return new XYZ(x, y, z);
   }

   private static double findAxisValue(double pos1, double pos2, double position,
                                       Axes wantedAxis, Axes axis1,
                                       Axes axis2)
   {
      if (wantedAxis == axis1)
         return pos1;

      if (wantedAxis == axis2)
         return pos2;

      return position;
   }

   static class Triangle
   {
      private final int PointIndex1;
      private final int PointIndex2;
      private final int PointIndex3;
      private final int NormalIndex1;
      private final int NormalIndex2;
      private final int NormalIndex3;

      Triangle(int pointIndex1, int pointIndex2, int pointIndex3,
               int normalIndex1, int normalIndex2, int normalIndex3)
      {
         PointIndex1 = pointIndex1;
         PointIndex2 = pointIndex2;
         PointIndex3 = pointIndex3;
         NormalIndex1 = normalIndex1;
         NormalIndex2 = normalIndex2;
         NormalIndex3 = normalIndex3;
      }
   }

   private final XYZ[] Points;
   private final XYZ[] Normals;
   private final Triangle[] Triangles;
}
