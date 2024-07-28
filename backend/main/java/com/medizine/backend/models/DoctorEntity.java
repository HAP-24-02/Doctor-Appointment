package com.medizine.backend.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "doctors")
@NoArgsConstructor
public class DoctorEntity {
  @Id
  private ObjectId id;

  private String firstName;

  private String lastName;

  private String emailAddress;

  private String password;

  private String phoneNumber;

  private String countryCode;
}
