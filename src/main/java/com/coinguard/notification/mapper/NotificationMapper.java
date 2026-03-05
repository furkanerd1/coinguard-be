package com.coinguard.notification.mapper;

import com.coinguard.notification.dto.NotificationResponse;
import com.coinguard.notification.dto.PagedNotificationResponse;
import com.coinguard.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);

    List<NotificationResponse> toResponseList(List<Notification> notifications);

    @Named("toPagedResponse")
    default PagedNotificationResponse toPagedResponse(Page<Notification> page) {
        List<NotificationResponse> notifications = toResponseList(page.getContent());

        return new PagedNotificationResponse(
                notifications,
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}




