

# VIDEO PLAYER INSTRUCTIONS
1. Download the rgbs and wavs folders from here: https://drive.google.com/drive/u/0/folders/1Zobg8iIJhoISsk13NJztsQJ0zCFL-J5y.
Place those folders inside the main directory

2. Run following commands in terminal

```
javac VideoEncoder.java
```

To compress file
```
java VideoEncoder .\rgb_video\Stairs.rgb .\wavs\Stairs.wav 3 3 c
```
To view cmp file
```
java VideoEncoder .\rgb_video\Stairs.rgb .\wavs\Stairs.wav 3 3 v
```
Right now the numbers dont do anything, but make sure you choose the correct audio which corresponds to the video

If you are getting `java.lang.OutOfMemoryError`, you can try setting the max heap size to 8GB using the `-Xmx8G` flag and setting the initial heap size to 4GB using the `-Xms4GV` flag as follows.
```
java -Xmx8G -Xms4G VideoEncoder .\rgb_video\Stairs.rgb .\wavs\Stairs.wav 3 3
```

