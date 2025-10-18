package basics;


import model.Employee;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Sorting {
    // sort by lambda
    public static void main(String[] args) {
        Sorting sortingProblem = new Sorting();
        sortingProblem.sortUsingLambda();
    }

    public void sortUsingLambda() {
        List<Employee> employeeList = Arrays.asList(
                new Employee(1, "George", 2000d),
                new Employee(2, "Robert", 4500d),
                new Employee(3, "Cathy", 3000d),
                new Employee(4, "Peter", 1500d)
        );

        // ascending order
        employeeList.sort(
                Comparator.comparing(Employee::getEmpName)
        );
        System.out.println("Ascending order");
        employeeList.forEach(System.out::println);
        // descending order
        employeeList.sort(
                Comparator.comparing(Employee::getEmpName).reversed()
        );
        System.out.println("Descending order");
        employeeList.forEach(System.out::println);

        employeeList.forEach(System.out::println);
        System.out.println("sort using streams");
        List<Employee> streamSorting = employeeList.stream().sorted(Comparator.comparing(Employee::getEmpName)).toList();
        streamSorting.forEach(System.out::println);

        // Using Collections static method
        System.out.println("Using Collections static method");
        employeeList.sort(Comparator.comparing(Employee::getEmpName).thenComparingDouble(Employee::getEmpSalary));
        employeeList.forEach(System.out::println);

    }
}