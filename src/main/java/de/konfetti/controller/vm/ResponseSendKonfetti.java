package de.konfetti.controller.vm;

import lombok.Data;

/**
 * Created by relampago on 10.10.16.
 */
@Data
public class ResponseSendKonfetti {
    private int resultCode = 0;
    private boolean transferedToAccount = false;
    private String response = "OK";
}
