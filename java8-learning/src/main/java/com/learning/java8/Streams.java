package com.learning.java8;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Streams {
    public static void main(String[] args) {
        List<PersonTest> persons = new ArrayList<>();
        persons.add(PersonTest.builder().fullName("Rodrigo Rodrigues")
                .dateOfBirth(LocalDate.of(1983, 1, 1))
                .children(Arrays.asList(new ChildTest("Daniel", 2), new ChildTest("Oliver", 2)))
                .build());

        persons.add(PersonTest.builder().fullName("Juninho")
                .dateOfBirth(LocalDate.of(1981, 5, 10))
                .children(Arrays.asList(new ChildTest("Dan", 5), new ChildTest("Iam", 3)))
                .build());

        persons.add(PersonTest.builder().fullName("Anonymous")
                .dateOfBirth(LocalDate.of(1985, 8, 25))
                .build());

        //Old way
        //Sort by age
        List<PersonTest> copyPersons = new ArrayList<>(persons);
        Collections.sort(copyPersons, new Comparator<PersonTest>() {
            @Override
            public int compare(PersonTest p, PersonTest p1) {
                return (int) (convertToTimestamp(p.getDateOfBirth()) - convertToTimestamp(p1.getDateOfBirth()));
            }
        });

        log.debug("old way - sort by age: {}", copyPersons);

        PersonTest youngestPerson = null;
        PersonTest oldestPerson = null;
        List<PersonTest> personsWithoutChild = new ArrayList<>();
        Map<String, String> mapPersonsNames = new HashMap<>();

        for (PersonTest person : persons) {
            if (youngestPerson == null && oldestPerson == null) {
                youngestPerson = person;
                oldestPerson = person;
            } else if (person.getDateOfBirth().isBefore(youngestPerson.getDateOfBirth()) ||  person.getDateOfBirth().isEqual(youngestPerson.getDateOfBirth())) {
                youngestPerson = person;
            } else if (person.getDateOfBirth().isAfter(oldestPerson.getDateOfBirth()) || person.getDateOfBirth().isEqual(oldestPerson.getDateOfBirth())) {
                oldestPerson = person;
            }

            if (person.getChildren() == null || person.getChildren().isEmpty()) {
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
        copyPersons.sort(Comparator.comparing(PersonTest::getDateOfBirth));

        youngestPerson = persons.stream()
                .min(Comparator.comparing(PersonTest::getDateOfBirth))
                .get();

        oldestPerson = persons.stream()
                .max(Comparator.comparing(PersonTest::getDateOfBirth))
                .get();

        personsWithoutChild = persons.stream()
                .filter(p -> p.getChildren() == null || p.getChildren().isEmpty())
                .collect(Collectors.toList());

        mapPersonsNames = persons.stream()
                .collect(Collectors.toMap(PersonTest::getId, PersonTest::getFullName));

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


@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
class PersonTest {
    private String id;

    private String fullName;

    private LocalDate dateOfBirth;

    private List<ChildTest> children;

    private AddressTest address;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class ChildTest {
    private String name;
    private Integer age;
}

@Data
@NoArgsConstructor
class AddressTest {
    private String id = UUID.randomUUID().toString();
    private String address;
    private String city;
    private String stateOrProvince;
    private String country;
    private String postalCode;
}