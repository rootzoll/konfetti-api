package de.konfetti.data;

import de.konfetti.data.mediaitem.MediaItemTypeEnum;
import lombok.Data;

import javax.persistence.*;

import static de.konfetti.data.mediaitem.MediaItemTypeEnum.TYPE_UNKOWN;

@Data
@Entity
public class MediaItem {

	public static final Integer REVIEWED_PUBLIC = 0;
	public static final Integer REVIEWED_PRIVATE = 1;
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // uploader
    private Long userId = 0l;
    
    // info if can be displayed to public
    private Integer reviewed = 0;
    
    private Long lastUpdateTS = 0l; 
    
    private MediaItemTypeEnum type = TYPE_UNKOWN;
    
    // JSON or BASE64
    @Lob
    @Column(length = 1000000)
	private String data = "";
}
