package com.thehecklers.coffeeservice;

import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;

@SpringBootApplication
public class CoffeeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoffeeServiceApplication.class, args);
    }

}

@Controller
@RequiredArgsConstructor
class RSController {
    private final CoffeeService coffeeService;

    @MessageMapping("coffees")
    Flux<Coffee> supplyCoffees() {
        return coffeeService.getAllCoffees();
    }

    @MessageMapping("orders.{name}")
    Flux<CoffeeOrder> orders(@DestinationVariable String name) {
        System.out.println("hey im alive");
        return coffeeService.getCoffeeByName(name)
                .flatMapMany(coffee -> coffeeService.getOrdersForCoffee(coffee.getName()));
    }

}

@RestController
@RequiredArgsConstructor
@RequestMapping("/coffees")
class CoffeeController {
    private final CoffeeService coffeeService;

    @GetMapping
    Flux<Coffee> all() {
        return coffeeService.getAllCoffees();
    }

    @GetMapping("/{id}")
    Mono<Coffee> byId(@PathVariable String id) {
        return coffeeService.getCoffeeById(id);
    }

    @GetMapping(value = "/{id}/orders", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<CoffeeOrder> orders(@PathVariable String id) {
        return coffeeService.getOrdersForCoffee(id);
    }
}

@Service
@RequiredArgsConstructor
class CoffeeService {
    private final CoffeeRepository coffeeRepository;

    Flux<Coffee> getAllCoffees() {
        return coffeeRepository.findAll();
    }

    Mono<Coffee> getCoffeeById(String id) {
        return coffeeRepository.findById(id);
    }

    Mono<Coffee> getCoffeeByName(String name) {
        return coffeeRepository.findByName(name)
                .defaultIfEmpty(new Coffee("123456", "My Favorite Coffee"));
    }

    Flux<CoffeeOrder> getOrdersForCoffee(String coffeeId) {
        return Flux.interval(Duration.ofSeconds(1))
                .onBackpressureDrop()
                .map(aLong -> new CoffeeOrder(coffeeId, Instant.now()));
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
    Mono<Coffee> findByName(String name);
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
@AllArgsConstructor
@RequiredArgsConstructor
class Coffee {
    @Id
    private String id;
    @NonNull
    private String name;
}
