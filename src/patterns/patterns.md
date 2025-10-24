### Pattern-1: 
pattern-1, for-loop

#### hint: row=1, col=row

```
*
* *
* * * 
* * * * 
* * * * * 
```
### pattern-1a, java streams
#### hint: row=1, col=row
```
*
* *
* * * 
* * * * 
* * * * * 
```

### pattern-1b
#### hint: row=1, col=row, print(colIndex)
```
1
12
123
1234
12345
```
### pattern-1c
#### hint: row=1, col=n, print(colIndex)
```
* * * * * 
* * * * * 
* * * * * 
* * * * * 
* * * * *
``` 

### pattern-1d
#### hint: row=1, row=n-1, col <= n-row+1
```
* * * * * 
* * * * 
* * * 
* *
*
```

### pattern-1e
#### hint: row=0, row < 2 * n, int totColRow = row > n ? 2 * n - row : row; col < totColRow
```
*
* *
* * * 
* * * * 
* * * * * 
* * * * 
* * * 
* *
*
```

### pattern-1e
#### hint: row=0, row < 2 * n, int totColRow = row > n ? 2 * n - row : row; numSpace = n - totColRow; space=0; space < numSpace; col < totColRow;
```
    * 
   * *
  * * * 
 * * * * 
* * * * * 
 * * * * 
  * * * 
   * *
    * 
```
### pattern-1g
```
1
2 1 2
3 2 1 2 3
4 3 2 1 2 3 4
5 4 3 2 1 2 3 4 5
```
### pattern-1h
```
1
2 1 2
3 2 1 2 3
4 3 2 1 2 3 4
5 4 3 2 1 2 3 4 5
4 3 2 1 2 3 4
3 2 1 2 3
2 1 2
1
```
### Pattern-1i
```
4 4 4 4 4 4 4 4 4
4 3 3 3 3 3 3 3 4
4 3 2 2 2 2 2 3 4
4 3 2 1 1 1 2 3 4
4 3 2 1 0 1 2 3 4
4 3 2 1 1 1 2 3 4
4 3 2 2 2 2 2 3 4
4 3 3 3 3 3 3 3 4
4 4 4 4 4 4 4 4 4 
```