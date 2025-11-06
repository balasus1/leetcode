package dsa;

/*
 * excel-sheet-column-title - LeetCode
 * https://leetcode.com/problems/excel-sheet-column-title/
 * Problem statement: 
 * ------------------
    Given an integer columnNumber, return its corresponding column title as it appears in an Excel sheet.

    For example:

    A -> 1
    B -> 2
    C -> 3
    ...
    Z -> 26
    AA -> 27
    AB -> 28 
    ...
 */
public class ExcelSheetColumnTitle {
    public static void main(String[] args) {
        System.out.print(convertToTitle(1));
        System.out.print(convertToTitle(2));
        System.out.print(convertToTitle(3));
        System.out.print(convertToTitle(26));
        System.out.print(convertToTitle(27));
        System.out.print(convertToTitle(28));
    }

    public static String convertToTitle(int n) {
        StringBuilder columnTitle = new StringBuilder();
        while (n > 0) {
            n--;
            columnTitle.append((char) ('A' + (n % 26)));
            n /= 26;
        }

        return columnTitle.reverse().toString();
    }
}
