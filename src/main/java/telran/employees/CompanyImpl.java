package telran.employees;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

import org.json.JSONObject;

import telran.io.Persistable;

public class CompanyImpl implements Company, Persistable{
   private TreeMap<Long, Employee> employees = new TreeMap<>();
   private HashMap<String, List<Employee>> employeesDepartment = new HashMap<>();
   private TreeMap<Float, List<Manager>> managersFactor = new TreeMap<>();
private class CompanyIterator implements Iterator<Employee> {
    Iterator<Employee> iterator = employees.values().iterator();
    Employee lastIterated;
    @Override
    public boolean hasNext() {
       return iterator.hasNext();
    }

    @Override
    public Employee next() {
       lastIterated = iterator.next();
       return lastIterated;
    }
    @Override
    public void remove() {
       iterator.remove();
       removeFromIndexMaps(lastIterated);
    }
}
    @Override
    public Iterator<Employee> iterator() {
       return new CompanyIterator();
    }

    @Override
    public void addEmployee(Employee empl) {
        long id = empl.getId();
        if (employees.putIfAbsent(id, empl) != null) {
            throw new IllegalStateException("Already exists employee " + id);
        }
        addIndexMaps(empl);
    }

    private void addIndexMaps(Employee empl) {
       employeesDepartment.computeIfAbsent(empl.getDepartment(), k -> new ArrayList<>()).add(empl);
       if (empl instanceof Manager manager) {
            managersFactor.computeIfAbsent(manager.getFactor(), k -> new ArrayList<>()).add(manager);
       }
    }

    

    @Override
    public Employee getEmployee(long id) {
        return employees.get(id);
    }

    @Override
    public Employee removeEmployee(long id) {
        Employee empl = employees.remove(id);
        if(empl == null) {
            throw new NoSuchElementException("Not found employee " + id);
        }
        removeFromIndexMaps(empl);
        return empl;
    }


    private void removeFromIndexMaps(Employee empl) {
        removeIndexMap(empl.getDepartment(), employeesDepartment, empl);
        if (empl instanceof Manager manager) {
            removeIndexMap(manager.getFactor(), managersFactor, manager);
        }
    }

    private <K, V extends Employee> void removeIndexMap(K key, Map<K, List<V>> map, V empl) {
        List<V> list = map.get(key);
        list.remove(empl);
        if (list.isEmpty()) {
            map.remove(key);
        }
    }

    @Override
    public int getDepartmentBudget(String department) {
        return employeesDepartment.getOrDefault(department, Collections.emptyList())
        .stream().mapToInt(Employee::computeSalary).sum();
    }

    @Override
    public String[] getDepartments() {
        return employeesDepartment.keySet().stream().sorted().toArray(String[]::new);
    }

    @Override
    public Manager[] getManagersWithMostFactor() {
        Manager [] res = new Manager[0];
        if (!managersFactor.isEmpty()) {
            res = managersFactor.lastEntry().getValue().toArray(res);
        }
        return res;
    }

   @Override
public void saveToFile(String fileName) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
        for (Employee employee : employees.values()) {
            
            JSONObject json = new JSONObject();
            json.put("id", employee.getId());
            json.put("basicSalary", employee.getBasicSalary());
            json.put("department", employee.getDepartment());

            
            if (employee instanceof Manager manager) {
                json.put("factor", manager.getFactor());
            }
            

            writer.write(json.toString());
            writer.newLine();
        }
    } catch (IOException e) {
        throw new UncheckedIOException("Error saving company data", e);
    }
}
@Override
public void restoreFromFile(String fileName) {
    try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
        employees.clear();
        employeesDepartment.clear();
        managersFactor.clear();

        String line;
        while ((line = reader.readLine()) != null) {
            JSONObject json = new JSONObject(line);
            long id = json.getLong("id");
            int basicSalary = json.getInt("basicSalary");
            String department = json.getString("department");

            Employee employee;
            if (json.has("factor")) { // Indica que es un Manager
                float factor = json.getFloat("factor");
                employee = new Manager(id, basicSalary, department, factor);
            } else {
                employee = new Employee(id, basicSalary, department);
            }
            addEmployee(employee); // Agrega el empleado restaurado a la compañía
        }
    } catch (IOException e) {
        throw new UncheckedIOException("Error restoring company data", e);
    }
}
}