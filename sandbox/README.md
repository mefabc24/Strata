# Sandbox placement demo

Run `./gradlew :sandbox:run` with JDK 21 and the Sandbox assets installed (see
[ASSETS.md](ASSETS.md)). The Tool Rail is visible on startup; Tab toggles it.

Right-click **Build** in the rail to select it and open its settings. Choose an
existing one-tile object, such as **Flower1**, **Trunk1**, or **OakTree**, in
Selection. In **Gesture**, choose **Path**:

- Left-click a world tile to start.
- Move the cursor to see the live preview; dragging also updates it.
- Left-click again to confirm a waypoint and continue from that point.
- Add as many waypoints as needed, then press Enter to place the complete preview.
- Press Escape or **Cancel path** to cancel. Left-click starts a new path afterward.
- Right-click world objects to remove them, as in rectangular Build mode.

Changing the object, gesture shape, or tool, hiding the Tool Rail, or switching
worlds cancels an unfinished path. Escape cancels an active path before toggling
the Sandbox Debug Window. The rail's separate **Path** tool demonstrates A*
pathfinding; path placement is the **Path** shape inside **Build**.

Choose **Rectangle** to use the existing left-drag/release placement. A click
without dragging places one object. Both shapes share footprint, occupancy,
bounds, preview colors, and external validation. Invalid origins are skipped;
large objects can therefore leave gaps along a path when their footprints overlap.
