package de.konfetti.controller.vm;

import de.konfetti.data.ClientAction;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by relampago on 10.10.16.
 */
@Data
public class RedeemResponse {
    private List<ClientAction> actions = new ArrayList<ClientAction>();;
    private String feedbackHtml;
}
