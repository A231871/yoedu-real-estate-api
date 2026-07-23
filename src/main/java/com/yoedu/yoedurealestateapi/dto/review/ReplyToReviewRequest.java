package com.yoedu.yoedurealestateapi.dto.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReplyToReviewRequest {

  @NotBlank(message = "reply text is required")
  @Size(max = 2000, message = "reply must be at most 2000 characters")
  private String reply;
}
