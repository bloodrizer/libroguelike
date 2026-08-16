package com.nuclearunicorn.serialkiller.game.events;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import org.lwjgl.util.Point;

/**
 * Someone used force on something. Whether that is a <i>crime</i> is {@code CrimeSensor}'s
 * question, and it needs the victim to answer it — swinging at a crate raises this exactly
 * like stabbing a neighbour does.
 */
public class CriminalActionEvent extends Event {

   public Point origin;
   public EntityActor criminal;
   /** What was struck. Null only for callers that predate this field. */
   public Entity victim;

   public CriminalActionEvent(Point origin, EntityActor criminal){
       this(origin, criminal, null);
   }

   public CriminalActionEvent(Point origin, EntityActor criminal, Entity victim){
       this.origin = origin;
       this.criminal = criminal;
       this.victim = victim;
   }
}
