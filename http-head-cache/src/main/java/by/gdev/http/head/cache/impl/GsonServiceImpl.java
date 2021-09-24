package by.gdev.http.head.cache.impl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.google.gson.Gson;

import by.gdev.http.head.cache.service.FileService;
import by.gdev.http.head.cache.service.GsonService;

public class GsonServiceImpl implements GsonService {
	Gson gson;
	FileService fileService;
	
	public GsonServiceImpl(Gson GSON, FileService fileService) {
		this.gson = GSON;
		this.fileService = fileService;
	}

	@Override
	public <T> T getObject(String url, Class<T> class1) throws FileNotFoundException, IOException {
		BufferedReader read = new BufferedReader(new FileReader(fileService.getRawObject(url, false).toFile()));
		return gson.fromJson(read, class1);
	}
}
