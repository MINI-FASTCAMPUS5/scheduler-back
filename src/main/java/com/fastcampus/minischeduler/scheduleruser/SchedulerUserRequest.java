package com.fastcampus.minischeduler.scheduleruser;

import com.fastcampus.minischeduler.scheduleradmin.SchedulerAdmin;
import com.fastcampus.minischeduler.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class SchedulerUserRequest {

    @Data
    @NoArgsConstructor
    public static class SchedulerUserRequestDto {

        @JsonIgnoreProperties({"hibernateLazyInitializer"})
        private User user;

        @JsonIgnoreProperties({"hibernateLazyInitializer"})
        private SchedulerAdmin schedulerAdmin;

        private LocalDateTime scheduleStart;
        private Progress progress;
        private LocalDateTime createdAt;
    }
}
