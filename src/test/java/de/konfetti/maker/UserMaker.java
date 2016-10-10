package de.konfetti.maker;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;
import de.konfetti.data.User;

import static com.natpryce.makeiteasy.Property.newProperty;

/**
 * Created by relampago on 05.10.16.
 */
public class UserMaker {

    public static final Property<User, String> name = newProperty();
    public static final Property<User, String> email = newProperty();
    public static final Property<User, String> password = newProperty();

    public static final Instantiator<User> ExampleUser = new Instantiator<User>() {
        @Override
        public User instantiate(PropertyLookup<User> propertyLookup) {
            User user = new User();
            user.setName(propertyLookup.valueOf(name, "testName"));
            user.setEMail(propertyLookup.valueOf(email, "testusername@test.de"));
            user.setPassword(propertyLookup.valueOf(password, "testPassword"));
            return user;
        }
    };

}
