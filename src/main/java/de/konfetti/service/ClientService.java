package de.konfetti.service;

import de.konfetti.data.Client;
import de.konfetti.data.User;

public interface ClientService {

    Client create(User user);

    Client findById(long client);
    
}