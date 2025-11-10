package dsa.tendays.challenge;

/*
    https://leetcode.com/problems/container-with-most-water/
 */
public class ContainerWithMostWater {
    static void main(String[] args) {
        int[] containerHeights = {1,8,6,2,5,4,8,3,7};
        System.out.println("Container with most water area: " + getMaxContainerArea(containerHeights));
    }
    static int getMaxContainerArea(int[] height) {
        int max = 0;
        int leftIndex = 0;
        int rightIndex = height.length - 1;

        while(leftIndex < rightIndex){
            /*
                area of rectangle = width * height
                base width is rightIndex - leftIndex
                water will spill if one height is shorter, so take the max's next min height to keep the water level same.
             */
            int containerWidth = rightIndex - leftIndex;
            int containerHeight = Math.min(height[leftIndex],  height[rightIndex]);
            int areaOfContainer = containerWidth * containerHeight;
            max = Math.max(max, areaOfContainer);

            if(height[leftIndex] < height[rightIndex]){
                leftIndex++;
            } else {
                rightIndex--;
            }
        }
        return max;
    }
}
