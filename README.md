# BetterHomes
 is a nice little plugin for managing homes on a Vanilla+ Server.
 

## Usage

### Setting your home

`/sethome <optional,Word,default="home":homename>`

With the permission `betterhomes.user.sethome`, users can run this command, to create a new "Home" at the current Position with a custom name.

Example: `/sethome myhome`

### Teleporting to a previously set Home

`/home <optional,Word,default="home":homename>`

With the permission `betterhomes.user.home`, Users can run this command to teleport to a previously set Home.

Exampe: `/home myhome`

### Deleting a prefiously set Home

`/delhome <required,Word:homename>`

With the permission `betterhomes.user.delhome`, Users can delete Homes they previously set with the homename

Exampe: `/delhome myhome`

## Configuration

The plugin can be configured using `config.yml` inside of `plugins/BetterHomes`

### Disabling the Plugin

`homes-enabled`

If you need to disable the Plugin for any reason you can do so by setting `homes-enabled` to false. It can be reenabled by setting this setting back to true

### Changing the maximum Homes

`max-homes`

To change the maximum number of Homes a player can have on the Server, you can change `max-homes` to any positive Integer starting at 1 up to 2147483647

### Allowing teleportation into other Worlds

`allow-cross-world-teleportation`

By default, players can not teleport into other Worlds using Homes. This can be changed by setting `allow-cross-world-teleportation` to true.

## Coming soon
 - Administration Tools