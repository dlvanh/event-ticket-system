package com.example.event_ticket_system.service;

import com.example.event_ticket_system.entity.Event;
import com.example.event_ticket_system.enums.ApprovalStatus;
import com.example.event_ticket_system.enums.EventStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> byOrganizer(Integer organizerId,
                                                   String status,
                                                   String approveStatus,
                                                   LocalDateTime startTime,
                                                   LocalDateTime endTime,
                                                   String name) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizer").get("id"), organizerId));
            addStatusFilter(predicates, root, cb, status);
            addApprovalStatusFilter(predicates, root, cb, approveStatus);
            addTimeOverlapFilter(predicates, root, cb, startTime, endTime);
            addNameFilter(predicates, root, cb, name);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Event> forRecommendations(String category,
                                                          String address,
                                                          LocalDateTime startTime,
                                                          LocalDateTime endTime,
                                                          String name) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("approvalStatus"), ApprovalStatus.approved));
            CriteriaBuilder.In<EventStatus> statusIn = cb.in(root.get("status"));
            statusIn.value(EventStatus.completed);
            statusIn.value(EventStatus.upcoming);
            predicates.add(statusIn);

            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            addAddressFilter(predicates, root, cb, address, true);
            addTimeOverlapFilter(predicates, root, cb, startTime, endTime);
            addNameFilter(predicates, root, cb, name);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Event> forAdminList(String status,
                                                    String approvalStatus,
                                                    String address,
                                                    LocalDateTime startTime,
                                                    LocalDateTime endTime,
                                                    String name) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            addAddressFilter(predicates, root, cb, address, false);
            addTimeOverlapFilter(predicates, root, cb, startTime, endTime);
            addNameFilter(predicates, root, cb, name);
            addStatusFilter(predicates, root, cb, status);
            addApprovalStatusFilter(predicates, root, cb, approvalStatus);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addStatusFilter(List<Predicate> predicates,
                                        jakarta.persistence.criteria.Root<Event> root,
                                        CriteriaBuilder cb,
                                        String status) {
        if (status != null && !status.isBlank()) {
            try {
                predicates.add(cb.equal(root.get("status"), EventStatus.valueOf(status.trim().toLowerCase(Locale.ROOT))));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid event status: " + status);
            }
        }
    }

    private static void addApprovalStatusFilter(List<Predicate> predicates,
                                                jakarta.persistence.criteria.Root<Event> root,
                                                CriteriaBuilder cb,
                                                String approvalStatus) {
        if (approvalStatus != null && !approvalStatus.isBlank()) {
            try {
                predicates.add(cb.equal(root.get("approvalStatus"),
                        ApprovalStatus.valueOf(approvalStatus.trim().toLowerCase(Locale.ROOT))));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid approval status: " + approvalStatus);
            }
        }
    }

    private static void addNameFilter(List<Predicate> predicates,
                                      jakarta.persistence.criteria.Root<Event> root,
                                      CriteriaBuilder cb,
                                      String name) {
        if (name != null && !name.isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("eventName")), "%" + name.trim().toLowerCase(Locale.ROOT) + "%"));
        }
    }

    private static void addTimeOverlapFilter(List<Predicate> predicates,
                                             jakarta.persistence.criteria.Root<Event> root,
                                             CriteriaBuilder cb,
                                             LocalDateTime startTime,
                                             LocalDateTime endTime) {
        if (startTime != null && endTime != null) {
            predicates.add(cb.and(
                    cb.lessThanOrEqualTo(root.get("startTime"), endTime),
                    cb.greaterThanOrEqualTo(root.get("endTime"), startTime)
            ));
        } else {
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startTime"), endTime));
            }
        }
    }

    private static void addAddressFilter(List<Predicate> predicates,
                                         jakarta.persistence.criteria.Root<Event> root,
                                         CriteriaBuilder cb,
                                         String address,
                                         boolean handleOtherLocation) {
        if (address == null || address.isBlank()) {
            return;
        }
        if (handleOtherLocation && "Vị trí khác".equalsIgnoreCase(address.trim())) {
            predicates.add(cb.not(cb.or(
                    cb.like(cb.lower(root.get("ward").get("district").get("province").get("name")), "%hồ chí minh%"),
                    cb.like(cb.lower(root.get("ward").get("district").get("province").get("name")), "%hà nội%"),
                    cb.like(cb.lower(root.get("ward").get("district").get("province").get("name")), "%đà lạt%")
            )));
            return;
        }
        String pattern = "%" + address.trim().toLowerCase(Locale.ROOT) + "%";
        predicates.add(cb.or(
                cb.like(cb.lower(root.get("addressName")), pattern),
                cb.like(cb.lower(root.get("addressDetail")), pattern),
                cb.like(cb.lower(root.get("ward").get("name")), pattern),
                cb.like(cb.lower(root.get("ward").get("district").get("name")), pattern),
                cb.like(cb.lower(root.get("ward").get("district").get("province").get("name")), pattern)
        ));
    }
}
