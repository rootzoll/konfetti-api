package de.konfetti.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.konfetti.data.Client;
import de.konfetti.data.MediaItem;
import de.konfetti.data.enums.MediaItemTypeEnum;
import de.konfetti.data.mediaitem.MultiLang;
import de.konfetti.service.ClientService;
import de.konfetti.service.MediaService;
import de.konfetti.service.UserService;
import de.konfetti.utils.AutoTranslator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.ByteArrayInputStream;

import static de.konfetti.data.enums.MediaItemReviewEnum.REVIEWED_PRIVATE;
import static de.konfetti.data.enums.MediaItemTypeEnum.TYPE_IMAGE;
import static de.konfetti.data.enums.MediaItemTypeEnum.TYPE_MULTILANG;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(MediaItemController.REST_API_MAPPING)
public class MediaItemController {

	public static final String REST_API_MAPPING = "konfetti/api/media";

	private final ClientService clientService;
	private final MediaService mediaService;

	@Autowired
	private AutoTranslator autoTranslator;

	@Autowired
	private ControllerSecurityHelper controllerSecurityHelper;

    @Autowired
    public MediaItemController(final ClientService clientService, final MediaService mediaService, final UserService userService) {
        this.clientService = clientService;
        this.mediaService = mediaService;
    }

    //---------------------------------------------------
    // MEDIA ITEM Controller
    //---------------------------------------------------
    
    @CrossOrigin(origins = "*")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public MediaItem createMedia(@RequestBody @Valid final MediaItem template, HttpServletRequest httpRequest) throws Exception {
    	
        log.info("*** POST Create Media ***");
    	
    	// check if user is allowed to create
    	if (httpRequest.getHeader("X-CLIENT-ID")!=null) {
    		// A) check that chat is just hosted by user
    		Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
    		if (client==null) throw new Exception("client is NULL");
        	template.setUserId(client.getUser().getId());
    	} else {
    		// B) check for trusted application with administrator privilege
        	controllerSecurityHelper.checkAdminLevelSecurity(httpRequest);
    	}
    	
    	// security override on template
    	template.setReviewed(REVIEWED_PRIVATE);
    	
    	// check if type is supported
		if (!MediaItemTypeEnum.validTypes().contains(template.getType()))
			throw new Exception("type("+template.getType()+") is not supported as media item. Supported MediaTypes are : " + MediaItemTypeEnum.validTypes());

    	// MULTI-LANG auto translation
    	if (TYPE_MULTILANG.equals(template.getType())) {
			log.info("Is MultiLang --> AUTOTRANSLATION");
			try {
    			MultiLang multiLang = new ObjectMapper().readValue(template.getData(), MultiLang.class);
    			multiLang = autoTranslator.reTranslate(multiLang);
    			template.setData(new ObjectMapper().writeValueAsString(multiLang));
				log.info(template.getData());
			} catch (Exception e) {
    			e.printStackTrace();
    			throw new Exception("MultiLang Data is not valid: "+e.getMessage());
    		}
    	} else {
			log.info("NOT MultiLang --> no special treatment needed");
		}
    	  	    	
    	// create new user
    	MediaItem item = mediaService.create(template);
		log.info("OK mediaItem(" + item.getId() + ") created");
		return item;
    }
    
    @CrossOrigin(origins = "*")
    @RequestMapping(value="/{mediaId}", method = RequestMethod.GET, produces = "application/json")
    public MediaItem getMedia(@PathVariable Long mediaId, HttpServletRequest httpRequest) throws Exception {
        
        log.info("*** GET Media ***");
    	
    	// try to item
    	MediaItem item = mediaService.findById(mediaId);
    	if (item==null) throw new Exception("media("+mediaId+") not found");

    	return item;
    }
    
    @CrossOrigin(origins = "*")
    @RequestMapping(value="/{mediaId}/image", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> getMediaAsImage(@PathVariable Long mediaId, HttpServletRequest httpRequest) throws Exception {
        
        log.info("*** GET Media As Image ***");
    	
    	// try to item
    	MediaItem item = mediaService.findById(mediaId);
    	if (item==null) throw new Exception("media("+mediaId+") not found");
    	
    	// check if image
    	if (!item.getType().equals(TYPE_IMAGE)) throw new Exception("media("+mediaId+") is not image");
    	
    	// get base64 string
    	String rawData = item.getData();
		String base64Matcher = "base64,";
		int base64Index = rawData.indexOf(base64Matcher);

    	if (base64Index <= 0) throw new Exception("no BASE64 start index found");
    	int startIndex = base64Index + base64Matcher.length();
		String base64String = rawData.substring(startIndex);

		// get mime type
		String dataMatcher = "data:";
		int dataIndex = rawData.indexOf(dataMatcher);
    	String mimeType = rawData.substring(dataIndex + dataMatcher.length(), rawData.indexOf(';'));
		log.info("READ IMAGE(" + mediaId + ") with MIMETYPE(" + mimeType + ")");

		// convert to binary
		byte[] data = javax.xml.bind.DatatypeConverter.parseBase64Binary(base64String);

    	// return response
    	return ResponseEntity
    	            .ok()
    	            .contentLength(data.length)
    	            .contentType(
    	                    MediaType.parseMediaType(mimeType))
    	            .body(new InputStreamResource(new ByteArrayInputStream(data)));
    }
     
}
