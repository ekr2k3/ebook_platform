package com.erk.ebookPlatform.service;

import com.cloudinary.Cloudinary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) { //inject = controller, có thể dùng @Autowired 
        this.cloudinary = cloudinary;
    }

	public Map<String, Object> uploadFile(MultipartFile file) throws IOException { // Method quan trong nhất
	
	    if (file == null || file.isEmpty()) {
	        throw new IllegalArgumentException("File is empty");
	    }
	
	    Map<String, Object> options = new HashMap<>();
	    options.put("resource_type", "auto");
	    options.put("folder", "demo");
	    options.put("use_filename", true);
	    options.put("unique_filename", true);
	
	    Map<String, Object> result = cloudinary.uploader().upload(
	            file.getBytes(),
	            options
	    );
	
	    return result;
	}
}
