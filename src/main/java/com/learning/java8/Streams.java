package com.learning.java8;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Streams {
    public static void main(String[] args) {
        List<Person> persons = Arrays.asList(new Person(1, "Rodrigo Rodrigues", 35, Arrays.asList(new Child("Daniel", 2), new Child("Oliver", 2))),
                new Person(2, "Juninho", 37, Arrays.asList(new Child("Dan", 5), new Child("Iam", 3))),
                new Person(3, "Anonymous", 30, null));

        //Old way
        //Sort by age
        List<Person> copyPersons = new ArrayList<>(persons);
        Collections.sort(copyPersons, new Comparator<Person>() {
            @Override
            public int compare(Person p, Person p1) {
                return p.getAge() - p1.getAge();
            }
        });

        log.debug("old way - sort by age: {}", copyPersons);

        Person youngestPerson = null;
        Person oldestPerson = null;
        List<Person> personsWithoutChild = new ArrayList<>();
        Map<Integer, String> mapPersonsNames = new HashMap<>();

        for (Person person : persons) {
            if (youngestPerson == null && oldestPerson == null) {
                youngestPerson = person;
                oldestPerson = person;
            } else if (person.getAge() <= youngestPerson.getAge()) {
                youngestPerson = person;
            } else if (person.getAge() >= oldestPerson.getAge() ) {
                oldestPerson = person;
            }

            if (CollectionUtils.isEmpty(person.getChildren())) {
                personsWithoutChild.add(person);
            }

            mapPersonsNames.put(person.getId(), person.getName());
        }

        log.debug("old way - youngestPerson: {}", youngestPerson);
        log.debug("old way - oldestPerson: {}", oldestPerson);
        log.debug("old way - personsWithoutChild: {}", personsWithoutChild);
        log.debug("old way - mapPersons: {}", mapPersonsNames);
        //Old way

        //New way using Lambda + Streams
        //Sort by age
        copyPersons = new ArrayList<>(persons);
        copyPersons.sort(Comparator.comparing(Person::getAge));

        youngestPerson = persons.stream()
                .min(Comparator.comparing(Person::getAge))
                .get();

        oldestPerson = persons.stream()
                .max(Comparator.comparing(Person::getAge))
                .get();

        personsWithoutChild = persons.stream()
                .filter(p -> CollectionUtils.isEmpty(p.getChildren()))
                .collect(Collectors.toList());

        mapPersonsNames = persons.stream()
                .collect(Collectors.toMap(Person::getId, Person::getName));

        log.debug("--------------");
        log.debug("new way - sort by age: {}", copyPersons);
        log.debug("new way - youngestPerson: {}", youngestPerson);
        log.debug("new way - oldestPerson: {}", oldestPerson);
        log.debug("new way - personsWithoutChild: {}", personsWithoutChild);
        log.debug("new way - mapPersonsNames: {}", mapPersonsNames);
    }

    @Data
    @AllArgsConstructor
    static class Person {
        private Integer id;
        private String name;
        private int age;
        private List<Child> children;
    }

    @Data
    @AllArgsConstructor
    static class Child {
        private String name;
        private int age;
    }
}
