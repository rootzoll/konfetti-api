package de.konfetti.controller.vm;

import lombok.Data;

/**
 * Created by relampago on 10.10.16.
 */
@Data
public class ResponseZip2Gps {
    private int resultCode = 0;
    private double lat = 0d;
    private double lon = 0d;
}
