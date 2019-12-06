package com.thehecklers.coffeeservice;

import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.beans.ConstructorProperties;
import java.time.Instant;

@SpringBootApplication
public class CoffeeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoffeeServiceApplication.class, args);
    }

}

@Component
@RequiredArgsConstructor
class DataLoader {
    private final CoffeeRepository coffeeRepository;

    @PostConstruct
    void loadData() {
        coffeeRepository.deleteAll().thenMany(
                Flux.just("Cafe Cereza", "Don Pablo", "Sumatra", "Starbucks", "CoffeeBeans", "Kona", "PREFER")
                        .map(Coffee::new)
                        .flatMap(coffeeRepository::save)
        )
                .thenMany(coffeeRepository.findAll())
                .subscribe(System.out::println);
    }
}

interface CoffeeRepository extends ReactiveCrudRepository<Coffee, String> {

}

@Data
@NoArgsConstructor
@AllArgsConstructor
class CoffeeOrder {
    private String coffeeId;
    private Instant whenOrdered;
}

@Document
@Data
@NoArgsConstructor
@RequiredArgsConstructor
class Coffee {
    @Id
    private String id;
    @NonNull
    private String name;
}
