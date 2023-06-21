package com.vikram.attendance.service;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FilesStorageService {
	
	void init(String path);

	void save(MultipartFile file);

	Resource load(String filename);

	boolean delete(String filename);

	void deleteAll();

	Stream<Path> loadAll();
	
	String getMetadata(MultipartFile file, String info);
}
