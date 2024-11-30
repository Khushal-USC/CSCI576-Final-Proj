

# VIDEO PLAYER INSTRUCTIONS
1. Download the rgbs and wavs folders from here: https://drive.google.com/drive/u/0/folders/1Zobg8iIJhoISsk13NJztsQJ0zCFL-J5y.
Place those folders inside the main directory

2. Run following commands in terminal

```
javac VideoEncoder.java
java VideoEncoder .\rgb_video\Stairs.rgb .\wavs\Stairs.wav 3 3
```
Right now the numbers dont do anything, but make sure you choose the correct audio which corresponds to the video

If you are getting `java.lang.OutOfMemoryError`, you can try setting the max heap size to 8GB using the `-Xmx8G` flag and setting the initial heap size to 4GB using the -`Xms4GV` flag as follows.
```
java -Xmx8G -Xms4GV ideoEncoder .\rgb_video\Stairs.rgb .\wavs\Stairs.wav 3 3
```

# Image Display Instructions 
To run the code from command line, first compile with:
```
 javac ImageDisplay.java
```

and then, you can run it with the path to the RGB file as a parameter

NOTE: spaces in filepath may break the code

Usage:
```
java ImageDisplay <filePath> <coefficients>
```

Usage Example:

This displays the original image
```
java ImageDisplay ./rgb/Lenna.rgb -1
```

This displays DCT encoded/decoded image with 4096 coefficients selected
```
java ImageDisplay ./rgb/Lenna.rgb 4096
```

