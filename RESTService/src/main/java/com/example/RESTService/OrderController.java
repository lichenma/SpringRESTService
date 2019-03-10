package com.example.RESTService;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

@RestController
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderResourceAssembler assembler;

    OrderController(OrderRepository orderRepository, OrderResourceAssembler assembler) {
        this.orderRepository = orderRepository;
        this.assembler = assembler;
    }

    @GetMapping("/orders")
    Resources<Resource<Order>> all() {

        List<Resource<Order>> orders = orderRepository.findAll().stream()
                .map(assembler::toResource)
                .collect(Collectors.toList());

        return new Resources<>(orders,
                linkTo(methodOn(OrderController.class).all()).withSelfRel());
    }

    @GetMapping("/orders/{id}")
    Resource<Order> one(@PathVariable Long id) {

        return assembler.toResource(
                orderRepository.findById(id)
                    .orElseThrow(() -> new OrderNotFoundException(id)));
    }

    @PostMapping("/orders")
    ResponseEntity<Resource<Order>> newOrder(@RequestBody Order order) {

        order.setStatus(Status.IN_PROGRESS);
        Order newOrder = orderRepository.save(order);

        return ResponseEntity
                .created(linkTo(methodOn(OrderController.class).one(newOrder.getId())).toUri())
                .body(assembler.toResource(newOrder));
    }
}
