package com.vikram.attendance.service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FilesStorageServiceImpl implements FilesStorageService {

	private Path root = Paths.get(System.getProperty("java.io.tmpdir"));

	@Override
	public void init(String path) {
		if (null != path && !path.isEmpty()) {
			try {
				root = Files.createDirectories(Paths.get(path));
			} catch (IOException e) {
				throw new RuntimeException("Could not initialize folder for upload!");
			}
		}
	}

	@Override
	public void save(MultipartFile file) {
		try {
			Path storePath = this.root.resolve(file.getOriginalFilename());
			long output = Files.copy(file.getInputStream(), storePath);
			System.out.println("Result = " + output);
			System.out.println(storePath.toAbsolutePath());
		} catch (Exception e) {
			if (e instanceof FileAlreadyExistsException) {
				throw new RuntimeException("A file of that name already exists.");
			}

			throw new RuntimeException(e.getMessage());
		}
	}
	
	@Override
	public Resource load(String filename) {
		try {
			Path file = root.resolve(filename);
			Resource resource = new UrlResource(file.toUri());

			if (resource.exists() || resource.isReadable()) {
				return resource;
			} else {
				throw new RuntimeException("Could not read the file!");
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException("Error: " + e.getMessage());
		}
	}

	@Override
	public boolean delete(String filename) {
		try {
			Path file = root.resolve(filename);
			return Files.deleteIfExists(file);
		} catch (IOException e) {
			throw new RuntimeException("Error: " + e.getMessage());
		}
	}

	@Override
	public void deleteAll() {
		FileSystemUtils.deleteRecursively(root.toFile());
	}

	@Override
	public Stream<Path> loadAll() {
		try {
			return Files.walk(this.root, 1).filter(path -> !path.equals(this.root)).map(this.root::relativize);
		} catch (IOException e) {
			throw new RuntimeException("Could not load the files!");
		}
	}

	@Override
	public String getMetadata(MultipartFile file, String info) {
		try {
			File imgFile = this.multipartToFile(file);
			this.readImageMeta(imgFile);
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ImageReadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return info;
	}
	
	public File multipartToFile(MultipartFile multipart) throws IllegalStateException, IOException {
		Path storePath = this.root.resolve(multipart.getOriginalFilename());
		File convFile = new File(storePath.toString());
	    multipart.transferTo(convFile);
	    return convFile;
	}
	
	/**
	 * Reference : https://github.com/apache/commons-imaging/blob/master/src/test/java/org/apache/commons/imaging/examples/MetadataExample.java
	 */
	public void readImageMeta(final File imgFile) throws ImageReadException, IOException {
	    /** get all metadata stored in EXIF format (ie. from JPEG or TIFF). **/
	    final ImageMetadata metadata = Imaging.getMetadata(imgFile);
	    System.out.println(metadata);
	    System.out.println("------------------------------------------------------------------------------");
	    
	    /** Get specific meta data information by drilling down the meta **/
	    if (metadata instanceof JpegImageMetadata) {
	        JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
	        printTagValue(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF);
	        printTagValue(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_LATITUDE);
	        printTagValue(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF);
	        printTagValue(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_LONGITUDE);
	        
	        // simple interface to GPS data
	        final TiffImageMetadata exifMetadata = jpegMetadata.getExif();
	        if (null != exifMetadata) {
	            final TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS();
	            if (null != gpsInfo) {
	                final String gpsDescription = gpsInfo.toString();
	                final double longitude = gpsInfo.getLongitudeAsDegreesEast();
	                final double latitude = gpsInfo.getLatitudeAsDegreesNorth();

	                System.out.println("    " + "GPS Description: " + gpsDescription);
	                System.out.println("    " + "GPS Longitude (Degrees East): " + longitude);
	                System.out.println("    " + "GPS Latitude (Degrees North): " + latitude);
	            }
	        }

	        // more specific example of how to manually access GPS values
	        final TiffField gpsLatitudeRefField = jpegMetadata.findEXIFValueWithExactMatch(GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF);
	        final TiffField gpsLatitudeField = jpegMetadata.findEXIFValueWithExactMatch(GpsTagConstants.GPS_TAG_GPS_LATITUDE);
	        final TiffField gpsLongitudeRefField = jpegMetadata.findEXIFValueWithExactMatch(GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF);
	        final TiffField gpsLongitudeField = jpegMetadata.findEXIFValueWithExactMatch(GpsTagConstants.GPS_TAG_GPS_LONGITUDE);
	        if (gpsLatitudeRefField != null && gpsLatitudeField != null && gpsLongitudeRefField != null && gpsLongitudeField != null) {
	            // all of these values are strings.
	            final String gpsLatitudeRef = (String) gpsLatitudeRefField.getValue();
	            final RationalNumber[] gpsLatitude = (RationalNumber[]) (gpsLatitudeField.getValue());
	            final String gpsLongitudeRef = (String) gpsLongitudeRefField.getValue();
	            final RationalNumber[] gpsLongitude = (RationalNumber[]) gpsLongitudeField.getValue();

	            final RationalNumber gpsLatitudeDegrees = gpsLatitude[0];
	            final RationalNumber gpsLatitudeMinutes = gpsLatitude[1];
	            final RationalNumber gpsLatitudeSeconds = gpsLatitude[2];

	            final RationalNumber gpsLongitudeDegrees = gpsLongitude[0];
	            final RationalNumber gpsLongitudeMinutes = gpsLongitude[1];
	            final RationalNumber gpsLongitudeSeconds = gpsLongitude[2];

	            // This will format the gps info like so:
	            //
	            // gpsLatitude: 8 degrees, 40 minutes, 42.2 seconds S
	            // gpsLongitude: 115 degrees, 26 minutes, 21.8 seconds E

	            System.out.println("    " + "GPS Latitude: " + gpsLatitudeDegrees.toDisplayString() + " degrees, " + gpsLatitudeMinutes.toDisplayString() + " minutes, " + gpsLatitudeSeconds.toDisplayString() + " seconds " + gpsLatitudeRef);
	            System.out.println("    " + "GPS Longitude: " + gpsLongitudeDegrees.toDisplayString() + " degrees, " + gpsLongitudeMinutes.toDisplayString() + " minutes, " + gpsLongitudeSeconds.toDisplayString() + " seconds " + gpsLongitudeRef);
	        }
	    }
	}

	private void printTagValue(final JpegImageMetadata jpegMetadata, TagInfo tagInfo) {
	    final TiffField field = jpegMetadata.findEXIFValueWithExactMatch(tagInfo);
	    if (field == null) {
	        System.out.println(tagInfo.name + ": " + "Not Found.");
	    } else {
	        System.out.println(tagInfo.name + ": " + field.getValueDescription());
	    }
	}

}
