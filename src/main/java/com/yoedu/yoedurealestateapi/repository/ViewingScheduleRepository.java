package com.yoedu.yoedurealestateapi.repository;





import com.yoedu.yoedurealestateapi.domain.entities.ViewingSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ViewingScheduleRepository extends JpaRepository<ViewingSchedule, UUID> {
}
