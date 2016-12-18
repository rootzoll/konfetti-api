package de.konfetti.controller;

import de.konfetti.controller.mapper.UserMapper;
import de.konfetti.controller.vm.*;
import de.konfetti.data.*;
import de.konfetti.service.*;
import de.konfetti.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static de.konfetti.data.enums.SendKonfettiModeEnum.SENDKONFETTIMODE_DISABLED;
import static de.konfetti.data.enums.SendKonfettiModeEnum.SENDKONFETTIMODE_JUSTEARNED;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(UserController.REST_API_MAPPING)
public class UserController {

    public static final String REST_API_MAPPING = "konfetti/api/account";

    private final UserService userService;
    private final ClientService clientService;
    private final AccountingService accountingService;
    private final PartyService partyService;
    private final CodeService codeService;

    @Value("${security.passwordsalt}")
    private String passwordSalt;

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.enabled}")
    private Boolean mailEnabled;

    @Value("${konfetti.api.cheatcodes.enabled}")
    private Boolean cheatCodesEnabled;

    @Autowired
    private ControllerSecurityHelper controllerSecurityHelper;

    @Autowired
    private EMailManager mailService;

    private UserMapper userMapper;

    @Autowired
    public UserController(final UserService userService, final ClientService clientService, final AccountingService accountingService, final PartyService partyService, final CodeService codeService) {
        this.userService = userService;
        this.clientService = clientService;
        this.accountingService = accountingService;
        this.partyService = partyService;
        this.codeService = codeService;
        userMapper = new UserMapper();
    }

    @PostConstruct
    public void init() {
        if ((this.passwordSalt == null) || (this.passwordSalt.trim().length() == 0))
            throw new RuntimeException("security.passwordsalt is not set in property file");
        this.passwordSalt = this.passwordSalt.trim();
    }

    //---------------------------------------------------
    // USER Controller
    //---------------------------------------------------

    @CrossOrigin(origins = "*")
    @GetMapping(produces = "application/json")
    public List<UserResponse> getAllUsers(HttpServletRequest httpRequest) throws Exception {
        controllerSecurityHelper.checkAdminLevelSecurity(httpRequest);
        List<UserResponse> listOfUserResponses = userService.getAllUsers()
                .stream().map(user -> userMapper.fromUserToUserResponse(user))
                .collect(Collectors.toList());
        return listOfUserResponses;
    }

    @CrossOrigin(origins = "*")
    @PostMapping(produces = "application/json")
    public ResponseEntity<UserResponse> createUser(
            @RequestParam(value = "mail", defaultValue = "") String email,
            @RequestParam(value = "pass", defaultValue = "") String pass,
            @RequestParam(value = "locale", defaultValue = "en") String locale) throws Exception {

        boolean createWithCredentials = false;
        if ((email != null) && (email.length() > 1)) {

            // check if credentials are available
            if ((pass == null) || (pass.trim().length() == 0)) {
                throw new Exception("password needs to be set");
            }
            pass = pass.trim();
            createWithCredentials = true;

            // if email is set - check if email exists on other account
            if (userService.findByMailIgnoreCase(email) != null) {
                return new ResponseEntity("\"User already exists with this email\"", HttpStatus.BAD_REQUEST);
            }

        }

        // TODO: remove when anonymouse login is allowed again -> what about email and username integrity on database
        //       then? craete UUIDs for temporary users until they register..?
        if (!createWithCredentials) {
            return new ResponseEntity("\"Anonymous Login not allowed, user email to register\"", HttpStatus.BAD_REQUEST);
        }

        // create new user
        User user = userService.create(email, pass, locale);

        if (createWithCredentials) {
            // TODO --> email multi lang by lang set in user
            try {
                if (!mailService.sendMail(email, "rest.user.created.subject", "username: " + email + "\npass: " + pass + "\n\nkeep email or write password down", null, user.getSpokenLangs())) {
                    log.warn("was not able to send eMail on account creation to(" + email + ")");
                }
            } catch (Exception e) {
                // TODO check why there gets an exception thrown later
                log.warn("EXCEPTION was not able to send eMail on account creation to(" + email + ")");
            }
        }

        // create new client
        log.info("create new client");
        Client client = clientService.create(user);
        user.getClients().add(client);

        log.info("update user");
        userService.update(user);

        log.info("mapping user response");
        UserResponse userResponse = userMapper.fromUserToUserResponse(user);
        userResponse.setClientId(client.getId());
        userResponse.setClientSecret(client.getSecret());
        
        log.info("return");
        return new ResponseEntity<UserResponse>(userResponse, HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/{userId}", produces = "application/json")
    public UserResponse getUser(@PathVariable Long userId, HttpServletRequest httpRequest) throws Exception {

        User user = userService.findById(userId);
        if (user == null) {
            log.warn("NOT FOUND user(" + userId + ")");
            // 0 --> signal, that client auth failed
            return new UserResponse(0L);
        }

        // check if user is allowed to read
        if (httpRequest.getHeader("X-CLIENT-ID") != null) {
            // A) check that user is himself
            Client client;
            try {
                client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
            } catch (Exception e) {
                log.warn("Exception on getUser (get client): " + e.getMessage());
                // 0 --> signal, that client auth failed
                return new UserResponse(0L);
            }

            if (!client.getUser().getId().equals(user.getId()))
                throw new Exception("client(" + client.getId() + ") is not allowed to read user(" + userId + ")");

            // update activity on user
            if (!user.wasUserActiveInLastMinutes(1)) {
                log.info("Updating ActivityTS of user(" + user.getId() + ")");
                user.setLastActivityTS(System.currentTimeMillis());
                userService.update(user);
            }
        } else {
            // B) check for trusted application with administrator privilege
            controllerSecurityHelper.checkAdminLevelSecurity(httpRequest);
        }

        // keep password hash just on server side
        user.setPassword("");
        return userMapper.fromUserToUserResponse(user);
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/login", produces = "application/json")
    public UserResponse login(@RequestParam(value = "mail", defaultValue = "") String email,
                              @RequestParam(value = "pass", defaultValue = "") String pass) throws Exception {

        pass = pass != null? pass.trim(): pass;

        // check user and input data
        User user = userService.findByMailIgnoreCase(email);
        if (user == null) {
            log.warn("LOGIN FAIL: user not found with mail(" + email + ")");
            throw new Exception("User and/or Passwort not valid.");
        }
        if ((pass == null) || (pass.length() == 0)) {
            log.warn("LOGIN FAIL: password is null or zero length");
            throw new Exception("User and/or Passwort not valid.");
        }

        // check password
        String passMD5 = Helper.hashPassword(this.passwordSalt, pass);
        if (!passMD5.equals(user.getPassword())) {
            log.warn("LOGIN FAIL: given passwordMD5(" + passMD5 + ") is not passwordMD5 on user (" + user.getPassword() + ")");
            throw new Exception("User and/or Passwort not valid.");
        }

        // create new client for session
        Client client = clientService.create(user);
        user.getClients().add(client);

        // update activity on user
        log.info("Updating ActivityTS of user(" + user.getId() + ")");
        user.setLastActivityTS(System.currentTimeMillis());
        userService.update(user);

        UserResponse userResponse = userMapper.fromUserToUserResponse(user);
        // set client data on user and return
        userResponse.setClientId(client.getId());
        userResponse.setClientSecret(client.getSecret());
        return userResponse;
    }

    /**
     * POST   /account/reset_password/init : Send an e-mail to reset the password of the user
     *
     * @param mail    the mail of the user
     * @param request the HTTP request
     * @return the ResponseEntity with status 200 (OK) if the e-mail was sent, or status 400 (Bad Request) if the e-mail address is not registered
     */
    @CrossOrigin(origins = "*")
    @PostMapping(value = "/reset_password/init",
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> requestPasswordReset(@RequestBody String mail, HttpServletRequest request) {
        return userService.requestPasswordReset(mail)
                .map(user -> {
                    mailService.sendPasswordResetMail(user);
                    return new ResponseEntity<>("e-mail was sent", HttpStatus.OK);
                }).orElse(new ResponseEntity<>("e-mail address not registered", HttpStatus.BAD_REQUEST));
    }

    /**
     * POST   /account/reset_password/finish : Finish to reset the password of the user
     *
     * @param keyAndPassword the generated key and the new password
     * @return the ResponseEntity with status 200 (OK) if the password has been reset,
     * or status 400 (Bad Request) or 500 (Internal Server Error) if the password could not be reset
     */
    @CrossOrigin(origins = "*")
    @PostMapping(value = "/reset_password/finish", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        return userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey())
                .map(user -> new ResponseEntity<String>(HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    // use to quickly check if a user name is already in use - no auth needed
    @CrossOrigin(origins = "*")
    @GetMapping(value = "/check_free", produces = "application/json")
    public Boolean checkUserNameStillFree(@RequestParam(value = "username", defaultValue = "") String name) {
    	if ((name==null) || (name.length()==0)) return false;
    	return (userService.findByName(name)==null);
    }

    @CrossOrigin(origins = "*")
    @PutMapping(value = "/{userId}", produces = "application/json")
    public ResponseEntity<UserResponse> updateUser(@RequestBody @Valid final User userInput, HttpServletRequest httpRequest) throws Exception {
        User user = userService.findById(userInput.getId());
        if (user == null) throw new Exception("NOT FOUND user(" + userInput.getId() + ")");

        // check if other user exists with name / email
        User byMail = userService.findByMailIgnoreCase(userInput.getEMail());
        if (byMail != null && byMail.getId() != userInput.getId()) {
            return new ResponseEntity("Another user exists with this email : '" + userInput.getEMail() + "'", HttpStatus.BAD_REQUEST);
        }
        User byName = userService.findByName(userInput.getName());
        if (byName != null
                && byName.getId() != userInput.getId()
                && userInput.getName().length() > 0
                ) {
            return new ResponseEntity("Another user exists with this name : '" + userInput.getName() + "'", HttpStatus.BAD_REQUEST);
        }

        // check if user is allowed to read
        if (httpRequest.getHeader("X-CLIENT-ID") != null) {

            // A) check that user is himself
            Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
            if (!client.getUser().getId().equals(user.getId()))
                throw new Exception("client(" + client.getId() + ") is not allowed to read user(" + user.getId() + ")");

            // B) check if email got changed
            boolean firstTimeMailSet = (user.getEMail() == null) || (user.getEMail().trim().length() == 0);
            if ((userInput.getEMail() != null) && (!userInput.getEMail().equals(user.getEMail()))) {
                user.setEMail(userInput.getEMail());
                String pass = RandomUtil.generadeCodeNumber() + "";
                user.setPassword(Helper.hashPassword(this.passwordSalt, pass));
                if (firstTimeMailSet) {
                    // TODO multi lang eMail text by lang in user object - use same text as on account created with email
                    mailService.sendMail(userInput.getEMail(), "rest.user.created.subject", "username: " + user.getEMail() + "\npass: " + pass + "\n\nkeep email or write password down", null, user.getSpokenLangs());
                }
            }

            // send initial welcome push message
            if ((user.getPushID() == null) && (userInput.getPushID() != null)) {
                if (PushManager.getInstance().isAvaliable()) {
                    PushManager.getInstance().sendNotification(
                            PushManager.mapUserPlatform(userInput.getPushSystem()),
                            userInput.getPushID(),
                            "Welcome. If something happens in your neighborhood, Konfetti will send you Push-Updates.",
                            null,
                            null,
                            0l
                    );
                }
            }
            // transfer selective values from input to existing user
            user.setEMail(userInput.getEMail());
            user.setImageMediaID(userInput.getImageMediaID());
            user.setName(userInput.getName());
            user.setPushActive(userInput.getPushActive());
            user.setPushSystem(userInput.getPushSystem());
            user.setPushID(userInput.getPushID());
            user.setSpokenLangs(userInput.getSpokenLangs());
            user.setLastActivityTS(System.currentTimeMillis());
        } else {
            // B) check for trusted application with administrator privilege
            controllerSecurityHelper.checkAdminLevelSecurity(httpRequest);
            // complete overwrite allowed
            user = userInput;
        }
        // update user in persistence
        userService.update(user);

        // keep password hash just on server side
        user.setPassword("");

        return new ResponseEntity(userMapper.fromUserToUserResponse(user), HttpStatus.OK);
    }

    @SuppressWarnings("deprecation")
    @CrossOrigin(origins = "*")
    @GetMapping(value = "/coupons/{partyId}", produces = "application/json")
    public Boolean generateCodes(@PathVariable Long partyId,
                                 @RequestParam(value = "count", defaultValue = "0") Integer count,
                                 @RequestParam(value = "amount", defaultValue = "0") Integer amount,
                                 @RequestParam(value = "email", defaultValue = "") String email,
                                 @RequestParam(value = "locale", defaultValue = "en") String locale,
                                 HttpServletRequest httpRequest) throws Exception {

        checkEmailConfiguration();

        // validate inputs
        if (count <= 0) throw new Exception("must be more than 0 coupons");
        if (amount <= 0) throw new Exception("must be more than 0 per coupon");

        // check if party exists
        Party party = partyService.findById(partyId);
        if (party == null) throw new Exception("party does not exist");

        // get user from HTTP request
        Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
        if (client == null) throw new Exception("invalid/missing client on request");
        User user = userService.findById(client.getUser().getId());
        if (user == null) throw new Exception("missing user with id(" + client.getUser().getId() + ")");

        // check if user is admin for party
        if (!Helper.userIsAdminOnParty(user, partyId)) {
            throw new Exception("user needs to be admin on party");
        }


        // check if user has set email
        if (email.trim().length() == 0) email = user.getEMail();
        if ((email == null) || (email.trim().length() < 4))
            throw new Exception("user needs to have a valid email on account");

        // generate codes
        List<String> codes = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            Code code = this.codeService.createKonfettiCoupon(partyId, client.getUser().getId(), new Long(amount));
            System.out.println("Generated CouponCode: " + code.getCode());
            codes.add(code.getCode());
        }

        // URL max 8KB
        String urlStr = "";
        for (String code : codes) {
            urlStr += ("," + code);
        }

        urlStr = "http://konfettiCouponGenerator:2342/generate?template=" + URLEncoder.encode("coupon-master-template.html") + "&amount=" + amount + "&codes=" + URLEncoder.encode(urlStr.substring(1));
        if (urlStr.length() > (6 * 1024))
            log.warn("the URL to generate the codes is >6KB - limit is 8KB - may become critical");
        if (urlStr.length() > (8 * 1024))
            throw new Exception("the URL to generate the codes is >8KB - thats bigger than URL GET data can be with NodeJS");

        log.info("URL to generate Coupons: " + urlStr);

        if ((mailEnabled) && (!mailService.sendMail(email.trim(), "rest.user.coupons.subject", "Print out the PDF attached and spread the love :)", urlStr, user.getSpokenLangs()))) {
            throw new Exception("Was not able to send eMail with Coupons to " + user.getEMail());
        }
        return true;
    }

    private void checkEmailConfiguration() throws Exception {
        if (StringUtils.isEmpty(mailHost) && mailEnabled) {
            throw new Exception("eMail is not configured in properties file - cannot generate/send coupons");
        }
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/coupons-admin/{partyId}", produces = "application/json")
    public List<String> generateCodesAdmin(@PathVariable Long partyId,
                                           @RequestParam(value = "count", defaultValue = "0") Integer count,
                                           @RequestParam(value = "amount", defaultValue = "0") Integer amount,
                                           HttpServletRequest httpRequest) throws Exception {

        // validate inputs
        if (count <= 0) throw new Exception("must be more than 0 coupons");
        if (amount <= 0) throw new Exception("must be more than 0 per coupon");

        // check if party exists
        Party party = partyService.findById(partyId);
        if (party == null) throw new Exception("party does not exist");

        // check for trusted application with administrator privilege
        controllerSecurityHelper.checkAdminLevelSecurity(httpRequest);

        // generate codes
        List<String> codes = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            Code code = this.codeService.createKonfettiCoupon(partyId, 0l, new Long(amount));
            System.out.println("Generated CouponCode: " + code.getCode());
            codes.add(code.getCode());
        }

        return codes;
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/codes-admin/{partyId}", produces = "application/json")
    public List<String> generateCodesAdmin(@PathVariable Long partyId,
                                           @RequestParam(value = "count", defaultValue = "1") Integer count,
                                           @RequestParam(value = "type", defaultValue = "admin") String type,
                                           HttpServletRequest httpRequest) throws Exception {

        // validate inputs
        if (count <= 0) throw new Exception("must be more than 0");
        if ((!type.equals("admin")) && (!type.equals("review"))) throw new Exception("unkown type");

        // check if party exists
        Party party = partyService.findById(partyId);
        if (party == null) throw new Exception("party does not exist");

        // check for trusted application with administrator privilege
        controllerSecurityHelper.checkAdminLevelSecurity(httpRequest);

        // generate codes
        List<String> codes = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            Code code = null;
            if (type.equals("admin")) code = this.codeService.createAdminCode(partyId);
            if (type.equals("review")) code = this.codeService.createReviewCode(partyId);
            System.out.println("Generated " + type + "+ Code: " + code.getCode());
            codes.add(code.getCode());
        }

        return codes;
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/send/{partyId}", produces = "application/json")
    public ResponseSendKonfetti sendKonfetti(@PathVariable Long partyId,
                                             @RequestParam(value = "address", defaultValue = "") String address,
                                             @RequestParam(value = "amount", defaultValue = "0") Integer amount,
                                             @RequestParam(value = "locale", defaultValue = "en") String locale,
                                             HttpServletRequest httpRequest) throws Exception {

        log.info("*** SEND KONFETTI *** partyId(" + partyId + ") amount(" + amount + ") to(" + address + ")");

        // get eMail config
        checkEmailConfiguration();

        // check input data
        if (amount <= 0) throw new Exception("must be more than 0 per coupon");
        if (address == null) throw new Exception("address is NULL");
        if (address.trim().length() < 4) throw new Exception("email not valid");
        address = address.trim();
        address = address.toLowerCase();

        // get user from HTTP request
        Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
        if (client == null) throw new Exception("invalid/missing client on request");
        User user = userService.findById(client.getUser().getId());
        if (user == null) throw new Exception("missing user with id(" + client.getUser().getId() + ")");

        log.info("- sending userID(" + user.getId() + ")");

        // check if party exists
        Party party = partyService.findById(partyId);
        if (party == null) throw new Exception("party does not exist");

        // check if party allows sending konfetti
        if (party.getSendKonfettiMode() == SENDKONFETTIMODE_DISABLED) {
            throw new Exception("party(" + party.getId() + ") is not allowing sending of konfetti");
        }

        // get users konfetti balance
        final String accountName = AccountingTools.getAccountNameFromUserAndParty(client.getUser().getId(), party.getId());
        Long userBalance = accountingService.getBalanceOfAccount(accountName);

        // check amount of sending
        if (party.getSendKonfettiMode() == SENDKONFETTIMODE_JUSTEARNED) {
            long userEarnedBalance = this.accountingService.getBalanceEarnedOfAccount(accountName);
            if (amount > userEarnedBalance)
                throw new Exception("user earned fund too low - has (" + userBalance + ") of that earned(" + userEarnedBalance + ")wants to send (" + amount + ")");

        }
        if (userBalance <= amount) {
            throw new Exception("user fund too low - has (" + userBalance + ") wants to send (" + amount + ")");
        }

        // check if sending to address is white listed
        if (party.getSendKonfettiWhiteList().length > 0) {
            log.info("Whitelist activated for sending konfetti ...");
            boolean toAddressIsInList = false;
            for (int i = 0; i < party.getSendKonfettiWhiteList().length; i++) {
                String whiteAddress = party.getSendKonfettiWhiteList()[i];
                if (whiteAddress == null) continue;
                if (address.equals(whiteAddress.trim().toLowerCase())) {
                    toAddressIsInList = true;
                    break;
                }
            }
            if (!toAddressIsInList) {
                log.warn("BLOCKED - send to address (" + address + ") is not part of whitelist");
                throw new Exception("address is not part of white list");
            } else {
                log.info("OK - send to address is part of white list");
            }
        }

        // prepare result data
        ResponseSendKonfetti result = new ResponseSendKonfetti();

        // check if user with that address has already an account
        User toUser = userService.findByMailIgnoreCase(address);
        if (toUser == null) {
            // receiver has no account
            // GENERATE SINGLE COUPON and SEND BY EMAIL
            log.info("GENERATE SINGLE COUPON and SEND BY EMAIL");
            result.setTransferedToAccount(false);

            // generate coupon
            Code code = this.codeService.createKonfettiCoupon(party.getId(), client.getUser().getId(), new Long(amount));
            if (code == null) throw new Exception("Was not able to generate coupon for transfering konfetti.");
            log.info("- generated single coupon with code: " + code.getCode());

            // remove amount from users balance
            Long newBalance = accountingService.removeBalanceFromAccount(TransactionType.COUPON, accountName, amount);
            if (newBalance.equals(userBalance)) {
                throw new Exception("Was not able to remove sended konfetti from account(" + accountName + ")");
            }

            // send coupon by eMail
            if ((mailEnabled) && (mailService.sendMail(address, "rest.user.coupons.received", "Open app and redeem coupon code: '" + code.getCode(), null, user.getSpokenLangs()))) {
                log.info("- email with coupon send to: " + address);
            } else {
                accountingService.addBalanceToAccount(TransactionType.PAYBACK, accountName, amount);
                throw new Exception("Was not able to send eMail with coupon code to " + user.getEMail() + " - check address and server email config");
            }
        } else {
            // receiver has account
            // TRANSFERE BETWEEN ACCOUNT and SEND NOTIFICATION
            log.info("TRANSFERE BETWEEN ACCOUNT and SEND NOTIFICATION");
            result.setTransferedToAccount(true);

            // check if other user is already active on party
            if (!user.getActiveParties().contains(party)) {
                // invite user to party
                user.getActiveParties().add(party);
                userService.update(user);
            }

            // transfer konfetti
            String toAccountName = AccountingTools.getAccountNameFromUserAndParty(toUser.getId(), party.getId());
            if (!accountingService.transferBetweenAccounts(TransactionType.SEND_BY_USER, accountName, toAccountName, amount)) {
                throw new Exception("Was not able to transfere amount(" + amount + ") from(" + accountName + ") to(" + toAccountName + ")");
            }

            // send notification receiver (email as fallback)
            boolean sendNotification = false;
            if ((toUser.getPushID() != null) && (PushManager.getInstance().isAvaliable())) {
                // push notification
                if (PushManager.getInstance().sendNotification(
                        PushManager.mapUserPlatform(toUser.getPushSystem()),
                        toUser.getPushID(),
                        "You received " + amount + " Konfetti on Party '" + party.getName() + "'",
                        null,
                        null,
                        0l
                )) {
                    log.info("- push notification send to");
                    sendNotification = true;
                } else {
                    log.warn("was not able to send push notification to uuserId(" + user.getId() + ")");
                }
            }

            if (!sendNotification) {
                // eMail
                if ((mailEnabled) && (mailService.sendMail(address, "rest.user.coupons.received.party", "Open app and check party '" + party.getName() + "' :)", null, user.getSpokenLangs()))) {
                    log.info("- eMail with Info notification send to: " + address);
                } else {
                    log.error("Was not able to send eMail with Notification about received konfetti to " + user.getEMail() + " - check address and server email config");
                }
            }
        }
        log.info("OK SENDING KONFETTI");
        return result;
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/zip2gps/{country}/{code}", produces = "application/json")
    public ResponseZip2Gps zip2Gps(@PathVariable String country, @PathVariable String code) throws Exception {
        GpsConverter gpsConverter = new GpsConverter();
        return gpsConverter.fromZipCode(country, code);
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/redeem/{code}", produces = "application/json")
    public RedeemResponse redeemCode(@PathVariable String code, @RequestParam(value = "locale", defaultValue = "en") String locale, HttpServletRequest httpRequest) throws Exception {
        if (StringUtils.isEmpty(code)) throw new Exception("code is not valid");
        // TODO implement reedem code feedback in locale
        if (!locale.equals("en")) log.warn("TODO: implement reedem code feedback in locale '" + locale + "'");

        // get user from HTTP request
        Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
        if (client == null) throw new Exception("invalid/missing client on request");
        User user = userService.findById(client.getUser().getId());
        if (user == null) throw new Exception("missing user with id(" + client.getUser().getId() + ")");

        // try to redeemcode
        Code coupon = this.codeService.redeemByCode(code);

        // just if backend is running on in test mode allow cheat codes
        if (cheatCodesEnabled && coupon == null) {
            // --> creating coupons that are not in the database for testing
            coupon = new CheatCodes().getCodeFromCouponCode(code);
        }

        RedeemResponse result = new RedeemResponse();
        if (coupon != null) {
            result = this.codeService.processCodeCoupon(user, coupon);
        } else {
            // CODE NOT KNOWN
            // TODO --> multi lang by lang set in user
            result.setFeedbackHtml("Sorry. The code '" + code + "' is not known or invalid.");
        }
        return result;
    }
}
