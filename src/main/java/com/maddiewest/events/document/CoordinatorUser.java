package com.maddiewest.rentalservice.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "coordinator_users")
public class CoordinatorUser {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String name;

    private String role;
}
