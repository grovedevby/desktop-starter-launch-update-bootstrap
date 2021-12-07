package by.gdev.model;


import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;

import com.beust.jcommander.Parameter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StarterAppConfig{
	private static final String APP_CONFIG = "/appConfig.json";
	
	//Size in megabytes
	@Parameter(names = "-memory", description = "The size of the required free disk space to download the application")
    private long minMemorySize;
	
	@Parameter(names = "-mainAppConfig", description = "URI of the directory in which appConfig.json is located, which contains all information about the application being launched, this config is used by all applications by default")
	private String serverFile;
	
	@Parameter(names = "-workDirectory", description = "Working directory where the files required for the application will be loaded and in which the application will be launched. The param used for test. "
			+ "The second way is to put in file with installer.  The file name is installer.properties which contains work.dir=... This is for production. "
			+ "The default method is DesktopUtil.getSystemPath defined with by.gdev.Main. The priority: StarterAppConfig.workDirectory, file installer.properties and default method")
	//TODO change name of the value
	private String workDirectory;
	
	@Parameter(names = "-version", description = "Specifies the version of the application to launch. Therefore, the config http://localhost:81/app/1.0/appConfig.json for version 1.0 will be used. "
			+ "This way we can install old versions of the application. For this you need set exactly version.")
	private Double version;
	
	public static final StarterAppConfig DEFAULT_CONFIG;
	static {
		DEFAULT_CONFIG = new StarterAppConfig(500, 
				"http://localhost:81/starter-app/1.0",
				"target/test_folder/testContainer/",
				null);
	}
	//TODO IMPROVE METHOD NAME
	public String controlVersion(StarterAppConfig config) {
		if (Objects.isNull(config.getVersion()))
			return config.getServerFile() + APP_CONFIG;
		else {
			String[] str = config.getServerFile().split("/");
			String[] newstr = ArrayUtils.insert(str.length, str, String.valueOf(config.getVersion()));
			String joinedString = String.join("/", newstr) + APP_CONFIG;
			return joinedString;
		}
	}
}