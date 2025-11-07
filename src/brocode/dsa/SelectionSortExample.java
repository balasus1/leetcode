package brocode.dsa;

public class SelectionSortExample {
    static void main(String[] args) {
        int[] array = {4,9,24,1,3,8,89,11};
        selectionSort(array);
        for(int a:array){
            System.out.print(a + " ");
        }
    }
    static void selectionSort(int[] array){
        for(int i=0;i<array.length-1;i++){
            int min=i;
            for(int j=i+1;j<array.length;j++){
                if(array[min] > array[j]){
                    min = j;
                }
            }
            swap(array, min, i);
        }
    }

    private static void swap(int[] array, int min, int i) {
        int temp= array[min];
        array[min]= array[i];
        array[i]=temp;
    }
}
