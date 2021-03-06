package engine.brep;

import engine.Box;
import engine.Util;
import engine.XY;

@SuppressWarnings("WeakerAccess")
public class CircleCurve extends Curve
{
   public enum RotationDirection
   {
      Forwards,
      Reverse
   }

   final public XY Position;
   final public double Radius;
   final public RotationDirection Rotation;

   public CircleCurve(XY position, double radius)
   {
      this(position, radius, 0, Math.PI * 2);
   }

   public CircleCurve(XY position, double radius,
                      double start_angle, double end_angle)
   {
      this(position, radius, start_angle, end_angle, RotationDirection.Forwards);
   }

   public CircleCurve(XY position, double radius,
                      RotationDirection rotation)
   {
      this (position, radius, 0, Math.PI * 2, rotation);
   }

   private static double fixEndAngle(double start_angle, double end_angle)
   {
      start_angle = Util.fixupAngle(start_angle);
      end_angle = Util.fixupAngle(end_angle);

      if (end_angle <= start_angle)
         end_angle += Math.PI * 2;

      return end_angle;
   }

   public CircleCurve(XY position, double radius,
                      double start_angle, double end_angle,
                      RotationDirection rotation)
   {
      super(Util.fixupAngle(start_angle), fixEndAngle(start_angle, end_angle));

      Position = position;
      Radius = radius;
      Rotation = rotation;

      if (Position == null)
         throw new NullPointerException("null position");

      if (Radius <= 0)
         throw new IllegalArgumentException("-ve or zero radius");
   }

   @Override
   protected XY computePosInner(double param)
   {
      if (Rotation == RotationDirection.Forwards)
      {
         return Position.plus(new XY(Math.sin(param), Math.cos(param)).multiply(Radius));
      }

      return Position.plus(new XY(Math.sin(-param), Math.cos(-param)).multiply(Radius));
   }

   @Override
   public int hashCode()
   {
      return super.hashCode_inner() * 17
            ^ Position.hashCode() * 31
            ^ Double.hashCode(Radius) * 11
            ^ (Rotation == RotationDirection.Forwards ? 1 : 0);
   }

   @Override
   public boolean equals(Object o)
   {
      if (o == this)
         return true;

      if (!(o instanceof CircleCurve))
         return false;

      if (!super.equals_inner(o))
         return false;

      CircleCurve cc_o = (CircleCurve)o;

      return Position.equals(cc_o.Position)
            && Radius == cc_o.Radius
            && Rotation == cc_o.Rotation;
   }

   @Override
   public Double findParamForPoint(XY pnt, double tol)
   {
      XY relative = pnt.minus(Position);

      if (Math.abs(relative.length() - Radius) > tol)
         return null;

      double ang = Util.atan2(relative);

      if (Rotation == RotationDirection.Reverse)
      {
         ang =- ang;
      }

      // atan2 returns between -pi and + pi
      // we use 0 -> 2pi
      // BUT, we also require EndParam > StartParam
      while (ang < StartParam) ang += 2 * Math.PI;

      if (!withinParams(ang, tol))
         return null;

      return ang;
   }

   @Override
   public Curve cloneWithChangedParams(double start, double end)
   {
      return new CircleCurve(Position, Radius, start, end, Rotation);
   }

   @Override
   public Box boundingBox()
   {
      // use whole circle here as the use I have for the moment doesn't need anything
      // tighter
      //
      // full solution is to union together startPos, EndPos and whichever of
      // 0, pi/2, pi and 3pi/2 points are within param range
      return new Box(Position.minus(new XY(Radius, Radius)),
            Position.plus(new XY(Radius, Radius)));
   }

   @Override
   public XY tangent(Double param)
   {
      if (Rotation == RotationDirection.Reverse)
      {
         return new XY(-Math.cos(-param), Math.sin(-param));
      }

      return new XY(Math.cos(param), -Math.sin(param));
   }

   @Override
   public Curve merge(Curve c_after)
   {
      if (c_after == this)
         return null;

      if (!(c_after instanceof CircleCurve))
         return null;

      CircleCurve c_cc = (CircleCurve)c_after;

      if (!Position.equals(c_cc.Position))
         return null;

      if (Rotation != c_cc.Rotation)
         return null;

      if (Radius != c_cc.Radius)
         return null;

      if (!Util.clockAwareAngleCompare(EndParam, c_cc.StartParam, 1e-12))
         return null;

      return new CircleCurve(Position, Radius, StartParam, c_cc.EndParam, Rotation);
   }

   @Override
   public double length()
   {
      return Radius * (EndParam - StartParam);
   }

   @Override
   public XY computeNormal(double p)
   {
      XY normal = new XY(Math.sin(p), Math.cos(p));

      if (Rotation == RotationDirection.Reverse)
      {
         normal = normal.negate();
      }

      return normal;
   }

   @Override
   public boolean withinParams(double p, double tol)
   {
      if (isCyclic())
         return true;

      // we've fixed start param to lie between 0 and 2pi
      // and end param to be < 2pi above that
      // so if we are below start param and we step up one full turn
      // that either takes us right past end param (because we were too high)
      // or it takes us past it because we were too low and shouldn't have stepped up
      // or it leaves us below end param in which case we are in range
      if (p < StartParam)
         p += Math.PI * 2;

      return p < EndParam + tol;
   }

   public boolean isCyclic()
   {
      return Util.clockAwareAngleCompare(StartParam, EndParam, 1e-12);
   }
}
