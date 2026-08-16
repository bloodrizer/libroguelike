#A. Pedestrians

A1. should located bed and sleep when tired or at night
A2. should not break furniture when pathfiding normally
A3. must escape player or other npc when attacked
A4. must open doors when crossing them and break furniture when escaping

#B. Police

B1. should chase player or npc who comitted crime
B2. should chase player who is chansed by other police npc

#C. World gen

C1. Furniture should not spawn adjacent to windows or doors.
C2. This includes doors and windows themselves (can not be adjacent to each other)
C3. Non-police NPC should spawn at their home rooms

#D. AI

D1. AI should not output text longer then two sentences or 30 chars.\
D2. NPC should not talk, move, listen or act when they are sleeping

#E. Pathfinding

E1. Every milestone must be a node of the nav graph (a doorway is a milestone too)
E2. Every milestone must be routable from every other one
E3. A route handed to a walker must be contiguous - no step may be a leap
E4. An NPC sent to the far side of town must get a route, not a straight line at it
