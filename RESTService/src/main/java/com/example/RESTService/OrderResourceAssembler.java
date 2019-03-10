package com.example.RESTService;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Component;

public class OrderResourceAssembler implements ResourceAssembler<Order, Resource<Order>> {

    @Override
    public Resource<Order> toResource(Order order) {

        //Unconditional links to single-item resource and aggregate root

        Resource<Order> orderResource = new Resource<>(order,
                linkTo(methodOn(OrderController.class).one(order.getId())).withSelfRel(),
                linkTo(methodOn(OrderController.class).all()).withRel("orders")
        );

        return orderResource;
    }
}
