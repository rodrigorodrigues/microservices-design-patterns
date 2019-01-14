package com.learning.java8;

import com.learning.springboot.model.Child;
import com.learning.springboot.model.Person;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Streams {
    public static void main(String[] args) {
        List<Person> persons = new ArrayList<>();
        persons.add(Person.builder().fullName("Rodrigo Rodrigues")
                .dateOfBirth(LocalDate.of(1983, 1, 1))
                .children(Arrays.asList(new Child("Daniel", 2), new Child("Oliver", 2)))
                .build());

        persons.add(Person.builder().fullName("Juninho")
                .dateOfBirth(LocalDate.of(1981, 5, 10))
                .children(Arrays.asList(new Child("Dan", 5), new Child("Iam", 3)))
                .build());

        persons.add(Person.builder().fullName("Anonymous")
                .dateOfBirth(LocalDate.of(1985, 8, 25))
                .build());

        //Old way
        //Sort by age
        List<Person> copyPersons = new ArrayList<>(persons);
        Collections.sort(copyPersons, new Comparator<Person>() {
            @Override
            public int compare(Person p, Person p1) {
                return (int) (convertToTimestamp(p.getDateOfBirth()) - convertToTimestamp(p1.getDateOfBirth()));
            }
        });

        log.debug("old way - sort by age: {}", copyPersons);

        Person youngestPerson = null;
        Person oldestPerson = null;
        List<Person> personsWithoutChild = new ArrayList<>();
        Map<String, String> mapPersonsNames = new HashMap<>();

        for (Person person : persons) {
            if (youngestPerson == null && oldestPerson == null) {
                youngestPerson = person;
                oldestPerson = person;
            } else if (person.getDateOfBirth().isBefore(youngestPerson.getDateOfBirth()) ||  person.getDateOfBirth().isEqual(youngestPerson.getDateOfBirth())) {
                youngestPerson = person;
            } else if (person.getDateOfBirth().isAfter(oldestPerson.getDateOfBirth()) || person.getDateOfBirth().isEqual(oldestPerson.getDateOfBirth())) {
                oldestPerson = person;
            }

            if (CollectionUtils.isEmpty(person.getChildren())) {
                personsWithoutChild.add(person);
            }

            mapPersonsNames.put(person.getId(), person.getFullName());
        }

        log.debug("old way - youngestPerson: {}", youngestPerson);
        log.debug("old way - oldestPerson: {}", oldestPerson);
        log.debug("old way - personsWithoutChild: {}", personsWithoutChild);
        log.debug("old way - mapPersons: {}", mapPersonsNames);
        //Old way

        //New way using Lambda + Streams
        //Sort by age
        copyPersons = new ArrayList<>(persons);
        copyPersons.sort(Comparator.comparing(Person::getDateOfBirth));

        youngestPerson = persons.stream()
                .min(Comparator.comparing(Person::getDateOfBirth))
                .get();

        oldestPerson = persons.stream()
                .max(Comparator.comparing(Person::getDateOfBirth))
                .get();

        personsWithoutChild = persons.stream()
                .filter(p -> CollectionUtils.isEmpty(p.getChildren()))
                .collect(Collectors.toList());

        mapPersonsNames = persons.stream()
                .collect(Collectors.toMap(Person::getId, Person::getFullName));

        log.debug("--------------");
        log.debug("new way - sort by age: {}", copyPersons);
        log.debug("new way - youngestPerson: {}", youngestPerson);
        log.debug("new way - oldestPerson: {}", oldestPerson);
        log.debug("new way - personsWithoutChild: {}", personsWithoutChild);
        log.debug("new way - mapPersonsNames: {}", mapPersonsNames);
    }

    private static long convertToTimestamp(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant())
                .getTime();
    }
}
