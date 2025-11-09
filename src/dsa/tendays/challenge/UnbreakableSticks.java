package dsa.tendays.challenge;

import java.util.Scanner;

/**
 * https://codeforces.com/problemset/problem/2167/A
 *
 * A. Square?
 * time limit per test1 second
 * memory limit per test256 megabytes
 * You are given 4
 *  sticks of lengths a
 * , b
 * , c
 * , and d
 * . You can not break or bend them.
 *
 * Determine whether it is possible to form a square∗
 *  using the given sticks.
 *
 * ∗
 * A square is defined as a polygon consisting of 4
 *  vertices, of which all sides have equal length and all inner angles are equal. No two edges of the polygon may intersect each other.
 *
 * Input
 * The first line contains a single integer t
 *  (1≤t≤104
 * ) — the number of test cases.
 *
 * The only line of each test case contains four integers a
 * , b
 * , c
 * , and d
 *  (1≤a,b,c,d≤10
 * ) — the lengths of the sticks.
 *
 * Output
 * For each test case, print "YES" if it is possible to form a square using the given sticks, and "NO" otherwise.
 *
 * You may print each letter in any case (uppercase or lowercase). For example, the strings "yEs", "yes", "Yes", and "YES" will all be recognized as a positive answer.
 *
 * Example
 * InputCopy
 * 7
 * 1 2 3 4
 * 1 1 1 1
 * 2 2 2 2
 * 1 2 1 2
 * 1 1 5 5
 * 5 5 5 5
 * 4 10 5 9
 * OutputCopy
 * NO
 * YES
 * YES
 * NO
 * NO
 * YES
 * NO
 * Note
 * In the first test case, we can prove that we can't make a square.
 *
 * In the second, third, and sixth test cases, we can make a square like this:
 */
public class UnbreakableSticks {
    static void main() {
        int[][] testCases = {
                {1, 2, 3, 4},
                {1, 1, 1, 1},
                {2, 2, 2, 2},
                {1, 2, 1, 2},
                {1, 1, 5, 5},
                {5, 5, 5, 5},
                {4, 10, 5, 9}
        };
        System.out.println("Number of test cases: " + testCases.length);
        System.out.println("Results:");
        for (int i = 0; i < testCases.length; i++) {
            int a = testCases[i][0];
            int b = testCases[i][1];
            int c = testCases[i][2];
            int d = testCases[i][3];

            String result = (a == b && b == c && c == d) ? "YES" : "NO";
            System.out.println("Test case " + (i + 1) + ": " + a + " " + b + " " + c + " " + d + " -> " + result);
        }
    }
}
