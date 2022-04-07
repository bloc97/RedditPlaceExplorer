# RedditPlaceExplorer
 A lightweight offline viewer for r/place written in java

![Screenshot](resources/Screenshot.png?raw=true)

Features:
 - Offline dataset (about ~1GiB, 19 GiB less than the original .csv, no internet connection needed after initial download)
 - Lossless compression (and much smaller than raw video files)
 - Accurate simulation of the canvas as it was displayed originally
 - Rewinding and backwards simulation of the canvas
 - Fast and efficient seeking
 - Dataset pre-caching/snapshotting for instantaneous skips in time

The x64 binaries download for windows with both 2017 and 2020 datasets is around 1GiB. After cache/snapshot generation the total program size grows to approximately 2.6GiB. The cache allows for instantaneous jumps and large seeks. To prevent the cache from generating, simply delete the `/cache/` folder before running the program, but be aware that you will lose the ability to seek large timesteps without lag.
The download with .jar and dataset alone (without JRE) is around 800MiB.

The program requires around of 200MB of RAM during my testing, and it should run fine on older systems.

Note that this was coded with my spare time during the Reddit 2022 r/place April Fools' event, so there might be unexpected bugs and the code is extremely messy.

## Controls
```
T: toggle between 2017/2022 canvas
F1: toggle help menu
F2: toggle hud
F3: toggle seek bar
F11: toggle fullscreen

Mouse drag: move canvas
Right click: set cursor
Scroll wheel: zoom

Shift/Ctrl: x10/x100 multiplier
Q/E: backward/forward one iteration
A/D: time seek
Arrow keys: move canvas
F: set cursor to camera
Alt+F: remove cursor
Z/X or PgUp/PgDown: zoom in/out

R: reset canvas
C: reset camera
V: jump to cursor
0-9: load preset
Ctrl+0-9: save camera preset
Shift+0-9: save time preset
Alt+0-9: delete preset

```

## Building
Simply download NetBeans and load the project.

## Dataset Sources
[2017 r/place Dataset](https://www.reddit.com/r/redditdata/comments/6640ru/place_datasets_april_fools_2017/) .csv ~1GiB  
[2022 r/place Dataset](https://www.reddit.com/r/place/comments/txvk2d/rplace_datasets_april_fools_2022/) .csv ~21GiB  
