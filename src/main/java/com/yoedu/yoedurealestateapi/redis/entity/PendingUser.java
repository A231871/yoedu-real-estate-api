package com.yoedu.yoedurealestateapi.redis.entity;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("PendingUser")
public class PendingUser {

    @Id
    private UUID id;

    private String email;

    private String passwordHash;

    private String fullName;

    private String phone;
}
