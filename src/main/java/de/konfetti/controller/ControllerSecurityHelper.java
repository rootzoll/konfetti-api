package de.konfetti.controller;

import de.konfetti.data.Client;
import de.konfetti.service.ClientService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.util.SubnetUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;

@Configuration
@Component
@Slf4j
public class ControllerSecurityHelper {

	public static final String HEADER_ADMIN_PASSWORD = "X-ADMIN-PASSWORD";

	private boolean doneInit = false;
	
	/*
	 * ADMIN-LEVEL
	 * all methods on REST API that are open for users, but for other service layers or applications
	 */
	
	// 1. IP-Security
	@Value("${konfetti.admin.check.ip}")
	private boolean checkIp = true;

	@Value("${konfetti.admin.allowedSubnet}")
	private String allowedSubnet = "127.0.0.0/16";

	// 2. PASSWORD-Security
	@Value("${konfetti.admin.check.password}")
	private boolean checkPassword = false;

	@Value("${konfetti.admin.password}")
	private String adminPassword = null;

	private void doInit() {

		log.info("Doing INIT ControllerSecurityHelper ...");
		
		// try to load password from properties file
		if (adminPassword !=null) {
			adminPassword = adminPassword.trim();
			if (adminPassword.length()<4) {
				log.warn("PASSWORD IN PROPERTIES FILE IS TOO SHORT -> PLEASE CHOOSE LONGER ONE");
			}
			if (adminPassword.length()==0) adminPassword = null;
		}
		
		// make settings based on password
		if (adminPassword !=null) {
			checkPassword = true;
			checkIp = false;
			log.info("- OK ADMIN ACCESS PER PASSWORD ACTIVATED (see config file) - IP SECURITY OFF");
		} else {
			log.info("(no ADMIN PASSWORD set in PROPERTIES FILE)");
		}
		
		doneInit = true;
	}
	
	// if throws an Exception ==> security check failed
	public boolean checkAdminLevelSecurity(HttpServletRequest req) throws Exception {
		
		if (!doneInit) doInit();
		
		// detect no security
		if ((!checkIp) && (!checkPassword)) {
			throw new Exception("ControllerSecurityHelper: MISSING ADMIN LEVEL SECURITY - BLOCKING");
		}
		
		// check IP security
		if (checkPassword) {
			// check PASSWORD security -> ip & password cannot be activated at the same time, password higher priority
			// TODO: ACTIVATE ON RELEASE
			// when password is used - HTTPS is mandatory
			// if (!req.isSecure()) throw new Exception("ControllerSecurityHelper: HTTPS is needed when password security is used from IP("+req.getRemoteAddr()+")");

			// get password from HTTP header
			String requestingPassword = req.getHeader(HEADER_ADMIN_PASSWORD);
			if (requestingPassword==null) throw new Exception("ControllerSecurityHelper: Missing " + HEADER_ADMIN_PASSWORD + " header field on HTTP request for ADMIN-LEVEL SECURITY from IP("+req.getRemoteAddr()+")");

			// check if given password is valid
			boolean correctPassword = ((adminPassword !=null) && (adminPassword.equals(requestingPassword)));
			if (!correctPassword) {
				try {
					Thread.sleep(300);
				} catch (Exception e) {}
				throw new Exception("FAIL-PASSWORD: ControllerSecurityHelper: Requesting Password("+requestingPassword+") is not correct for ADMIN-LEVEL SECURITY from IP("+req.getRemoteAddr()+")");
			}
		} else {
			// get IP from request
			String requestingIP = req.getRemoteAddr();

			InetAddress inetAddress = InetAddress.getByName(requestingIP);
			SubnetUtils subnetUtils = new SubnetUtils(allowedSubnet);

			// check if requested IP is within allowed subnet
			boolean correctIP = false;
			if (inetAddress.isLoopbackAddress() || subnetUtils.getInfo().isInRange(requestingIP)){
				correctIP = true;
			}

			if (!correctIP) throw new Exception("ControllerSecurityHelper: Requesting IP("+requestingIP+") is not allowed for ADMIN-LEVEL SECURITY");
		}
		return true;
	}
		
	public Client getClientFromRequestWhileCheckAuth(HttpServletRequest req, ClientService clientService) {
		
		log.info("Check Client Security ...");
		if (!doneInit) doInit();
		
		// get user credentials from HTTP header
		String clientId = req.getHeader("X-CLIENT-ID");
		String clientSecret = req.getHeader("X-CLIENT-SECRET"); 
		
		// check if input data is valid
		if ((clientId==null) || (clientSecret==null)) throw new IllegalArgumentException("ControllerSecurityHelper: Missing X-CLIENT-* fields on client request from IP("+req.getRemoteAddr()+")");
		if ((clientId.indexOf(" ")>=0) || (clientSecret.indexOf(" ")>=0)) throw new IllegalArgumentException("ControllerSecurityHelper: Missing X-CLIENT-* fields contain ' ' ("+clientId+"/"+clientSecret+") from IP("+req.getRemoteAddr()+")");
		Long clientIdLong =  null;
		try {
			clientIdLong = Long.parseLong(clientId);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("ControllerSecurityHelper: X-CLIENT-ID id no Number ("+clientId+"/"+clientSecret+") from IP("+req.getRemoteAddr()+")");
		}

		log.info("clientId(" + clientId + ") clientSecret(" + clientSecret + ")");
		
		// check if client exists
		Client client = null;
		try {
			client = clientService.findById(clientIdLong);
		} catch (Exception e) {
			log.error("EXCEPTION in finding client with id("+clientIdLong+")", e);
		}
		if (client==null) {
			log.trace("CLIENT NOT FOUND");
			try {
				Thread.sleep(300); // security delay against brute force
			} finally {
				throw new IllegalArgumentException("ControllerSecurityHelper: No client found with id ("+clientId+") from IP("+req.getRemoteAddr()+")");
			}
		}
		
		// check if client has correct secret
		if (!clientSecret.equals(client.getSecret())) {
			log.trace("WRONG SECRET");
			try {
				Thread.sleep(300); // security delay against brute force
			} finally {
				throw new IllegalArgumentException("ControllerSecurityHelper: Client(" + clientId + ") wrong secretGiven(" + clientSecret + ") should be secretIs(" + client.getSecret() + ") from IP(" + req.getRemoteAddr() + ")");
			}
		}

		log.info("Check Client Security ... OK");
		return client;
	}
}
