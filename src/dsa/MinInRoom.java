package dsa;/*
    There are some people in a meeting hall, everyone is wearing a t-shirt in which a number is written on his t-shirt. we asked some of these people how many people other than themselves are wearing a t-shirt with the same number. in array asked, asked[i] means from ith person has been asked and that person gave answer for that.

    Given the array, return the mininum number of people in meeting hall.
    example-1: input: asked = [1,4,1], output: 7 explaination: 2 person said 1that can have same number lets say 0 t-shirt number. one person said 4 soin total 7 people (minimum) would be in hall.
    Example-2: input: asked = [30,30,30], output = 31
    constraints: 1 <= asked.length <= 5*10^4 0 <= asked[i] < 10^4

 */

public class MinInRoom {
    public static void main(String[] args) {
        // Test case 1: [1,4,1] should return 7
        int[] test1 = { 1, 4, 1 };
        System.out.println("Test case 1: " + minInRoom(test1)); // Expected: 7

        // Test case 2: [30,30,30] should return 31
        int[] test2 = { 30, 30, 30 };
        System.out.println("Test case 2: " + minInRoom(test2)); // Expected: 31
        int[] test3 = { 2, 5, 6, 8, 9 };
        System.out.println("Test case 3: " + minInRoom(test3)); // Expected: 31
    }

    public static int minInRoom(int[] asked) {

        // Edge case: array is empty
        if (asked == null || asked.length == 0) {
            return 0;
        }

        int maxNeeded = 0;

        // for each person response
        for( int response : asked) {
            // we need response + 1 person who have answered to be included
            maxNeeded = Math.max(maxNeeded, response + 1);
        }
        return Math.max(asked.length, maxNeeded);
    }
}
