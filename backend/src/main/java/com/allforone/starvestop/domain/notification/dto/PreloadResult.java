package com.allforone.starvestop.domain.notification.dto;

import java.util.List;

public record PreloadResult(
        List<NotificationJobCreateDto> dtoList,
        long nextCursor
) {
    public boolean hasNext() {
        return nextCursor > 0 && !dtoList.isEmpty();
    }
}
