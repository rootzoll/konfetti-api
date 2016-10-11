package de.konfetti.data;

import de.konfetti.data.enums.MediaItemReviewEnum;
import de.konfetti.data.enums.MediaItemTypeEnum;
import lombok.Data;

import javax.persistence.*;

import static de.konfetti.data.enums.MediaItemReviewEnum.REVIEWED_PUBLIC;
import static de.konfetti.data.enums.MediaItemTypeEnum.TYPE_UNKOWN;

@Data
@Entity
public class MediaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // uploader
    private Long userId = 0l;
    
    // info if can be displayed to public
    private MediaItemReviewEnum reviewed = REVIEWED_PUBLIC;
    
    private Long lastUpdateTS = 0l; 
    
    private MediaItemTypeEnum type = TYPE_UNKOWN;
    
    // JSON or BASE64
    @Lob
    @Column(length = 1000000)
	private String data = "";
}
