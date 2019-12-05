package tnt.pl.streamapi;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

@Slf4j
class StreamApiTests {

    @Data
    private static class Customer {
        private final String name;
        private final int age;
        private final String city;
        private Set<String> phones = new HashSet<>();
    }

    @Test
    void getStarted() {
        Collection<String> itemList = Arrays.asList("item1", "item2", "item3");

        itemList.removeIf(s -> s.endsWith("2"));

        Set<String> anotherList = itemList.stream()
                .map(s -> {
                    log.info("lolololo");
                    return s.toUpperCase();
                })
                .filter(s -> s.endsWith("2"))
                .collect(Collectors.toSet());

        log.info("{}", anotherList);
    }

    @Test
    void primitiveStream() {
        //works on primitives
        IntStream integerStream = IntStream.range(0, 10)
                .map(operand -> operand * 10);
        Stream<Object> integerStream2 = IntStream.range(0, 10)
                .mapToObj(operand -> new Object());

        Stream<Object> objectStream = IntStream.range(0, 10).mapToObj(operand -> new Object());
        Stream<Integer> integerBoxedStream = IntStream.range(0, 10).boxed();
    }

    @Test
    void orderOfExecution() {
        Collection<String> itemList = Arrays.asList("item1", "item2", "item3");
        itemList
                .stream()
                .map(s -> {
                    log.info("Uppercasing {}", s);
                    return s.toUpperCase();
                })
                .forEach(log::info);
    }

    @Test
    void shortCircuit() {
        Collection<String> itemList = Arrays.asList("item1", "item2", "item3");
        itemList.stream()
                .map(s -> {
                    log.info("Doing expensive mapping for {}", s);
                    return s.toUpperCase();
                })
                .anyMatch(s -> s.endsWith("1"));
    }

    @Test
    void shortCircuitV2() {
        int limit = 5;
        List<Object> entities = new ArrayList<>(new ArrayList<>());

        IntStream.range(0, 10)
                .anyMatch(id -> {
                    //some dao load(id) AND with condition, so you don't know if it will match
                    //entites.addAll(dao.load(id, ));

                    return entities.size() > limit;
                });
    }

    @Test
    void intermediateOpTest() {
        Collection<String> itemList = Arrays.asList("item1", "item2", "item3");
        Stream<String> aStream = itemList.stream();

        aStream.map(s -> {
            log.info("Mapping: {}", s);
            s = s.toUpperCase();
            return s;
        });
    }

    @Test
    void intermediateStatefulOp() {
        Collection<String> itemList = Arrays.asList("item1", "item2", "item3");
        itemList.stream()
                .sorted((left, right) -> {
                    log.info("Comparing {} to {}", left, right);
                    return left.compareTo(right);
                })
                .forEach(log::info);
    }

    @Test
    void reUsingTheStream() {
        Collection<String> itemList = Arrays.asList("item1", "item2", "item3");

        Stream<String> aStream = itemList.stream();

        Supplier<Stream<String>> streamSupplier = itemList::stream;

        //OK
        streamSupplier.get().forEach(log::info);
        streamSupplier.get().forEach(log::info);

        //Boom
        aStream.forEach(log::info);
        aStream.forEach(log::info);
    }

    @Test
    void distinct() {
        Collection<String> itemList =
                Arrays.asList("item1", "item2", "item3", "item3");

        itemList.stream()
                .distinct()
                .forEach(log::info);
    }

    @Test
    void collectorToDedupByField() {
        Collection<Customer> customers = Arrays.asList(
                new Customer("Brian", 23, "Warsaw"),
                new Customer("Ada", 27, "Berlin"),
                new Customer("Brian", 17, "Berlin"));

        Collection<Customer> dedupedByName = customers.stream()
                .collect(toMap(Customer::getName, item -> item, (l, r) -> l))
                .values();

        log.info("Deduped: {}", dedupedByName);
    }

    @Test
    void flatMap() {
        Collection<Customer> customers = Arrays.asList(
                new Customer("Brian", 23, "Warsaw"),
                new Customer("Ada", 27, "Berlin"),
                new Customer("Ada", 22, "Berlin"));

        customers.forEach(customer -> customer.getPhones().add("134134134"));

        boolean anyLandLinePhone = customers
                .stream()
                .flatMap(customer -> customer.getPhones().stream())
                .anyMatch(s -> s.startsWith("32"));

        //the same differently
        customers
                .stream()
                .map(Customer::getPhones)
                .flatMap(Collection::stream) //implicit parameter
                .anyMatch(s -> s.startsWith("32"));
    }

    @Test
    void groupBy() {
        Collection<Customer> customers = Arrays.asList(
                new Customer("Brian", 23, "Warsaw"),
                new Customer("Ada", 27, "Berlin"),
                new Customer("Ada", 22, "Berlin"));

        //simple case
        Map<String, List<Customer>> listOfCustomersByCity = customers.stream()
                .collect(groupingBy(Customer::getCity));

        //average age by city
        Map<String, Double> avgAgeByCity = customers.stream()
                .collect(groupingBy(Customer::getCity, averagingInt(Customer::getAge)));

        log.info("{}", avgAgeByCity);
    }

    @Test
    void partitionBy() {
        Collection<Customer> customers = Arrays.asList(
                new Customer("Brian", 23, "Warsaw"),
                new Customer("Ada", 27, "Berlin"),
                new Customer("Ada", 15, "Berlin"));

        Map<Boolean, List<Customer>> byMaturity = customers.stream()
                .collect(Collectors.partitioningBy(c -> c.getAge() >= 18));
    }

    @Test
    void reduce() {
        Collection<Customer> customers = Arrays.asList(
                new Customer("Brian", 23, "Warsaw"),
                new Customer("Ada", 27, "Berlin"));

        //example 1
        Optional<Customer> babyCustomer = customers.stream()
                .reduce((customer, customer2)
                        -> new Customer("Jessica",
                        0,
                        customer.getCity()));

        //example 2
        Optional<Customer> superCustomer = customers.stream()
                .reduce((customer, customer2) ->
                        new Customer(customer.getName().concat(customer2.getName()),
                                customer.getAge() + customer2.getAge() / 2,
                                "sewage"));
    }
}
