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
