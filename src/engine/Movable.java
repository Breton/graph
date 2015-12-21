package engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

// for the moment, not separating physically simulated from movable, but if required later, could split this
// into a base class of Movable and a derived class of PhysicallyMovable, giving us scope for other derived
// classes such as NonPhysicallyMoving for unstoppable things
//
// also for the moment, not separating Movable from "ICollidable" (which non-movable things, such as walls, could
// also implement...
public abstract class Movable
{
   protected Movable(double mass, double momentOfIntertia, double coefficientOfRestitution)
   {
      Mass = mass;
      MomentOfInertia = momentOfIntertia;
      CoefficientOfRestitution = coefficientOfRestitution;
   }

   public void setPosition(XY pos)
   {
      m_state.Position = pos;
   }

   public XY getPosition()
   {
      return m_state.Position;
   }

   public void applyForceAbsolute(XY force, XY position)
   {
      applyForceRelative(force, position.minus(m_state.Position));
   }

   @SuppressWarnings("WeakerAccess")
   public void applyForceRelative(XY force, XY relativePosition)
   {
      m_force = m_force.plus(force);

      double torque = relativePosition.rot270().dot(force);
      m_torque += torque;
   }

   public DynamicsState step(double timeStep)
   {
      return step(timeStep, timeStep);
   }

   public DynamicsState step(double positionTimeStep, double velocityTimeStep)
   {
      DynamicsState ret = new DynamicsState();

      ret.Position = m_state.Position.plus(m_state.Velocity.multiply(positionTimeStep));
      ret.Orientation = m_state.Orientation + m_state.Spin * positionTimeStep;
      ret.Velocity = m_state.Velocity.plus(m_force.multiply(velocityTimeStep / Mass));
      ret.Spin = m_state.Spin + m_torque * velocityTimeStep / MomentOfInertia;

      return ret;
   }

   public DynamicsState getState()
   {
      return m_state;
   }

   public void setState(DynamicsState state)
   {
      m_state = state;
   }

   public void applyImpulseRelative(XY impulse, XY relPoint)
   {
      m_state.Velocity = m_state.Velocity.plus(impulse.divide(Mass));
      m_state.Spin += relPoint.rot270().dot(impulse) / MomentOfInertia;
   }

   public ArrayList<XY> getTransformedCorners()
   {
      return getTransformedCorners(m_state);
   }

   public ArrayList<XY> getTransformedCorners(DynamicsPosition pos)
   {
      Transform t = new Transform(pos);

      return getCorners().stream().map(t::transform).collect(Collectors.toCollection(ArrayList::new));
   }

   public static class DynamicsPosition
   {
      public XY Position = new XY();
      public double Orientation = 0;

      @SuppressWarnings("SameParameterValue")
      DynamicsPosition interpolate(DynamicsPosition towards, double amount)
      {
         DynamicsPosition ret = new DynamicsPosition();

         ret.Position = Position.plus(towards.Position.minus(Position).multiply(amount));
         ret.Orientation = Orientation + (towards.Orientation - Orientation) * amount;

         return ret;
      }
   }

   public static class DynamicsState extends DynamicsPosition
   {
      public XY Velocity = new XY();
      public double Spin = 0;
   }

   // sum the combined effewct of translation and rotation for a point on the body
   // relativePoint is the offset from the mass centre
   public XY pointVelocity(XY relativePoint)
   {
      return m_state.Velocity.plus(relativePoint.rot270().multiply(m_state.Spin));
   }

   @SuppressWarnings("WeakerAccess")
   public abstract Collection<XY> getCorners();

   public abstract double getRadius();

   // these final more as a way of letting the compiler
   // check they got assigned, no absolute reason why they can't be changed later
   final public double Mass;
   final public double MomentOfInertia;
   final public double CoefficientOfRestitution;

   private DynamicsState m_state = new DynamicsState();

   private XY m_force = new XY();
   private double m_torque = 0;
}
