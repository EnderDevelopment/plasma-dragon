# Minecraft Plugin: Plasma Dragon

Plasma Dragon is a Paper Minecraft plugin that adds a tameable flying dragon companion with a blue plasma attack, follow and stay behavior, hostile mob targeting, particle effects, and simple player commands. The dragon uses an invisible wolf anchor with a blue and light blue block model, follows its owner through the air, and can be controlled with the `/dragon` command.

## Install

1. Build the plugin with Java 21 and Maven:

   ```text
   mvn package
   ```

2. Copy the generated `target/plugin.jar` file into your server's `plugins` folder.

3. Run a Paper Minecraft server using Minecraft 1.21 or a compatible 1.21 version.

4. Start or restart the server.

5. Confirm that the plugin loaded successfully in the server console.

## Use

1. Join the server and run:

   ```text
   /dragon summon
   ```

2. Feed the summoned dragon a bone by right-clicking it with a bone in your main hand. The bone is consumed and the dragon becomes tamed.

3. Command the dragon with:

   ```text
   /dragon follow
   ```

   Makes the dragon follow you through the air.

   ```text
   /dragon stay
   ```

   Holds the dragon in its current position.

   ```text
   /dragon attack
   ```

   Enables automatic attacks against nearby hostile mobs.

4. The dragon searches for hostile mobs within its attack area and launches blue plasma projectiles. Plasma hits deal 10 damage and create particle and sound effects.

5. If you need to summon the dragon again, use:

   ```text
   /dragon summon
   ```

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/ec3baf38-ad1a-4f2c-b038-cd27796b73b9?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=ec3baf38-ad1a-4f2c-b038-cd27796b73b9
