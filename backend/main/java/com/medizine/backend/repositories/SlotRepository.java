package com.medizine.backend.repositories;

import com.medizine.backend.dto.Slot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SlotRepository extends MongoRepository<Slot, String> {

    List<Slot> getAllByDoctorId(String doctorId);

    List<Slot> getAllByDoctorIdAndDate(String doctorId, LocalDate localDate);
}
