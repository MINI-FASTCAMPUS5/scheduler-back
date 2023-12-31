package com.fastcampus.minischeduler.scheduleruser;

import com.fastcampus.minischeduler.scheduleradmin.SchedulerAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchedulerUserRepository extends JpaRepository<SchedulerUser, Long> {

    List<SchedulerUser> findByUserId(Long userId);

    List<SchedulerUser> findBySchedulerAdmin(SchedulerAdmin schedulerAdmin);
}
