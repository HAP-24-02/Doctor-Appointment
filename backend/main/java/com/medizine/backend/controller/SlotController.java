package com.medizine.backend.controller;

import com.medizine.backend.dto.Appointment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.time.LocalDate;
import java.time.LocalTime;
import com.medizine.backend.dto.Slot;
import com.medizine.backend.exchanges.BaseResponse;
import com.medizine.backend.exchanges.SlotBookingRequest;
import com.medizine.backend.exchanges.SlotResponse;
import com.medizine.backend.exchanges.SlotStatusRequest;
import com.medizine.backend.repositories.SlotRepository;
import com.medizine.backend.repositoryservices.SlotRepositoryService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping(UserController.BASE_API_ENDPOINT + "/slot")
@Log4j2
@Validated
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SlotController {

  @Autowired
  private SlotRepositoryService slotService;
  @Autowired
  private SlotRepositoryService slotRepository;
  private SlotRepository slotRepositoryy;
  @ApiOperation(value = "Create new slot", response = Slot.class)
  @ApiResponses({
          @ApiResponse(code = 200, message = "OK"),
          @ApiResponse(code = 400, message = "Bad Request")
  })
  private static boolean isSlotOverlap(List<LocalTime[]> timeSlots, Slot slot) {

    timeSlots.add(new LocalTime[]{
            getLocalTimeFromDate(slot.getStartTime()),
            getLocalTimeFromDate(slot.getEndTime())
    });

    timeSlots.sort(Comparator.comparing(time -> time[0]));

    // Now in the sorted timeSlots list, if start time of a slot
    // is less than end of previous slot, then there is an overlap.

    for (int i = 1; i < timeSlots.size(); i++) {
      if (timeSlots.get(i - 1)[1].compareTo(timeSlots.get(i)[0]) > 0)
        return true;
    }
    return false;
  }
  private static LocalTime getLocalTimeFromDate(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    int hours = calendar.get(Calendar.HOUR_OF_DAY);
    int minutes = calendar.get(Calendar.MINUTE);
    int seconds = calendar.get(Calendar.SECOND);
    return LocalTime.of(hours, minutes, seconds);
  }

  public static LocalDate getLocalDate(Date date) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata")); // Set the desired time zone (IST)
    String formattedDate = dateFormat.format(date);
    return LocalDate.parse(formattedDate);
  }

  @PutMapping("/create")
  public ResponseEntity<BaseResponse<Slot>> createNewSlot(Slot slot) {
    try {
      // Start time must be less than the end time.
      if (slot.getStartTime().compareTo(slot.getEndTime()) >= 0) {
        return ResponseEntity.badRequest().body(new BaseResponse<>(null, "End Time must be greater than Start time"));
      }

      // Slot duration must be between 30 to 50 minutes.
      long slotDurationInMillis = slot.getEndTime().getTime() - slot.getStartTime().getTime();
      long slotDurationInMinutes = Duration.ofMillis(slotDurationInMillis).toMinutes();
      if (slotDurationInMinutes < 30 || slotDurationInMinutes > 50) {
        return ResponseEntity.badRequest().body(new BaseResponse<>(null, "Slot duration must be between 30 to 50 minutes"));
      }

      String currentDoctorId = slot.getDoctorId();
      List<Slot> allSlotOfGivenDoctor = slotRepository.getAllByDoctorId(currentDoctorId);

      List<LocalTime[]> timeSlots = new ArrayList<>();

      // Check if the current slot overlaps with other already added slots.
      for (Slot currentSlot : allSlotOfGivenDoctor) {
        LocalTime localStartTime = getLocalTimeFromDate(currentSlot.getStartTime());
        LocalTime localEndTime = getLocalTimeFromDate(currentSlot.getEndTime());
        timeSlots.add(new LocalTime[]{localStartTime, localEndTime});
      }

      // Check if the new slot overlaps with any existing slots
      if (isSlotOverlap(timeSlots, slot)) {
        return ResponseEntity.badRequest().body(new BaseResponse<>(null, "Error, Overlapping Slot!"));
      } else {
        // Save the slot after parsing the date strings into Date objects
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Set the desired time zone (UTC)
        Date startTime = dateFormat.parse(String.valueOf(slot.getStartTime()));
        Date endTime = dateFormat.parse(String.valueOf(slot.getEndTime()));
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);

        Slot createdSlot = slotRepositoryy.save(slot);
        return ResponseEntity.ok().body(new BaseResponse<>(createdSlot, "Slot Created Successfully"));
      }
    } catch (Exception e) {
      log.error("Error occurred while creating a new slot: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(new BaseResponse<>(null, "Internal Server Error"));
    }
  }





  @ApiOperation(value = "Get all slots by doctor Id", response = SlotResponse.class)
  @ApiResponses({
          @ApiResponse(code = 200, message = "OK"),
          @ApiResponse(code = 400, message = "Bad Request"),
          @ApiResponse(code = 500, message = "Server Error")
  })
  @GetMapping("/getAllByDocId")
  public SlotResponse getAllSlotByDoctorId(@Valid @RequestParam String doctorId) {
    List<Slot> slots = slotService.getAllByDoctorId(doctorId);
    if (slots == null || slots.size() == 0) {
      return new SlotResponse(null, "NOT FOUND");
    } else {
      return new SlotResponse(slots, "FOUND");
    }
  }

  @ApiOperation(value = "Get Slot Live Status", response = SlotResponse.class)
  @ApiResponses({
          @ApiResponse(code = 200, message = "OK"),
          @ApiResponse(code = 400, message = "Bad Request"),
          @ApiResponse(code = 500, message = "Server Error")
  })
  @GetMapping("/liveSlotStatus")
  public SlotResponse getSlotLiveStatus(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date date,
                                        @RequestParam String doctorId,
                                        @RequestParam String userId) {

    SlotStatusRequest slotStatusRequest = new SlotStatusRequest(doctorId, userId, date);
    return slotService.getLiveSlotStatus(slotStatusRequest);
  }

  @ApiOperation(value = "Get all the slots", response = SlotResponse.class)
  @ApiResponses({
          @ApiResponse(code = 200, message = "OK"),
          @ApiResponse(code = 400, message = "Bad Request"),
          @ApiResponse(code = 500, message = "Server Error")
  })
  @GetMapping("/getAll")
  public SlotResponse getAllSlot() {
    List<Slot> slotList = slotService.getAll();
    return new SlotResponse(slotList, "DONE");
  }

  @ApiOperation(value = "Book slot for a given date", response = Appointment.class)
  @ApiResponses({
          @ApiResponse(code = 200, message = "Appointment Book Successfully"),
          @ApiResponse(code = 400, message = "Bad Request"),
          @ApiResponse(code = 500, message = "Server Error")
  })
  @PatchMapping("/book")
  public BaseResponse<Appointment> bookSlotByPatientId(@Valid @RequestBody SlotBookingRequest slotRequest) {

    ResponseEntity<?> response = slotService.bookSlot(slotRequest);
    if (response.getStatusCode().is2xxSuccessful()) {
      return new BaseResponse<>((Appointment) response.getBody(),
          response.getStatusCode().toString());
    } else {
      return new BaseResponse<>(null, response.getStatusCode().toString());
    }
  }

  @ApiOperation(value = "Delete Slot by id", response = SlotResponse.class)
  @ApiResponses({
      @ApiResponse(code = 200, message = "OK"),
      @ApiResponse(code = 400, message = "Bad Request"),
      @ApiResponse(code = 500, message = "Server Error")
  })
  @DeleteMapping("/deleteById")
  public BaseResponse<Slot> deleteSlotById(@RequestParam String slotId) {

    Slot deletedSlot = slotService.deleteSlotById(slotId);
    if (deletedSlot == null) {
      return new BaseResponse<>(null, "Bad Request");
    } else {
      return new BaseResponse<>(deletedSlot, "OK");
    }
  }
    @GetMapping("/doctorIdDate")
    public ResponseEntity<List<Slot>> getSlotsByDoctorIdAndDate(
            @RequestParam String doctorId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {

        List<Slot> slots = slotService.getSlotsByDoctorIdAndDate(doctorId, date);

        if (slots.isEmpty()) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.ok(slots);
        }
    }

}
