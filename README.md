# spider-game

A game about being a spider

Still very muich WIP, but the core mechanics are nearly there, then there's a lot of polishing to do.

## Controls

- Click to move the spider
- Press `space` when near a fly to try and eat it before it escapes and breaks your web
  - Press space again to bite at the right time
  - Click threads across the fly enough times to wrap it up
- Press `r` when near a broken web node to repair it (there'll be a minigame mechanic for this soon)

## Running locally

``` bash
lein run
```

## Build jar

``` bash
# Build
lein uberjar

# Run
java -jar target/uberjar/spider-game-0.1.0-standalone.jar
```
