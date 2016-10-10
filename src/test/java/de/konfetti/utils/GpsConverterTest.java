package de.konfetti.utils;

import de.konfetti.controller.vm.ResponseZip2Gps;
import org.junit.Test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by relampago on 10.10.16.
 */
public class GpsConverterTest {

    @Test
    public void fromZipCode() throws Exception {
        GpsConverter gpsConverter = new GpsConverter();
        ResponseZip2Gps responseZip2Gps = gpsConverter.fromZipCode("Germany", "10961");
        assertThat(responseZip2Gps, notNullValue());
    }

}